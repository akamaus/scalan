package scalan.compilation.lms.cxx.sharedptr

import java.io.PrintWriter

import scala.collection.immutable.ListSet
import scala.lms.common.{TupledFunctionsExp, FunctionsExp}
import scala.lms.internal.{CLikeCodegen, Expressions, GenerationFailedException}
import scalan.compilation.language.{Adjustment, Adjusted}
import scalan.compilation.lms.ManifestUtil
import scalan.compilation.lms.common.JNILmsOps

trait Ptr[T]
trait Ref[T]
trait ConstQual[T]

trait CxxShptrCodegen extends CLikeCodegen with ManifestUtil {
  val IR: Expressions
  import IR._

  trait size_t
  trait SharedPtr[T]
  trait auto_t
//  trait Ptr[T]
//  trait Ref[T]
//  trait ConstQual[T]

  var headerFiles: collection.mutable.HashSet[String] = collection.mutable.HashSet.empty
  var globals = ListSet.empty[Adjusted[Exp[_]]]

  headerFiles ++= Seq("memory", "scalan/common.hpp")

  def toShptrManifest(m: Manifest[_]): Manifest[_] = {
    if( m.runtimeClass == classOf[SharedPtr[_]] )
      m
    else {
      val newM = m.typeArguments match {
        case Nil => m
        case h :: t =>
          Manifest.classType(m.runtimeClass, toShptrManifest(h), t.map(toShptrManifest): _*)
      }

      wrapSharedPtr(newM)
    }
  }

  protected def doNotWrap(m: Manifest[_]) =
    m.isPrimitive || m.isOneOf(classOf[SharedPtr[_]], classOf[scala.Tuple2[_, _]], classOf[Variable[_]], classOf[_ => _])

  def wrapSharedPtr(m: Manifest[_]) = if (doNotWrap(m)) m else Manifest.classType(classOf[SharedPtr[_]], m)

  final override def emitValDef(sym: Sym[Any], rhs: String ): Unit = {
    val newTp = toShptrManifest(sym.tp)
    emitValDef(quote(sym), newTp, rhs)
  }

  final override def emitValDef(sym: String, tpe: Manifest[_], rhs: String): Unit = {
      val cv = if( tpe.runtimeClass == classOf[Unit] ) "const " else ""
      stream.println(src"$cv${remap(tpe)} $sym = $rhs;")
  }

  override def remap[A](m: Manifest[A]) : String = {
    m match {
      case _ if m.runtimeClass == classOf[SharedPtr[_]] =>
        src"std::shared_ptr<${m.typeArguments(0)}>"
      case _ if m.runtimeClass == classOf[auto_t] =>
        "auto"
      case _ if m.runtimeClass == classOf[size_t] =>
        "size_t"
      // TODO the rules are more complex, but this should work for now
      case _ if m.runtimeClass == classOf[Ptr[_]] =>
        src"${m.typeArguments(0)}*"
      case _ if m.runtimeClass == classOf[Ref[_]] =>
        src"${m.typeArguments(0)}&"
      case _ if m.runtimeClass == classOf[ConstQual[_]] =>
        src"const ${m.typeArguments(0)}"
      case _ if m.runtimeClass == classOf[Unit] =>
        "boost::blank"
      case _ if m.isPrimitive =>
        super[CLikeCodegen].remap(m)
      case _ =>
        throw new GenerationFailedException(s"CxxShptrCodegen.remap(): $m can not be remaped.")
    }
  }

  def remapWithoutTemplateArgs[A](m: Manifest[A]) = {
    val remappedM = remap(m)
    remappedM.split("<")(0)
  }

  final override def emitVarDecl(sym: Sym[Any]): Unit = {
    emitConstruct(sym)
  }

  final override def emitVarDef(sym: Sym[Variable[Any]], rhs: String): Unit =
    emitValDef(sym, rhs)

  final def emitConstruct(sym: Sym[Any], args: String*): Unit = {
    val shptrTp = toShptrManifest(sym.tp)
    emitConstruct(sym, shptrTp, args: _*)
  }

  protected def emitConstruct(sym: Sym[Any], shptrTp: Manifest[_], args: String*): Unit = {
    shptrTp.runtimeClass match {
      case c if c == classOf[SharedPtr[_]] =>
        stream.println(src"$shptrTp $sym = std::make_shared<${shptrTp.typeArguments(0)}>($args);")
      case _ =>
        stream.println(src"$shptrTp $sym = $shptrTp($args);")
    }
  }

  override def quote(x: Exp[Any]) = x match {
    case Const(s: Unit) => "scalan::unit_value"
    case _ => super.quote(x)
  }

  override def emitSource[A: Manifest](args: List[Sym[_]], body: Block[A], className: String, out: PrintWriter) = {
    val resultM = manifest[A]

    //      val staticData = getFreeDataBlock(body)
    withStream(out) {
      preprocess(args, body, className)(resultM)

      stream.println(
        "#if __cplusplus < 201103L\n" +
        "#error C++11 support required\n" +
        "#endif\n"
      )

      headerFiles.map {fn => s"#include <${fn}>"} map ( stream.println _ )
      stream.println(
          "/*****************************************\n" +
          "  Emitting Generated Code                  \n" +
          "*******************************************/")
      //emitFileHeader()

      stream.println("namespace scalan {")

      globals.foreach {
        // assume e is a function and e.tp is Manifest[(...) => ...]
        // nasty tricks because we can't reasonably add FunctionsExp to IR type
        case Adjusted(e @ IR.Def(l), adj) =>
          l.asInstanceOf[Any] match {
            case lam: FunctionsExp#Lambda[a, b] =>
              val args = lam.x.asInstanceOf[Any] match {
                case x: Sym[_] => List(x)
                case xs: TupledFunctionsExp#UnboxedTuple[_] => xs.vars.asInstanceOf[List[Sym[_]]]
              }
              emitFunctionDef(args, lam.y.asInstanceOf[Block[b]], quote(e), lam.y.tp, adj)
            case _ =>
              throw new GenerationFailedException(s"Expected global $e's definition to be a lambda, got $l")
          }
      }

      emitFunctionDef(args, body, className, resultM, None)

      stream.println("}") // namespace scalan

      stream.println("/*****************************************\n" +
        "  End of Generated Code                  \n" +
        "*******************************************/")
    }

    Nil
  }

  def emitFunctionDef[A](args: List[Sym[_]], body: Block[A], functionName: String, resultM: Manifest[A], adj: Option[Adjustment]): Unit = {
    val hasJNI = args.map(_.tp.runtimeClass).contains(classOf[JNILmsOps#JNIType[_]]) || resultM.runtimeClass == classOf[JNILmsOps#JNIType[_]]
    val returnType = returnTyp(resultM, adj)
    val extraArgs = if (hasJNI) "JNIEnv* env, jobject, " else ""
    val argsString = this.mainArgs(args, adj)
    val linkage = if (hasJNI) """extern "C" JNIEXPORT""" else ""
    val callConvention = if (hasJNI) "JNICALL " else ""
    stream.println(s"$linkage $returnType $callConvention$functionName($extraArgs$argsString) {")

    emitBlock(body)
    stream.println(functionResult(args, body, adj))

    stream.println("}")
  }

  def functionResult[A](args: List[Sym[_]], body: Block[A], adj: Option[Adjustment]) =
    src"return ${getBlockResult(body)};"

  protected def returnTyp(resultM: Manifest[_], adj: Option[Adjustment]) =
    remap(toShptrManifest(resultM))

  protected def mainArgs(args: List[Sym[_]], adj: Option[Adjustment]) =
    args.map(arg => src"${toShptrManifest(arg.tp)} $arg").mkString(", ")

  // override to initialize headerFiles, globals, etc.
  def preprocess[A: Manifest](args: List[Sym[_]], body: Block[A], className: String): Unit = {}
}
