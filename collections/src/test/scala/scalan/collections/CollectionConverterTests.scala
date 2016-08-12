package scalan.collections

import scala.language.reflectiveCalls
import scalan._

class CollectionConverterTests extends BaseCtxTests {

  trait ConvProg extends ScalanDsl with CollectionsDsl {
    lazy val t1 = fun { (in: Rep[PairCollectionSOA[Int,Double]]) => in.convertTo[PairCollectionAOS[Int, Double]] }
    lazy val t2 = fun { (in: Rep[(Array[Int], Array[Double])]) =>
      val Pair(as, bs) = in
      val ps = Collection.fromArray(as) zip Collection.fromArray(bs)
      val ps1 = ps.convertTo[PairCollectionAOS[Int, Double]]
      ps1.arr
    }
    lazy val t3 = fun { (in: Rep[Array[(Int,Double)]]) => {
      val ps = PairCollectionAOS(CollectionOverArray(in))
      val Pair(as, bs) = ps.convertTo[PairCollectionSOA[Int, Double]].toData
      Pair(as.arr, bs.arr)
    } }
    lazy val t4 = fun { (in: Rep[Array[Int]]) => CollectionOverArray(in).convertTo[CollectionOverSeq[Int]].toData }
    lazy val t5 = fun { (in: Rep[SSeq[Int]]) => CollectionOverSeq(in).convertTo[CollectionOverArray[Int]].toData }
    lazy val t6 = fun { (in: Rep[Array[(Int,Int)]]) =>
      val in0 = CollectionOverArray(in)
      val in1 = in0.map(i => (i._1 + i._2, i._2) )
      (in1.arr)
    }
  }

  class ConvProgStaged extends TestContext with ConvProg with CollectionsDslExp {
  }
  class ConvProgStd extends ScalanDslStd with ConvProg with CollectionsDslStd {
  }

  test("convert") {
    val ctx = new ConvProgStaged
    ctx.emit("t1", ctx.t1)
    ctx.emit("t2", ctx.t2)
    ctx.emit("t3", ctx.t3)
    ctx.emit("t4", ctx.t4)
    ctx.emit("t5", ctx.t5)
    ctx.emit("t6", ctx.t6)
  }


  test("convertSeq") {
    val ctx = new ConvProgStd
    import ctx._
    {
      val res = ctx.t2((Array(10, 20), Array(10, 20)))
      assertResult(Array((10, 10), (20, 20)))(res)
    }
    {
      val res = ctx.t3(Array((10, 10.0), (20, 20.0)))
      assertResult(Array(10, 20))(res._1)
      assertResult(Array(10.0, 20.0))(res._2)
    }
    {
      val res = ctx.t4(Array(10, 20))
      assertResult(SSeqImpl(Seq(10, 20)))(res)
    }
    {
      val res = ctx.t5(SSeqImpl(Seq(10, 20)))
      assertResult(Array(10, 20))(res)
    }
    {
      val res = ctx.t6(Array((10,10), (20,20)))
      assertResult(Array((20,10), (40,20)))(res)
    }
  }

  test("tryComposeIso_with_help_of_converters") {
    val ctx = new ConvProgStaged {
      //      override def isInvokeEnabled(d: Def[_], m: Method) = false
      override def shouldUnpack(e: Elem[_]) = false
    }
    import ctx._

    def test[A,B1,B2 >: B1,C](name: String, iso1: Iso[A, B1], iso2: Iso[B2, C]) = {
      val Some(Def(iso: IsoUR[A,C] @unchecked)) = tryComposeIso(iso2, iso1.asInstanceOf[Iso[A,B2]])
      implicit val eA = iso1.eFrom
      implicit val eC = iso2.eTo
      val to = iso.toFun
      val from = iso.fromFun
      val idA @ Def(lA: Lambda[_,_]) = fun({ x: Rep[A] =>
        from(to(x))
      })
      val idC @ Def(lC: Lambda[_,_]) = fun({ x: Rep[C] =>
        to(from(x))
      })
      assert(lA.isIdentity)
      assert(lA.elem.eDom == lA.elem.eRange)
      assert(lC.isIdentity)
      assert(lC.elem.eDom == lC.elem.eRange)
      ctx.emit(name + "_iso1", iso1.toFun, iso1.fromFun)
      ctx.emit(name + "_iso2", iso2.toFun, iso2.fromFun)
      ctx.emit(name + "_composed", to, from)
      ctx.emit(name + "_identities", idA, idC)
    }

    test("t1", isoCollectionOverArray[(Int,Double)], isoPairCollectionAOS[Int, Double])
    test("t2", isoCollectionOverList[(Int,Double)], isoPairCollectionAOS[Int, Double])
    test("t3", isoCollectionOverSeq[(Int,Double)], isoPairCollectionAOS[Int, Double])
    test("t4", isoPairCollectionSOA[Int,Double], isoPairCollectionAOS[Int, Double])
  }

}
