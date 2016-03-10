package scalan
package compilation
package lms
package scalac

import java.io._
import java.lang.reflect.{Constructor, Method}
import java.net.{URL, URLClassLoader}

import scala.reflect.runtime.universe._

import scalan.compilation.lms.source2bin.{SbtConfig, Nsc, Sbt}
import scalan.compilation.lms.uni.NativeMethodsConfig
import scalan.util.{ReflectionUtil, FileUtil}
import scalan.util.FileUtil.file

case class LmsCompilerScalaConfig(extraCompilerOptions: Seq[String] = Seq.empty, sbtConfigOpt: Option[SbtConfig] = None, traits: Seq[String] = Seq.empty[String], nativeMethods: NativeMethodsConfig = new NativeMethodsConfig) {
  def withSbtConfig(sbtConfig: SbtConfig) = copy(sbtConfigOpt = Some(sbtConfig))
}

class LmsCompilerScala[+ScalanCake <: ScalanDslExp](_scalan: ScalanCake) extends LmsCompiler(_scalan) with CoreBridgeScala {
  val lms = new ScalaCoreLmsBackend

  import scalan._

  // optConstructor is defined when input is a struct, see below for usage
  class ObjMethodPair(instance: AnyRef, method: Method, val optConstructor: Option[Constructor[_]]) {
    def invoke(args: AnyRef*) = method.invoke(instance, args: _*)
  }

  case class CustomCompilerOutput(objMethod: ObjMethodPair, sources: List[File], jar: File, mainClass: String, output: Option[Array[String]])

  type CompilerConfig = LmsCompilerScalaConfig
  implicit val defaultCompilerConfig = LmsCompilerScalaConfig()

  protected def doBuildExecutable[A, B](sourcesDir: File, executableDir: File, functionName: String, graph: PGraph, graphVizConfig: GraphVizConfig)
                                       (compilerConfig: CompilerConfig, eInput: Elem[A], eOutput: Elem[B]) = {
    Sbt.prepareDir(executableDir) //todo - check: is it sbt-specific function?
    /* LMS stuff */
    val sourceFile = emitSource(sourcesDir, functionName, graph, eInput, eOutput, graphVizConfig)
    val jarFile = file(executableDir.getAbsoluteFile, s"$functionName.jar")
    FileUtil.deleteIfExist(jarFile)
    val jarPath = jarFile.getAbsolutePath
    val mainClass = mainClassName(functionName, compilerConfig)
    val output: Option[Array[String]] = compilerConfig.sbtConfigOpt match {
      case Some(sbtConfig) =>
        val dependencies:Array[String] = methodReplaceConf.flatMap(conf => conf.dependencies).toArray
        Some(Sbt.compile(sourcesDir, executableDir, functionName, compilerConfig.extraCompilerOptions, sbtConfig, dependencies, sourceFile, jarPath))
      case None =>
        Nsc.compile(executableDir, functionName, compilerConfig.extraCompilerOptions.toList, sourceFile, jarPath)
        None
    }
    val objMethod = loadMethod(jarFile, eInput, mainClass)

    CustomCompilerOutput(objMethod, List(sourceFile), jarFile, mainClass, output)
  }

  def mainClassName(functionName: String, compilerConfig: LmsCompilerScalaConfig): String =
    compilerConfig.sbtConfigOpt.flatMap(_.mainPack) match {
      case None => functionName
      case Some(mainPackage) => s"$mainPackage.$functionName"
    }

  def loadMethod(jarFile: File, eInput: Elem[_], className: String) = {
    val jarUrl = jarFile.toURI.toURL
    val classLoader = getClassLoader(Array(jarUrl))
    val cls = classLoader.loadClass(className)
    val instance = cls.newInstance().asInstanceOf[AnyRef]
    val (staticArgClass, optConstructor) = eInput match {
      case se: StructElem[_] =>
        val runtimeArgClassName = structName(se.asInstanceOf[StructElem[_ <: Struct]])
        val runtimeArgClass = classLoader.loadClass(runtimeArgClassName)
        val constructors = runtimeArgClass.getConstructors
        assert(constructors.length == 1,
          s"Class $runtimeArgClassName generated by LMS from structure is expected to have 1 constructor")
        // AnyRef { ... } refinement type is generated in GenericCodegen.remap on RefinedManifest,
        // so the JVM static type is AnyRef
        (classOf[AnyRef], Some(constructors(0)))
      case _ =>
        (eInput.runtimeClass, None)
    }
    val method = cls.getMethod("apply", staticArgClass)
    new ObjMethodPair(instance, method, optConstructor)
  }

  def getClassLoader(jarUrls: Array[URL]): ClassLoader =
    new URLClassLoader(jarUrls, getClass.getClassLoader)

  protected def doExecute[A, B](compilerOutput: CompilerOutput[A, B], input: A): B = {
    val methodArg = fixUpInput(input)(compilerOutput)
    val methodResult = compilerOutput.custom.objMethod.invoke(methodArg)
    fixUpOutput(methodResult)(compilerOutput)
  }

  // Casts to scalanSeq._ below should be safe even though arguments may have different outer instances
  protected def fixUpInput[A](input: A)(compilerOutput: CompilerOutput[A, _]): AnyRef = (compilerOutput.custom.objMethod.optConstructor match {
    case Some(constructor) =>
      val fieldValues = input.asInstanceOf[scalanSeq.Struct].fields.map(_._2.asInstanceOf[AnyRef])
      constructor.newInstance(fieldValues: _*)
    case _ =>
      input
  }).asInstanceOf[AnyRef]

  protected def fixUpOutput[A](output: AnyRef)(compilerOutput: CompilerOutput[_, A]): A = (compilerOutput.common.eOutput match {
    case se: StructElem[a] =>
      val tag = se.asInstanceOf[scalanSeq.StructElem[a with scalanSeq.Struct]].structTag
      val paramMirrors = ReflectionUtil.paramMirrors(output)
      val fieldValues = paramMirrors.map(_.get)
      val fields = se.fieldNames.zip(fieldValues)
      scalanSeq.struct(tag, fields)
    case _ =>
      output
  }).asInstanceOf[A]

  protected[this] lazy val scalanSeq = new ScalanDslStd
}
