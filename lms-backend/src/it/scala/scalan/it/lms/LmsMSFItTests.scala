package scalan.it.lms

import scalan._
import scalan.compilation.lms._
import scalan.compilation.lms.scalac.{CommunityLmsCompilerScala, LmsCompilerScala}
import scalan.graphs._
import scalan.it.{BaseItTests, BaseCtxItTests}

trait MsfFuncs extends GraphExamples {
  lazy val msfFunAdjBase = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
    val segments = Collection.fromArray(in._3) zip Collection.fromArray(in._4)
    val links = NestedCollectionFlat(Collection.fromArray(in._1), segments)
    val edge_vals = NestedCollectionFlat(Collection.fromArray(in._2), segments)

    val vertex_vals = UnitCollection(segments.length)
    val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, links)
    val startFront = Front.emptyBaseFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }
  lazy val msfFunIncBase = fun { in: Rep[(Array[Double], Int)] =>
    val incMatrix = Collection.fromArray(in._1)
    val vertex_vals = UnitCollection(in._2)
    val graph = IncidenceGraph.fromAdjacencyMatrix(vertex_vals, incMatrix, in._2)
    val out_in = Collection.replicate(graph.vertexNum, UNVISITED).update(0, NO_PARENT)
    val startFront = Front.emptyBaseFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }
  lazy val msfFunAdjMap = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
    val segments = Collection.fromArray(in._3) zip Collection.fromArray(in._4)
    val links = NestedCollectionFlat(Collection.fromArray(in._1), segments)
    val edge_vals = NestedCollectionFlat(Collection.fromArray(in._2), segments)

    val vertex_vals = UnitCollection(segments.length)
    val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, links)
    val startFront = Front.emptyMapBasedFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }
  lazy val msfFunIncMap = fun { in: Rep[(Array[Double], Int)] =>
    val incMatrix = Collection.fromArray(in._1)
    val vertex_vals = UnitCollection(in._2)
    val graph = IncidenceGraph.fromAdjacencyMatrix(vertex_vals, incMatrix, in._2)
    val out_in = Collection.replicate(graph.vertexNum, UNVISITED).update(0, NO_PARENT)
    val startFront = Front.emptyMapBasedFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }

  lazy val msfFunAdjList = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
    val segments = Collection.fromArray(in._3) zip Collection.fromArray(in._4)
    val links = NestedCollectionFlat(Collection.fromArray(in._1), segments)
    val edge_vals = NestedCollectionFlat(Collection.fromArray(in._2), segments)

    val vertex_vals = UnitCollection(segments.length)
    val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, links)
    val startFront = Front.emptyListBasedFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }

  lazy val msfFunAdjColl = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
    val segments = Collection.fromArray(in._3) zip Collection.fromArray(in._4)
    val links = NestedCollectionFlat(Collection.fromArray(in._1), segments)
    val edge_vals = NestedCollectionFlat(Collection.fromArray(in._2), segments)

    val vertex_vals = UnitCollection(segments.length)
    val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, links)
    val startFront = Front.emptyCollBasedFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }

  lazy val msfFunIncList = fun { in: Rep[(Array[Double], Int)] =>
    val incMatrix = Collection.fromArray(in._1)
    val vertex_vals = UnitCollection(in._2)
    val graph = IncidenceGraph.fromAdjacencyMatrix(vertex_vals, incMatrix, in._2)
    val out_in = Collection.replicate(graph.vertexNum, UNVISITED).update(0, NO_PARENT)
    val startFront = Front.emptyListBasedFront(graph.vertexNum)
    val res = MSF_prime(graph, startFront)
    res.arr
  }

  def createGraph(links: Arr[Int], evalues: Rep[Array[Double]], ofs: Arr[Int], lens: Arr[Int]) = {
    val segments = Collection.fromArray(ofs) zip Collection.fromArray(lens)
    val linksColl = NestedCollectionFlat(Collection.fromArray(links), segments)
    val edge_vals = NestedCollectionFlat(Collection.fromArray(evalues), segments)

    val vertex_vals = UnitCollection(segments.length)
    val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, linksColl)
    graph
  }

  lazy val funFallingTestWithLists = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
    val Pair(links, Pair(evalues, Pair(ofs, lens))) = in
    val graph = createGraph(links, evalues, ofs, lens)
    val startFront = Front.emptyListBasedFront(graph.vertexNum)
    val res = fallingTest(graph, startFront)
    res.arr
  }
  lazy val funFallingTestWithArrays = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
    val Pair(links, Pair(evalues, Pair(ofs, lens))) = in
    val graph = createGraph(links, evalues, ofs, lens)
    val startFront = Front.emptyBaseFront(graph.vertexNum)
    val res = fallingTest(graph, startFront)
    res.arr
  }
}

trait GraphTestInputs {

  val graph = Array(
    Array(1, 8),
    Array(0, 2, 8),
    Array(1, 3, 5, 7),
    Array(2, 4, 5),
    Array(3, 5),
    Array(2, 3, 4, 6),
    Array(5, 7, 8),
    Array(2, 6, 8),
    Array(0, 1, 6, 7),
    Array(10,11),
    Array(9,11),
    Array(9,10)
  )
  val graphValues = Array(
    Array(4.0, 8.0),
    Array(4.0, 8.0, 11.0),
    Array(8.0, 7.0, 4.0, 2.0),
    Array(7.0, 9.0, 14.0),
    Array(9.0, 10.0),
    Array(4.0, 14.0, 10.0, 2.0),
    Array(2.0, 6.0, 1.0),
    Array(2.0, 6.0, 7.0),
    Array(8.0, 11.0, 1.0, 7.0),
    Array(1.0, 2.0),
    Array(1.0, 0.5),
    Array(2.0, 0.5)
  )

  val links = graph.flatMap(i => i)
  val edgeVals = graphValues.flatMap(i => i)
  val lens = graph.map(i => i.length)
  val offs = Array(0,2,5,9,12,14,18,21,24,28,30,32) //(Array(0) :+ lens.scan.slice(lens.length-1)
  val vertexNum = graph.length
  val incMatrix = (graph zip graphValues).flatMap { in =>
    val row = in._1
    val vals = in._2
    val zero = scala.Array.fill(vertexNum)(0.0)
    for (i <- row.indices) { zero(row(i)) = vals(i) }
    zero
  }
  val listInput = (links, (edgeVals, (offs, lens)))
  val matrixInput = (incMatrix, vertexNum)
}

class LmsMsfPrimeItTests extends BaseItTests[MsfFuncs](new ScalanCommunityDslSeq with GraphsDslSeq with MsfFuncs) with GraphTestInputs {
  class ProgExp extends ScalanCommunityDslExp with GraphsDslExp with MsfFuncs

  class CompExp extends CommunityLmsCompilerScala(new ProgExp) with CommunityBridge

  val progStaged = new CompExp
  val defaultCompilers = compilers(progStaged)

  def commonTestScenario[A, B](f: MsfFuncs => MsfFuncs#Rep[A => Array[B]])(input: A) = {
    val resSeq = f(progSeq).asInstanceOf[A => Array[B]](input)
    println("Seq: " + resSeq.mkString(", "))
    val Seq(Seq(resStaged)) = getStagedOutput(f)(input)
    println("Staged: " + resStaged.mkString(", "))
  }

  test("MSF_adjList") {
    commonTestScenario(_.msfFunAdjBase)(listInput)
  }

  test("MSF_adjMatrix") {
    commonTestScenario(_.msfFunIncBase)(matrixInput)
  }

  test("MSF_adjListMap") {
    commonTestScenario(_.msfFunAdjMap)(listInput)
  }

  test("MSF_adjMatrixMap") {
    commonTestScenario(_.msfFunIncMap)(matrixInput)
  }

  test("MSF_adjListList") {
    commonTestScenario(_.msfFunAdjList)(listInput)
  }

  test("MSF_adjListColl") {
    commonTestScenario(_.msfFunAdjColl)(listInput)
  }

  test("MSF_adjMatrixList") {
    commonTestScenario(_.msfFunIncList)(matrixInput)
  }

  test("fallingTest") {
    commonTestScenario(_.funFallingTestWithLists)(listInput)
  }

}
