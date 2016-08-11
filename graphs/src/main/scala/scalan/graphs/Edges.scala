package scalan.graphs

import scalan.collections.CollectionsDsl
import scalan.Owner

trait Edges extends CollectionsDsl { self: GraphsDsl =>

  /**
   * Created by afilippov on 2/16/15.
   */
  trait Edge[V, E] extends Def[Edge[V,E]]{
    implicit def eV: Elem[V]

    implicit def eE: Elem[E]

    @Owner
    implicit def graph: Rep[Graph[V, E]]

    //private def indexOfTarget = this.graph.edgeValues.segOffsets(fromId) + outIndex

    def outIndex: Rep[Int]

    def fromId: Rep[Int]

    def toId: Rep[Int]

    //= this.graph.links.values(indexOfTarget)
    def fromNode: Rep[Vertex[V, E]]

    //= Vertex(fromId, graph)
    def toNode: Rep[Vertex[V, E]]

    //= Vertex(toId, graph)
    def value: Rep[E] //= this.graph.edgeValues.values(indexOfTarget)
  }

  trait EdgeCompanion extends TypeFamily2[Edge] {
  }
  abstract class AdjEdge[V, E](val fromId: Rep[Int], val outIndex: Rep[Int], val graph: Rep[Graph[V, E]])
                              (implicit val eV: Elem[V], val eE: Elem[E]) extends Edge[V, E] {
    private def indexOfTarget = graph.edgeValues.segOffsets(fromId) + outIndex

    def toId: Rep[Int] = graph.links.values(indexOfTarget)

    def fromNode: Rep[Vertex[V, E]] = SVertex(fromId, graph)

    def toNode: Rep[Vertex[V, E]] = SVertex(toId, graph)

    def value: Rep[E] = graph.edgeValues.values(indexOfTarget)
  }

  trait AdjEdgeCompanion extends ConcreteClass2[Edge]

  abstract class IncEdge[V, E](val fromId: Rep[Int], val toId: Rep[Int], val graph: Rep[Graph[V, E]])
                              (implicit val eV: Elem[V], val eE: Elem[E]) extends Edge[V, E] {
    private def indexOfTarget = fromId*graph.vertexNum + toId
    //def toId: Rep[Int] = graph.links.values(indexOfTarget)
    def outIndex: Rep[Int] = ???

    def fromNode: Rep[Vertex[V, E]] = SVertex(fromId, graph)

    def toNode: Rep[Vertex[V, E]] = SVertex(toId, graph)

    def value: Rep[E] = graph.incMatrixWithVals(indexOfTarget)
  }
  trait IncEdgeCompanion extends ConcreteClass2[Edge]

}
