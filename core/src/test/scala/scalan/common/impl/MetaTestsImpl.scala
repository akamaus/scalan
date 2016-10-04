package scalan.common

import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait MetaTestsAbs extends scalan.ScalanDsl with MetaTests {
  self: MetaTestsDsl =>

  // single proxy for each type family
  implicit def proxyMetaTest[T](p: Rep[MetaTest[T]]): MetaTest[T] = {
    proxyOps[MetaTest[T]](p)(scala.reflect.classTag[MetaTest[T]])
  }

  // familyElem
  class MetaTestElem[T, To <: MetaTest[T]](implicit _elem: Elem[T])
    extends EntityElem[To] {
    def elem = _elem
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("T" -> elem)
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagT = elem.tag
      weakTypeTag[MetaTest[T]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[MetaTest[T]] => convertMetaTest(x) }
      tryConvert(element[MetaTest[T]], this, x, conv)
    }

    def convertMetaTest(x: Rep[MetaTest[T]]): Rep[To] = {
      x.selfType1 match {
        case _: MetaTestElem[_, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have MetaTestElem[_, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def metaTestElement[T](implicit elem: Elem[T]): Elem[MetaTest[T]] =
    cachedElem[MetaTestElem[T, MetaTest[T]]](elem)

  implicit case object MetaTestCompanionElem extends CompanionElem[MetaTestCompanionAbs] {
    lazy val tag = weakTypeTag[MetaTestCompanionAbs]
    protected def getDefaultRep = MetaTest
  }

  abstract class MetaTestCompanionAbs extends CompanionDef[MetaTestCompanionAbs] with MetaTestCompanion {
    def selfType = MetaTestCompanionElem
    override def toString = "MetaTest"
  }
  def MetaTest: Rep[MetaTestCompanionAbs]
  implicit def proxyMetaTestCompanionAbs(p: Rep[MetaTestCompanionAbs]): MetaTestCompanionAbs =
    proxyOps[MetaTestCompanionAbs](p)

  abstract class AbsMT0
      (size: Rep[Int])
    extends MT0(size) with Def[MT0] {
    lazy val selfType = element[MT0]
  }
  // elem for concrete class
  class MT0Elem(val iso: Iso[MT0Data, MT0])
    extends MetaTestElem[Unit, MT0]
    with ConcreteElem[MT0Data, MT0] {
    override lazy val parent: Option[Elem[_]] = Some(metaTestElement(UnitElement))
    override lazy val typeArgs = TypeArgs()

    override def convertMetaTest(x: Rep[MetaTest[Unit]]) = MT0(x.size)
    override def getDefaultRep = MT0(0)
    override lazy val tag = {
      weakTypeTag[MT0]
    }
  }

  // state representation type
  type MT0Data = Int

  // 3) Iso for concrete class
  class MT0Iso
    extends EntityIso[MT0Data, MT0] with Def[MT0Iso] {
    override def from(p: Rep[MT0]) =
      p.size
    override def to(p: Rep[Int]) = {
      val size = p
      MT0(size)
    }
    lazy val eFrom = element[Int]
    lazy val eTo = new MT0Elem(self)
    lazy val selfType = new MT0IsoElem
    def productArity = 0
    def productElement(n: Int) = ???
  }
  case class MT0IsoElem() extends Elem[MT0Iso] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new MT0Iso())
    lazy val tag = {
      weakTypeTag[MT0Iso]
    }
    lazy val typeArgs = TypeArgs()
  }
  // 4) constructor and deconstructor
  class MT0CompanionAbs extends CompanionDef[MT0CompanionAbs] with MT0Companion {
    def selfType = MT0CompanionElem
    override def toString = "MT0"

    @scalan.OverloadId("fromFields")
    def apply(size: Rep[Int]): Rep[MT0] =
      mkMT0(size)

    def unapply(p: Rep[MetaTest[Unit]]) = unmkMT0(p)
  }
  lazy val MT0Rep: Rep[MT0CompanionAbs] = new MT0CompanionAbs
  lazy val MT0: MT0CompanionAbs = proxyMT0Companion(MT0Rep)
  implicit def proxyMT0Companion(p: Rep[MT0CompanionAbs]): MT0CompanionAbs = {
    proxyOps[MT0CompanionAbs](p)
  }

  implicit case object MT0CompanionElem extends CompanionElem[MT0CompanionAbs] {
    lazy val tag = weakTypeTag[MT0CompanionAbs]
    protected def getDefaultRep = MT0
  }

  implicit def proxyMT0(p: Rep[MT0]): MT0 =
    proxyOps[MT0](p)

  implicit class ExtendedMT0(p: Rep[MT0]) {
    def toData: Rep[MT0Data] = isoMT0.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoMT0: Iso[MT0Data, MT0] =
    reifyObject(new MT0Iso())

  // 6) smart constructor and deconstructor
  def mkMT0(size: Rep[Int]): Rep[MT0]
  def unmkMT0(p: Rep[MetaTest[Unit]]): Option[(Rep[Int])]

  abstract class AbsMT1[T]
      (data: Rep[T], size: Rep[Int])(implicit elem: Elem[T])
    extends MT1[T](data, size) with Def[MT1[T]] {
    lazy val selfType = element[MT1[T]]
  }
  // elem for concrete class
  class MT1Elem[T](val iso: Iso[MT1Data[T], MT1[T]])(implicit override val elem: Elem[T])
    extends MetaTestElem[T, MT1[T]]
    with ConcreteElem[MT1Data[T], MT1[T]] {
    override lazy val parent: Option[Elem[_]] = Some(metaTestElement(element[T]))
    override lazy val typeArgs = TypeArgs("T" -> elem)

    override def convertMetaTest(x: Rep[MetaTest[T]]) = // Converter is not generated by meta
!!!("Cannot convert from MetaTest to MT1: missing fields List(data)")
    override def getDefaultRep = MT1(element[T].defaultRepValue, 0)
    override lazy val tag = {
      implicit val tagT = elem.tag
      weakTypeTag[MT1[T]]
    }
  }

  // state representation type
  type MT1Data[T] = (T, Int)

  // 3) Iso for concrete class
  class MT1Iso[T](implicit elem: Elem[T])
    extends EntityIso[MT1Data[T], MT1[T]] with Def[MT1Iso[T]] {
    override def from(p: Rep[MT1[T]]) =
      (p.data, p.size)
    override def to(p: Rep[(T, Int)]) = {
      val Pair(data, size) = p
      MT1(data, size)
    }
    lazy val eFrom = pairElement(element[T], element[Int])
    lazy val eTo = new MT1Elem[T](self)
    lazy val selfType = new MT1IsoElem[T](elem)
    def productArity = 1
    def productElement(n: Int) = elem
  }
  case class MT1IsoElem[T](elem: Elem[T]) extends Elem[MT1Iso[T]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new MT1Iso[T]()(elem))
    lazy val tag = {
      implicit val tagT = elem.tag
      weakTypeTag[MT1Iso[T]]
    }
    lazy val typeArgs = TypeArgs("T" -> elem)
  }
  // 4) constructor and deconstructor
  class MT1CompanionAbs extends CompanionDef[MT1CompanionAbs] {
    def selfType = MT1CompanionElem
    override def toString = "MT1"
    @scalan.OverloadId("fromData")
    def apply[T](p: Rep[MT1Data[T]])(implicit elem: Elem[T]): Rep[MT1[T]] =
      isoMT1(elem).to(p)
    @scalan.OverloadId("fromFields")
    def apply[T](data: Rep[T], size: Rep[Int])(implicit elem: Elem[T]): Rep[MT1[T]] =
      mkMT1(data, size)

    def unapply[T](p: Rep[MetaTest[T]]) = unmkMT1(p)
  }
  lazy val MT1Rep: Rep[MT1CompanionAbs] = new MT1CompanionAbs
  lazy val MT1: MT1CompanionAbs = proxyMT1Companion(MT1Rep)
  implicit def proxyMT1Companion(p: Rep[MT1CompanionAbs]): MT1CompanionAbs = {
    proxyOps[MT1CompanionAbs](p)
  }

  implicit case object MT1CompanionElem extends CompanionElem[MT1CompanionAbs] {
    lazy val tag = weakTypeTag[MT1CompanionAbs]
    protected def getDefaultRep = MT1
  }

  implicit def proxyMT1[T](p: Rep[MT1[T]]): MT1[T] =
    proxyOps[MT1[T]](p)

  implicit class ExtendedMT1[T](p: Rep[MT1[T]])(implicit elem: Elem[T]) {
    def toData: Rep[MT1Data[T]] = isoMT1(elem).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoMT1[T](implicit elem: Elem[T]): Iso[MT1Data[T], MT1[T]] =
    reifyObject(new MT1Iso[T]()(elem))

  // 6) smart constructor and deconstructor
  def mkMT1[T](data: Rep[T], size: Rep[Int])(implicit elem: Elem[T]): Rep[MT1[T]]
  def unmkMT1[T](p: Rep[MetaTest[T]]): Option[(Rep[T], Rep[Int])]

  abstract class AbsMT2[T, R]
      (indices: Rep[T], values: Rep[R], size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R])
    extends MT2[T, R](indices, values, size) with Def[MT2[T, R]] {
    lazy val selfType = element[MT2[T, R]]
  }
  // elem for concrete class
  class MT2Elem[T, R](val iso: Iso[MT2Data[T, R], MT2[T, R]])(implicit val eT: Elem[T], val eR: Elem[R])
    extends MetaTestElem[(T, R), MT2[T, R]]
    with ConcreteElem[MT2Data[T, R], MT2[T, R]] {
    override lazy val parent: Option[Elem[_]] = Some(metaTestElement(pairElement(element[T],element[R])))
    override lazy val typeArgs = TypeArgs("T" -> eT, "R" -> eR)

    override def convertMetaTest(x: Rep[MetaTest[(T, R)]]) = // Converter is not generated by meta
!!!("Cannot convert from MetaTest to MT2: missing fields List(indices, values)")
    override def getDefaultRep = MT2(element[T].defaultRepValue, element[R].defaultRepValue, 0)
    override lazy val tag = {
      implicit val tagT = eT.tag
      implicit val tagR = eR.tag
      weakTypeTag[MT2[T, R]]
    }
  }

  // state representation type
  type MT2Data[T, R] = (T, (R, Int))

  // 3) Iso for concrete class
  class MT2Iso[T, R](implicit eT: Elem[T], eR: Elem[R])
    extends EntityIso[MT2Data[T, R], MT2[T, R]] with Def[MT2Iso[T, R]] {
    override def from(p: Rep[MT2[T, R]]) =
      (p.indices, p.values, p.size)
    override def to(p: Rep[(T, (R, Int))]) = {
      val Pair(indices, Pair(values, size)) = p
      MT2(indices, values, size)
    }
    lazy val eFrom = pairElement(element[T], pairElement(element[R], element[Int]))
    lazy val eTo = new MT2Elem[T, R](self)
    lazy val selfType = new MT2IsoElem[T, R](eT, eR)
    def productArity = 2
    def productElement(n: Int) = n match {
      case 0 => eT
      case 1 => eR
    }
  }
  case class MT2IsoElem[T, R](eT: Elem[T], eR: Elem[R]) extends Elem[MT2Iso[T, R]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new MT2Iso[T, R]()(eT, eR))
    lazy val tag = {
      implicit val tagT = eT.tag
      implicit val tagR = eR.tag
      weakTypeTag[MT2Iso[T, R]]
    }
    lazy val typeArgs = TypeArgs("T" -> eT, "R" -> eR)
  }
  // 4) constructor and deconstructor
  class MT2CompanionAbs extends CompanionDef[MT2CompanionAbs] {
    def selfType = MT2CompanionElem
    override def toString = "MT2"
    @scalan.OverloadId("fromData")
    def apply[T, R](p: Rep[MT2Data[T, R]])(implicit eT: Elem[T], eR: Elem[R]): Rep[MT2[T, R]] =
      isoMT2(eT, eR).to(p)
    @scalan.OverloadId("fromFields")
    def apply[T, R](indices: Rep[T], values: Rep[R], size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R]): Rep[MT2[T, R]] =
      mkMT2(indices, values, size)

    def unapply[T, R](p: Rep[MetaTest[(T, R)]]) = unmkMT2(p)
  }
  lazy val MT2Rep: Rep[MT2CompanionAbs] = new MT2CompanionAbs
  lazy val MT2: MT2CompanionAbs = proxyMT2Companion(MT2Rep)
  implicit def proxyMT2Companion(p: Rep[MT2CompanionAbs]): MT2CompanionAbs = {
    proxyOps[MT2CompanionAbs](p)
  }

  implicit case object MT2CompanionElem extends CompanionElem[MT2CompanionAbs] {
    lazy val tag = weakTypeTag[MT2CompanionAbs]
    protected def getDefaultRep = MT2
  }

  implicit def proxyMT2[T, R](p: Rep[MT2[T, R]]): MT2[T, R] =
    proxyOps[MT2[T, R]](p)

  implicit class ExtendedMT2[T, R](p: Rep[MT2[T, R]])(implicit eT: Elem[T], eR: Elem[R]) {
    def toData: Rep[MT2Data[T, R]] = isoMT2(eT, eR).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoMT2[T, R](implicit eT: Elem[T], eR: Elem[R]): Iso[MT2Data[T, R], MT2[T, R]] =
    reifyObject(new MT2Iso[T, R]()(eT, eR))

  // 6) smart constructor and deconstructor
  def mkMT2[T, R](indices: Rep[T], values: Rep[R], size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R]): Rep[MT2[T, R]]
  def unmkMT2[T, R](p: Rep[MetaTest[(T, R)]]): Option[(Rep[T], Rep[R], Rep[Int])]

  registerModule(MetaTests_Module)
}

// Std -----------------------------------
trait MetaTestsStd extends scalan.ScalanDslStd with MetaTestsDsl {
  self: MetaTestsDslStd =>

  lazy val MetaTest: Rep[MetaTestCompanionAbs] = new MetaTestCompanionAbs {
  }

  case class StdMT0
      (override val size: Rep[Int])
    extends AbsMT0(size) {
  }

  def mkMT0
    (size: Rep[Int]): Rep[MT0] =
    new StdMT0(size)
  def unmkMT0(p: Rep[MetaTest[Unit]]) = p match {
    case p: MT0 @unchecked =>
      Some((p.size))
    case _ => None
  }

  case class StdMT1[T]
      (override val data: Rep[T], override val size: Rep[Int])(implicit elem: Elem[T])
    extends AbsMT1[T](data, size) {
  }

  def mkMT1[T]
    (data: Rep[T], size: Rep[Int])(implicit elem: Elem[T]): Rep[MT1[T]] =
    new StdMT1[T](data, size)
  def unmkMT1[T](p: Rep[MetaTest[T]]) = p match {
    case p: MT1[T] @unchecked =>
      Some((p.data, p.size))
    case _ => None
  }

  case class StdMT2[T, R]
      (override val indices: Rep[T], override val values: Rep[R], override val size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R])
    extends AbsMT2[T, R](indices, values, size) {
  }

  def mkMT2[T, R]
    (indices: Rep[T], values: Rep[R], size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R]): Rep[MT2[T, R]] =
    new StdMT2[T, R](indices, values, size)
  def unmkMT2[T, R](p: Rep[MetaTest[(T, R)]]) = p match {
    case p: MT2[T, R] @unchecked =>
      Some((p.indices, p.values, p.size))
    case _ => None
  }
}

// Exp -----------------------------------
trait MetaTestsExp extends scalan.ScalanDslExp with MetaTestsDsl {
  self: MetaTestsDslExp =>

  lazy val MetaTest: Rep[MetaTestCompanionAbs] = new MetaTestCompanionAbs {
  }

  case class ExpMT0
      (override val size: Rep[Int])
    extends AbsMT0(size)

  object MT0Methods {
    object test {
      def unapply(d: Def[_]): Option[Rep[MT0]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT0Elem] && method.getName == "test" =>
          Some(receiver).asInstanceOf[Option[Rep[MT0]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT0]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object give {
      def unapply(d: Def[_]): Option[Rep[MT0]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT0Elem] && method.getName == "give" =>
          Some(receiver).asInstanceOf[Option[Rep[MT0]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT0]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object elem {
      def unapply(d: Def[_]): Option[Rep[MT0]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT0Elem] && method.getName == "elem" =>
          Some(receiver).asInstanceOf[Option[Rep[MT0]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT0]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object MT0CompanionMethods {
  }

  def mkMT0
    (size: Rep[Int]): Rep[MT0] =
    new ExpMT0(size)
  def unmkMT0(p: Rep[MetaTest[Unit]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: MT0Elem @unchecked =>
      Some((p.asRep[MT0].size))
    case _ =>
      None
  }

  case class ExpMT1[T]
      (override val data: Rep[T], override val size: Rep[Int])(implicit elem: Elem[T])
    extends AbsMT1[T](data, size)

  object MT1Methods {
    object test {
      def unapply(d: Def[_]): Option[Rep[MT1[T]] forSome {type T}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT1Elem[_]] && method.getName == "test" =>
          Some(receiver).asInstanceOf[Option[Rep[MT1[T]] forSome {type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT1[T]] forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object give {
      def unapply(d: Def[_]): Option[Rep[MT1[T]] forSome {type T}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT1Elem[_]] && method.getName == "give" =>
          Some(receiver).asInstanceOf[Option[Rep[MT1[T]] forSome {type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT1[T]] forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  def mkMT1[T]
    (data: Rep[T], size: Rep[Int])(implicit elem: Elem[T]): Rep[MT1[T]] =
    new ExpMT1[T](data, size)
  def unmkMT1[T](p: Rep[MetaTest[T]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: MT1Elem[T] @unchecked =>
      Some((p.asRep[MT1[T]].data, p.asRep[MT1[T]].size))
    case _ =>
      None
  }

  case class ExpMT2[T, R]
      (override val indices: Rep[T], override val values: Rep[R], override val size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R])
    extends AbsMT2[T, R](indices, values, size)

  object MT2Methods {
    object test {
      def unapply(d: Def[_]): Option[Rep[MT2[T, R]] forSome {type T; type R}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT2Elem[_, _]] && method.getName == "test" =>
          Some(receiver).asInstanceOf[Option[Rep[MT2[T, R]] forSome {type T; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT2[T, R]] forSome {type T; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object give {
      def unapply(d: Def[_]): Option[Rep[MT2[T, R]] forSome {type T; type R}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MT2Elem[_, _]] && method.getName == "give" =>
          Some(receiver).asInstanceOf[Option[Rep[MT2[T, R]] forSome {type T; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MT2[T, R]] forSome {type T; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  def mkMT2[T, R]
    (indices: Rep[T], values: Rep[R], size: Rep[Int])(implicit eT: Elem[T], eR: Elem[R]): Rep[MT2[T, R]] =
    new ExpMT2[T, R](indices, values, size)
  def unmkMT2[T, R](p: Rep[MetaTest[(T, R)]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: MT2Elem[T, R] @unchecked =>
      Some((p.asRep[MT2[T, R]].indices, p.asRep[MT2[T, R]].values, p.asRep[MT2[T, R]].size))
    case _ =>
      None
  }

  object MetaTestMethods {
    object test {
      def unapply(d: Def[_]): Option[Rep[MetaTest[T]] forSome {type T}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MetaTestElem[_, _]] && method.getName == "test" =>
          Some(receiver).asInstanceOf[Option[Rep[MetaTest[T]] forSome {type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MetaTest[T]] forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object give {
      def unapply(d: Def[_]): Option[Rep[MetaTest[T]] forSome {type T}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MetaTestElem[_, _]] && method.getName == "give" =>
          Some(receiver).asInstanceOf[Option[Rep[MetaTest[T]] forSome {type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MetaTest[T]] forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object size {
      def unapply(d: Def[_]): Option[Rep[MetaTest[T]] forSome {type T}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MetaTestElem[_, _]] && method.getName == "size" =>
          Some(receiver).asInstanceOf[Option[Rep[MetaTest[T]] forSome {type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MetaTest[T]] forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object MetaTestCompanionMethods {
  }
}

object MetaTests_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVXTWwbRRSedX5sx6ZNU6EAEiQEl5QK7NCCihQhFBKXtnISK+tQ5FZB491xumV3dtgdWzaHwqkCekMIJCSEyo+4VEWIG5W4tEgIIQ5cOXMqoKoHKpBAvJn98fpnnVRVfRjtzL793nvf+97M+PIfaMx10OOuhk1M8xbhOK/K5yWX59Qi5QZvr9p6wyQrpL51/NN/TlvvTCfQZBWNn8XuimtWUdp7KLZY+KxyvYTSmGrE5bbjcvRoSXooaLZpEo0bNi0YltXguGaSQslw+WIJjdZsvf06Oo+UEprUbKo5hBN12cSuS1x/PUVEREY4T8t5e511fNCCyKIQyaLiYIND+OBj0rPfIExtU5u2LY72+KGtMxEW2GRIi0EOJyxmSjcjJZQ0LGY7PPCaBA9nbT2YjlIMC2iqdA43cQG8bhdU7hh0W4AxrL2Gt8kamAjzUcjBJWa90mbEB8+4XO/y12IIIajKYRlYvsNZPuQsLzjLqcQxsGm8gcXLsmO32sj7KSMItRhAPLkDRIBAilTPvXtGO31bzVgJ8XFLhJKUAY0D0EyMQmR5gNsfNt5zb7106WgCTVTRhOEu1VzuYI1HZeDTlcGU2lzGHDKInW2o4FxcBaWXJbDpkUlasy2GKSD5XGahUKahGVwYi7WsX54Y7pOckcBUaTElzHc2Jl+ppWVsmuUbDz514PfiKwmU6HaRBkgVmsEJQDlKrQJKBUgI4R+Lg2ek7BgWSLxJnr12dfPmd2tj0sOUTuq4YfKXsdkgnrp8fx3fwlXi4BMcjW5Sg4uldKszJodkFfI7f+NP/fsFdCYRVsVPYndCAIip5z769gApX0mgVFX2zTETb0tJCNpXiKtVUcpuEsdbTzaxKZ4GyiLpJ+0XK8ryCLDM0WxsyzMiSrAoW0kJ0s943bBmU5I7Vs79pf74/mUhdgdlvTfeHvCfcfTfX/fUuewDYNOF/pAh7eVoBLYOnwsx7udIWYDVE3Qg3xMerGpbZN/cLWPr0kUumVVa3dvFeu0ctOei/O6RISQHO9nXFy7cf/PzV/fLbkvVDG5hllu4g14LWuMe9hKSJHR2kenOXAwzwGx2tbKwHPU602sO1IJNz6uMEqnA3v728peVSuisuy4SPmL7UKgR6QgqrmOOYyreCxyDMFgzYsj1BSS/6Y9K6cUkJrECzNFiMBmWr6Tv6Q6+aOCH47ceqB6dVlc//GpmK4HGTqKxOnSmW0JjNbtB9UAWcDRz0uIvBmtKtyxABtjBVigD+ZtFnRoOInCnkg5pCUYqDWaSZ67+vfX2W8eZ7K++PXggU+F0Y6BQdi2XpEF1A/rurhQz3hQ7+zCMjR0x7oXqEqSyW83FAWwMA+jnXor2cFS0YiwPEdIQg374COYh1B3MCGytd7O1dCkmtPDm877HnuUBu+K+wOeArTF6mbgThnpjvNj5eB6aKx/TXCtEM7FDdHE3JRbcnb2T5MgHL5w6+cCpTdlrWV0aeW/CI3nwTX8Vs0V5Lz045F4KRrmixeB/Bzwcuf78L2/+9OUX8izucMVROqwMR/f54cMJZvkHnchqLiYr1T+3oNrnb3+8dujnb36Td6wJcQLC3YCGF/3o3apbAdnQPVzdIxSLJgT4SMk/EcNn/wP7B1NeaQ0AAA=="
}
}

trait MetaTestsDslStd extends impl.MetaTestsStd
trait MetaTestsDslExp extends impl.MetaTestsExp
