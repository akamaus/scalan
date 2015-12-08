package scalan.compilation

import java.io.File

import scalan.ScalanCtxExp

abstract class Compiler[+ScalanCake <: ScalanCtxExp](val scalan: ScalanCake) extends Passes {
  import scalan._

  type CompilerConfig

  def defaultCompilerConfig: CompilerConfig

  type CustomCompilerOutput

  case class CommonCompilerOutput[A, B](graph: PGraph, name: String, eInput: Elem[A], eOutput: Elem[B])

  case class CompilerOutput[A, B](common: CommonCompilerOutput[A, B], custom: CustomCompilerOutput, config: CompilerConfig)

  // see comment for buildInitialGraph
  // TODO sequence may depend on input or intermediate graphs, use a state monad instead
  def graphPasses(compilerConfig: CompilerConfig): Seq[PGraph => GraphPass] = Seq()

  // Can it return ProgramGraph[Ctx] for some other Ctx?
  // If so, may want to add Ctx as type argument or type member
  protected def buildInitialGraph[A, B](func: Exp[A => B])(compilerConfig: CompilerConfig): PGraph = {
    new PGraph(func)
  }

  // G is PGraph with some extra information
  protected def emittingGraph[G](sourcesDir: File, fileName: String, passName: String, graphVizConfig: GraphVizConfig, toGraph: G => PGraph)(mkGraph: => G): G = {
    val file = new File(sourcesDir, fileName)
    try {
      val g = mkGraph
      emitDepGraph(toGraph(g), file)(graphVizConfig)
      g
    } catch {
      case e: Exception =>
        emitExceptionGraph(e, file)(graphVizConfig)
        throw new CompilationException(s"$passName failed. See ${file.getAbsolutePath} for exception graph.", e)
    }
  }

  protected def buildAndEmitInitialGraph[A, B](sourcesDir: File, functionName: String, func: => Exp[A => B], graphVizConfig: GraphVizConfig, compilerConfig: CompilerConfig): (PGraph, Elem[A], Elem[B]) = {
    emittingGraph[(PGraph, Elem[A], Elem[B])](sourcesDir, s"$functionName.dot", "Initial graph generation", graphVizConfig, _._1) {
      val func0 = func
      val eFunc = func0.elem
      (buildInitialGraph(func0)(compilerConfig), eFunc.eDom, eFunc.eRange)
    }
  }

  def buildGraph[A, B](sourcesDir: File, functionName: String, func: => Exp[A => B], graphVizConfig: GraphVizConfig)(compilerConfig: CompilerConfig): CommonCompilerOutput[A, B] = {

    val (initialGraph, eInput, eOutput) = buildAndEmitInitialGraph(sourcesDir, functionName, func, graphVizConfig, compilerConfig)

    val passes = graphPasses(compilerConfig)

    val numPassesLength = passes.length.toString.length

    val finalGraph = passes.zipWithIndex.foldLeft(initialGraph) { case (graph, (passFunc, index)) =>
      val pass = passFunc(graph)

      val indexStr = (index + 1).toString
      val dotFileName = s"${functionName}_${"0" * (numPassesLength - indexStr.length) + indexStr}_${pass.name}.dot"

      emittingGraph[PGraph](sourcesDir, dotFileName, pass.name, graphVizConfig, g => g) {
        scalan.beginPass(pass)
        val graph1 = pass(graph).withoutContext
        scalan.endPass(pass)

        graph1
      }
    }

    CommonCompilerOutput(finalGraph, functionName, eInput, eOutput)
  }

  def buildExecutable[A, B](sourcesDir: File, executableDir: File, functionName: String, func: => Exp[A => B], graphVizConfig: GraphVizConfig)
                           (implicit compilerConfig: CompilerConfig): CompilerOutput[A, B] = {
    sourcesDir.mkdirs()
    executableDir.mkdirs()
    val commonOutput = buildGraph(sourcesDir, functionName, func, graphVizConfig)(compilerConfig)
    val customOutput = doBuildExecutable(sourcesDir, executableDir, functionName, commonOutput.graph, graphVizConfig)(compilerConfig, commonOutput.eInput, commonOutput.eOutput)
    CompilerOutput(commonOutput, customOutput, compilerConfig)
  }

  def buildExecutable[A, B](sourcesAndExecutableDir: File, functionName: String, func: => Exp[A => B], graphVizConfig: GraphVizConfig)
                           (implicit compilerConfig: CompilerConfig): CompilerOutput[A, B] = {
    buildExecutable(sourcesAndExecutableDir, sourcesAndExecutableDir, functionName, func, graphVizConfig)(compilerConfig)
  }
  protected def doBuildExecutable[A, B](sourcesDir: File, executableDir: File, functionName: String, graph: PGraph, graphVizConfig: GraphVizConfig)
                                       (compilerConfig: CompilerConfig, eInput: Elem[A], eOutput: Elem[B]): CustomCompilerOutput

  // func is passed to enable inference of B and to get types if needed
  def execute[A, B](compilerOutput: CompilerOutput[A, B], input: A): B = {
    doExecute(compilerOutput, input)
  }

  protected def doExecute[A, B](compilerOutput: CompilerOutput[A, B], input: A): B
}

class CompilationException(message: String, cause: Exception) extends RuntimeException(message, cause)
