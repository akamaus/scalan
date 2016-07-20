package scalan.monads

import scalan._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait FreeMsAbs extends scalan.ScalanDsl with FreeMs {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyFreeM[F[_], A](p: Rep[FreeM[F, A]]): FreeM[F, A] = {
    proxyOps[FreeM[F, A]](p)(scala.reflect.classTag[FreeM[F, A]])
  }

  // familyElem
  class FreeMElem[F[_], A, To <: FreeM[F, A]](implicit _cF: Cont[F], _eA: Elem[A])
    extends EntityElem[To] {
    def cF = _cF
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[FreeM[F, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[FreeM[F, A]] => convertFreeM(x) }
      tryConvert(element[FreeM[F, A]], this, x, conv)
    }

    def convertFreeM(x: Rep[FreeM[F, A]]): Rep[To] = {
      x.selfType1.asInstanceOf[Elem[_]] match {
        case _: FreeMElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have FreeMElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def freeMElement[F[_], A](implicit cF: Cont[F], eA: Elem[A]): Elem[FreeM[F, A]] =
    cachedElem[FreeMElem[F, A, FreeM[F, A]]](cF, eA)

  implicit case object FreeMCompanionElem extends CompanionElem[FreeMCompanionAbs] {
    lazy val tag = weakTypeTag[FreeMCompanionAbs]
    protected def getDefaultRep = FreeM
  }

  abstract class FreeMCompanionAbs extends CompanionDef[FreeMCompanionAbs] with FreeMCompanion {
    def selfType = FreeMCompanionElem
    override def toString = "FreeM"
  }
  def FreeM: Rep[FreeMCompanionAbs]
  implicit def proxyFreeMCompanionAbs(p: Rep[FreeMCompanionAbs]): FreeMCompanionAbs =
    proxyOps[FreeMCompanionAbs](p)

  abstract class AbsDone[F[_], A]
      (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends Done[F, A](a) with Def[Done[F, A]] {
    lazy val selfType = element[Done[F, A]]
  }
  // elem for concrete class
  class DoneElem[F[_], A](val iso: Iso[DoneData[F, A], Done[F, A]])(implicit override val eA: Elem[A], override val cF: Cont[F])
    extends FreeMElem[F, A, Done[F, A]]
    with ConcreteElem[DoneData[F, A], Done[F, A]] {
    override lazy val parent: Option[Elem[_]] = Some(freeMElement(container[F], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)

    override def convertFreeM(x: Rep[FreeM[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from FreeM to Done: missing fields List(a)")
    override def getDefaultRep = Done(element[A].defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Done[F, A]]
    }
  }

  // state representation type
  type DoneData[F[_], A] = A

  // 3) Iso for concrete class
  class DoneIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends EntityIso[DoneData[F, A], Done[F, A]] with Def[DoneIso[F, A]] {
    override def from(p: Rep[Done[F, A]]) =
      p.a
    override def to(p: Rep[A]) = {
      val a = p
      Done(a)
    }
    lazy val eFrom = element[A]
    lazy val eTo = new DoneElem[F, A](self)
    lazy val selfType = new DoneIsoElem[F, A](eA, cF)
    def productArity = 2
    def productElement(n: Int) = (eA, cF).productElement(n)
  }
  case class DoneIsoElem[F[_], A](eA: Elem[A], cF: Cont[F]) extends Elem[DoneIso[F, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new DoneIso[F, A]()(eA, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[DoneIso[F, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)
  }
  // 4) constructor and deconstructor
  class DoneCompanionAbs extends CompanionDef[DoneCompanionAbs] with DoneCompanion {
    def selfType = DoneCompanionElem
    override def toString = "Done"

    @scalan.OverloadId("fromFields")
    def apply[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]] =
      mkDone(a)

    def unapply[F[_], A](p: Rep[FreeM[F, A]]) = unmkDone(p)
  }
  lazy val DoneRep: Rep[DoneCompanionAbs] = new DoneCompanionAbs
  lazy val Done: DoneCompanionAbs = proxyDoneCompanion(DoneRep)
  implicit def proxyDoneCompanion(p: Rep[DoneCompanionAbs]): DoneCompanionAbs = {
    proxyOps[DoneCompanionAbs](p)
  }

  implicit case object DoneCompanionElem extends CompanionElem[DoneCompanionAbs] {
    lazy val tag = weakTypeTag[DoneCompanionAbs]
    protected def getDefaultRep = Done
  }

  implicit def proxyDone[F[_], A](p: Rep[Done[F, A]]): Done[F, A] =
    proxyOps[Done[F, A]](p)

  implicit class ExtendedDone[F[_], A](p: Rep[Done[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[DoneData[F, A]] = isoDone(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoDone[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[DoneData[F, A], Done[F, A]] =
    reifyObject(new DoneIso[F, A]()(eA, cF))

  // 6) smart constructor and deconstructor
  def mkDone[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]]
  def unmkDone[F[_], A](p: Rep[FreeM[F, A]]): Option[(Rep[A])]

  abstract class AbsMore[F[_], A]
      (k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F])
    extends More[F, A](k) with Def[More[F, A]] {
    lazy val selfType = element[More[F, A]]
  }
  // elem for concrete class
  class MoreElem[F[_], A](val iso: Iso[MoreData[F, A], More[F, A]])(implicit override val eA: Elem[A], override val cF: Cont[F])
    extends FreeMElem[F, A, More[F, A]]
    with ConcreteElem[MoreData[F, A], More[F, A]] {
    override lazy val parent: Option[Elem[_]] = Some(freeMElement(container[F], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)

    override def convertFreeM(x: Rep[FreeM[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from FreeM to More: missing fields List(k)")
    override def getDefaultRep = More(cF.lift(element[FreeM[F, A]]).defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[More[F, A]]
    }
  }

  // state representation type
  type MoreData[F[_], A] = F[FreeM[F, A]]

  // 3) Iso for concrete class
  class MoreIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends EntityIso[MoreData[F, A], More[F, A]] with Def[MoreIso[F, A]] {
    override def from(p: Rep[More[F, A]]) =
      p.k
    override def to(p: Rep[F[FreeM[F, A]]]) = {
      val k = p
      More(k)
    }
    lazy val eFrom = element[F[FreeM[F, A]]]
    lazy val eTo = new MoreElem[F, A](self)
    lazy val selfType = new MoreIsoElem[F, A](eA, cF)
    def productArity = 2
    def productElement(n: Int) = (eA, cF).productElement(n)
  }
  case class MoreIsoElem[F[_], A](eA: Elem[A], cF: Cont[F]) extends Elem[MoreIso[F, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new MoreIso[F, A]()(eA, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[MoreIso[F, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)
  }
  // 4) constructor and deconstructor
  class MoreCompanionAbs extends CompanionDef[MoreCompanionAbs] with MoreCompanion {
    def selfType = MoreCompanionElem
    override def toString = "More"

    @scalan.OverloadId("fromFields")
    def apply[F[_], A](k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]] =
      mkMore(k)

    def unapply[F[_], A](p: Rep[FreeM[F, A]]) = unmkMore(p)
  }
  lazy val MoreRep: Rep[MoreCompanionAbs] = new MoreCompanionAbs
  lazy val More: MoreCompanionAbs = proxyMoreCompanion(MoreRep)
  implicit def proxyMoreCompanion(p: Rep[MoreCompanionAbs]): MoreCompanionAbs = {
    proxyOps[MoreCompanionAbs](p)
  }

  implicit case object MoreCompanionElem extends CompanionElem[MoreCompanionAbs] {
    lazy val tag = weakTypeTag[MoreCompanionAbs]
    protected def getDefaultRep = More
  }

  implicit def proxyMore[F[_], A](p: Rep[More[F, A]]): More[F, A] =
    proxyOps[More[F, A]](p)

  implicit class ExtendedMore[F[_], A](p: Rep[More[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[MoreData[F, A]] = isoMore(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoMore[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[MoreData[F, A], More[F, A]] =
    reifyObject(new MoreIso[F, A]()(eA, cF))

  // 6) smart constructor and deconstructor
  def mkMore[F[_], A](k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]]
  def unmkMore[F[_], A](p: Rep[FreeM[F, A]]): Option[(Rep[F[FreeM[F, A]]])]

  abstract class AbsFlatMap[F[_], S, B]
      (a: Rep[FreeM[F, S]], f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends FlatMap[F, S, B](a, f) with Def[FlatMap[F, S, B]] {
    lazy val selfType = element[FlatMap[F, S, B]]
  }
  // elem for concrete class
  class FlatMapElem[F[_], S, B](val iso: Iso[FlatMapData[F, S, B], FlatMap[F, S, B]])(implicit val eS: Elem[S], override val eA: Elem[B], override val cF: Cont[F])
    extends FreeMElem[F, B, FlatMap[F, S, B]]
    with ConcreteElem[FlatMapData[F, S, B], FlatMap[F, S, B]] {
    override lazy val parent: Option[Elem[_]] = Some(freeMElement(container[F], element[B]))
    override lazy val typeArgs = TypeArgs("F" -> cF, "S" -> eS, "B" -> eA)

    override def convertFreeM(x: Rep[FreeM[F, B]]) = // Converter is not generated by meta
!!!("Cannot convert from FreeM to FlatMap: missing fields List(a, f)")
    override def getDefaultRep = FlatMap(element[FreeM[F, S]].defaultRepValue, constFun[S, FreeM[F, B]](element[FreeM[F, B]].defaultRepValue))
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[FlatMap[F, S, B]]
    }
  }

  // state representation type
  type FlatMapData[F[_], S, B] = (FreeM[F, S], S => FreeM[F, B])

  // 3) Iso for concrete class
  class FlatMapIso[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends EntityIso[FlatMapData[F, S, B], FlatMap[F, S, B]] with Def[FlatMapIso[F, S, B]] {
    override def from(p: Rep[FlatMap[F, S, B]]) =
      Pair(p.a, p.f)
    override def to(p: Rep[(FreeM[F, S], S => FreeM[F, B])]) = {
      val Pair(a, f) = p
      FlatMap(a, f)
    }
    lazy val eFrom = pairElement(element[FreeM[F, S]], element[S => FreeM[F, B]])
    lazy val eTo = new FlatMapElem[F, S, B](self)
    lazy val selfType = new FlatMapIsoElem[F, S, B](eS, eA, cF)
    def productArity = 3
    def productElement(n: Int) = (eS, eA, cF).productElement(n)
  }
  case class FlatMapIsoElem[F[_], S, B](eS: Elem[S], eA: Elem[B], cF: Cont[F]) extends Elem[FlatMapIso[F, S, B]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new FlatMapIso[F, S, B]()(eS, eA, cF))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[FlatMapIso[F, S, B]]
    }
    lazy val typeArgs = TypeArgs("F" -> cF, "S" -> eS, "B" -> eA)
  }
  // 4) constructor and deconstructor
  class FlatMapCompanionAbs extends CompanionDef[FlatMapCompanionAbs] with FlatMapCompanion {
    def selfType = FlatMapCompanionElem
    override def toString = "FlatMap"
    @scalan.OverloadId("fromData")
    def apply[F[_], S, B](p: Rep[FlatMapData[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
      isoFlatMap(eS, eA, cF).to(p)
    @scalan.OverloadId("fromFields")
    def apply[F[_], S, B](a: Rep[FreeM[F, S]], f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
      mkFlatMap(a, f)

    def unapply[F[_], S, B](p: Rep[FreeM[F, B]]) = unmkFlatMap(p)
  }
  lazy val FlatMapRep: Rep[FlatMapCompanionAbs] = new FlatMapCompanionAbs
  lazy val FlatMap: FlatMapCompanionAbs = proxyFlatMapCompanion(FlatMapRep)
  implicit def proxyFlatMapCompanion(p: Rep[FlatMapCompanionAbs]): FlatMapCompanionAbs = {
    proxyOps[FlatMapCompanionAbs](p)
  }

  implicit case object FlatMapCompanionElem extends CompanionElem[FlatMapCompanionAbs] {
    lazy val tag = weakTypeTag[FlatMapCompanionAbs]
    protected def getDefaultRep = FlatMap
  }

  implicit def proxyFlatMap[F[_], S, B](p: Rep[FlatMap[F, S, B]]): FlatMap[F, S, B] =
    proxyOps[FlatMap[F, S, B]](p)

  implicit class ExtendedFlatMap[F[_], S, B](p: Rep[FlatMap[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]) {
    def toData: Rep[FlatMapData[F, S, B]] = isoFlatMap(eS, eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoFlatMap[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Iso[FlatMapData[F, S, B], FlatMap[F, S, B]] =
    reifyObject(new FlatMapIso[F, S, B]()(eS, eA, cF))

  // 6) smart constructor and deconstructor
  def mkFlatMap[F[_], S, B](a: Rep[FreeM[F, S]], f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]]
  def unmkFlatMap[F[_], S, B](p: Rep[FreeM[F, B]]): Option[(Rep[FreeM[F, S]], Rep[S => FreeM[F, B]])]

  registerModule(FreeMs_Module)
}

// Std -----------------------------------
trait FreeMsStd extends scalan.ScalanDslStd with FreeMsDsl {
  self: MonadsDslStd =>
  lazy val FreeM: Rep[FreeMCompanionAbs] = new FreeMCompanionAbs {
  }

  case class StdDone[F[_], A]
      (override val a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsDone[F, A](a) {
  }

  def mkDone[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]] =
    new StdDone[F, A](a)
  def unmkDone[F[_], A](p: Rep[FreeM[F, A]]) = p match {
    case p: Done[F, A] @unchecked =>
      Some((p.a))
    case _ => None
  }

  case class StdMore[F[_], A]
      (override val k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsMore[F, A](k) {
  }

  def mkMore[F[_], A]
    (k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]] =
    new StdMore[F, A](k)
  def unmkMore[F[_], A](p: Rep[FreeM[F, A]]) = p match {
    case p: More[F, A] @unchecked =>
      Some((p.k))
    case _ => None
  }

  case class StdFlatMap[F[_], S, B]
      (override val a: Rep[FreeM[F, S]], override val f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends AbsFlatMap[F, S, B](a, f) {
  }

  def mkFlatMap[F[_], S, B]
    (a: Rep[FreeM[F, S]], f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
    new StdFlatMap[F, S, B](a, f)
  def unmkFlatMap[F[_], S, B](p: Rep[FreeM[F, B]]) = p match {
    case p: FlatMap[F, S, B] @unchecked =>
      Some((p.a, p.f))
    case _ => None
  }
}

// Exp -----------------------------------
trait FreeMsExp extends scalan.ScalanDslExp with FreeMsDsl {
  self: MonadsDslExp =>
  lazy val FreeM: Rep[FreeMCompanionAbs] = new FreeMCompanionAbs {
  }

  case class ExpDone[F[_], A]
      (override val a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsDone[F, A](a)

  object DoneMethods {
    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[Done[F, A]], Rep[A => FreeM[F, B]]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: DoneElem[_, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Done[F, A]], Rep[A => FreeM[F, B]]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Done[F, A]], Rep[A => FreeM[F, B]]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[Done[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: DoneElem[_, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[Done[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Done[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resumeFlatMap {
      def unapply(d: Def[_]): Option[(Rep[Done[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: DoneElem[_, _] => true; case _ => false }) && method.getName == "resumeFlatMap" =>
          Some((receiver, f, fF)).asInstanceOf[Option[(Rep[Done[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Done[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object DoneCompanionMethods {
  }

  def mkDone[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]] =
    new ExpDone[F, A](a)
  def unmkDone[F[_], A](p: Rep[FreeM[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: DoneElem[F, A] @unchecked =>
      Some((p.asRep[Done[F, A]].a))
    case _ =>
      None
  }

  case class ExpMore[F[_], A]
      (override val k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsMore[F, A](k)

  object MoreMethods {
    object resume {
      def unapply(d: Def[_]): Option[(Rep[More[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: MoreElem[_, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[More[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[More[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resumeFlatMap {
      def unapply(d: Def[_]): Option[(Rep[More[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: MoreElem[_, _] => true; case _ => false }) && method.getName == "resumeFlatMap" =>
          Some((receiver, f, fF)).asInstanceOf[Option[(Rep[More[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[More[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object MoreCompanionMethods {
  }

  def mkMore[F[_], A]
    (k: Rep[F[FreeM[F, A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]] =
    new ExpMore[F, A](k)
  def unmkMore[F[_], A](p: Rep[FreeM[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: MoreElem[F, A] @unchecked =>
      Some((p.asRep[More[F, A]].k))
    case _ =>
      None
  }

  case class ExpFlatMap[F[_], S, B]
      (override val a: Rep[FreeM[F, S]], override val f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends AbsFlatMap[F, S, B](a, f)

  object FlatMapMethods {
    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F, R]]) forSome {type F[_]; type S; type B; type R}] = d match {
        case MethodCall(receiver, method, Seq(f1, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FlatMapElem[_, _, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f1)).asInstanceOf[Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F, R]]) forSome {type F[_]; type S; type B; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F, R]]) forSome {type F[_]; type S; type B; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[FlatMap[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FlatMapElem[_, _, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[FlatMap[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FlatMap[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resumeFlatMap {
      def unapply(d: Def[_]): Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F, C]], Functor[F]) forSome {type F[_]; type S; type B; type C}] = d match {
        case MethodCall(receiver, method, Seq(g, fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FlatMapElem[_, _, _] => true; case _ => false }) && method.getName == "resumeFlatMap" =>
          Some((receiver, g, fF)).asInstanceOf[Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F, C]], Functor[F]) forSome {type F[_]; type S; type B; type C}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F, C]], Functor[F]) forSome {type F[_]; type S; type B; type C}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object FlatMapCompanionMethods {
  }

  def mkFlatMap[F[_], S, B]
    (a: Rep[FreeM[F, S]], f: Rep[S => FreeM[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
    new ExpFlatMap[F, S, B](a, f)
  def unmkFlatMap[F[_], S, B](p: Rep[FreeM[F, B]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: FlatMapElem[F, S, B] @unchecked =>
      Some((p.asRep[FlatMap[F, S, B]].a, p.asRep[FlatMap[F, S, B]].f))
    case _ =>
      None
  }

  object FreeMMethods {
    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F, B]]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F, B]]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F, B]]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object mapBy {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "mapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[FreeM[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[FreeM[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resumeFlatMap {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "resumeFlatMap" =>
          Some((receiver, f, fF)).asInstanceOf[Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F, B]], Functor[F]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object FreeMCompanionMethods {
  }
}

object FreeMs_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAANVXS4gcRRiunt3Z2ZlZN2viO8Rdl8lqfMwEc4iwyDK7MyMJM7vLdiQyBpea7ppJJ91dbXfN0uMhgocc1JOIoOAhoHgJgnjzIDkoSBBB8eZJUBBiRHIwJ8W/qh/T8+jZR+KCfSi6qv/+H9///VV/Xb2Jko6NFhwF69jMG4ThvCzeiw7LyWWTaaxTo2pbJyXS/P6pazTzxturCTRTRxPnsVNy9DpKey9l1wrfZaZWURqbCnEYtR2GHqsKCwWF6jpRmEbNgmYYbYYbOilUNYctVtF4g6qdV9ElJFXRjEJNxSaMyCs6dhzi+OuThHukhfO0mHfWrK4Ns8CjKESiOGNjjYH7YGPGk98gltwxqdkxGJr2XVuzuFsgkyWuBTGcMixdmBmropRmWNRmgdUUWDhP1WA6bmJYQAerF/AWLoDVVkFmtma2uDILKxdxi6yCCBcfhxgcojfPdCziK886TO2x51oIIcjKs8KxfBezfIhZnmOWk4mtYV17DfOP6zZ1O8h7pDGEXAtUPL2NikADKZtq7s1zysu35ayR4D+73JWUcGgCFM3GMESkB7D9ZuMd59YLV04mUKaOMppTbDjMxgqL0sCHK4tNkzLhc4ggtluQwfm4DAorRZDpo0laoYaFTdDkYzkFidI1RWNcmK9N+emJwT7FLBKISq4lhfHOxcQruLSCdX39xsPPHP29/FICJXpNpEGlDMVgB0oZSlZsQmq+bj4eYEiqdAHm06KY8iHtdsfUCFdCUB6/8Yf69XF0LhFC6VveWfZAxcHnPvjiKFn/NIEm64LsFR23RB45ViXiKHU0SbeI7a2ntrDO34bmMqWSJm7rzEc4Cs0YQMPQXGydWoTjtij4LwXhZz0Kr1KT5Crrub/k6+9e5Qy10ZT3xSvcf7STf/803WSCvIAnDpAdg2Lvgz4e64ynUqYGuXf+lvbKlbeYQFVye+t7rXEB6mlR/PfoCICDreezy5fv//OjzUOiPCYbGjOwlTu+i+IIuPwfkh+FqHisfLA758MsoHpPCbKwEjU7G5GPoPyIFGRQCDGUIMUA/vGyTowRGYlRoFRCBSvUZEOrKZpSkOPeit/DQjgSlyeBxAMb1fv0m0tfJlDyNEo2geFOFSUbtG2qAcRwLjHisuVgTeqFGCDFNjZCSMUzh7pIcVcjri8NldjsR2O42ABoWakXlTvZdwYyivoSIl0cUWGVu+XIoFMLYjy2E7bWqP0/Yiv3NsrW+MRvwyA+lPaRQst7ptCITXonhuUBwzF2mkPs2LBxx24Glbap/Hjq/UMHjmz+LE74CZUaWBMkOgx7gg2HgEjWYf+U7bpz58jtje0zcCKzGrb2SHh5FOGjQO+pYpa3V7D7ikn5Ee930UTyPFxgMK0R28dQb2hj0B7c3d1ygCK7ZNK0sD+ER2Eru9+ID4/2eteNBSjnfEw5l4iiY5uo/IJFDLgAet3VifeWzp5+6OyLor+bUoWQ9yVsUYdfV4Fxi+Jy9cSIyxUI5cqGBZdneDnx1fM/vP7tJx+L3rSLN0MTXqbhqAp8pyZWnTCk+ZiQZL+RA+pcuv3h6pPfff6r2KUyvCWEpscMr6rd5sTt22DTNWELbp4RcKH4eJMYIcwvfPjtXz9Hf48oEAAA"
}
}

