package scalan

import scalan.staged.Expressions
import scalan.common.Lazy
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait ConvertersAbs extends Converters {
  self: Scalan =>

  // single proxy for each type family
  implicit def proxyConverter[T, R](p: Rep[Converter[T, R]]): Converter[T, R] = {
    proxyOps[Converter[T, R]](p)(scala.reflect.classTag[Converter[T, R]])
  }

  // familyElem
  class ConverterElem[T, R, To <: Converter[T, R]](implicit _eT: Elem[T], _eR: Elem[R])
    extends EntityElem[To] {
    def eT = _eT
    def eR = _eR
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("T" -> Left(eT), "R" -> Left(eR))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagT = eT.tag
      implicit val tagR = eR.tag
      weakTypeTag[Converter[T, R]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Converter[T, R]] => convertConverter(x) }
      tryConvert(element[Converter[T, R]], this, x, conv)
    }

    def convertConverter(x: Rep[Converter[T, R]]): Rep[To] = {
      x.selfType1 match {
        case _: ConverterElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have ConverterElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def converterElement[T, R](implicit eT: Elem[T], eR: Elem[R]): Elem[Converter[T, R]] =
    cachedElem[ConverterElem[T, R, Converter[T, R]]](eT, eR)

  implicit case object ConverterCompanionElem extends CompanionElem[ConverterCompanionAbs] {
    lazy val tag = weakTypeTag[ConverterCompanionAbs]
    protected def getDefaultRep = Converter
  }

  abstract class ConverterCompanionAbs extends CompanionDef[ConverterCompanionAbs] with ConverterCompanion {
    def selfType = ConverterCompanionElem
    override def toString = "Converter"
  }
  def Converter: Rep[ConverterCompanionAbs]
  implicit def proxyConverterCompanionAbs(p: Rep[ConverterCompanionAbs]): ConverterCompanionAbs =
    proxyOps[ConverterCompanionAbs](p)

  abstract class AbsBaseConverter[T, R]
      (convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R])
    extends BaseConverter[T, R](convFun) with Def[BaseConverter[T, R]] {
    lazy val selfType = element[BaseConverter[T, R]]
  }
  // elem for concrete class
  class BaseConverterElem[T, R](val iso: Iso[BaseConverterData[T, R], BaseConverter[T, R]])(implicit override val eT: Elem[T], override val eR: Elem[R])
    extends ConverterElem[T, R, BaseConverter[T, R]]
    with ConcreteElem[BaseConverterData[T, R], BaseConverter[T, R]] {
    override lazy val parent: Option[Elem[_]] = Some(converterElement(element[T], element[R]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("T" -> Left(eT), "R" -> Left(eR))
    }

    override def convertConverter(x: Rep[Converter[T, R]]) = BaseConverter(x.convFun)
    override def getDefaultRep = BaseConverter(constFun[T, R](element[R].defaultRepValue))
    override lazy val tag = {
      implicit val tagT = eT.tag
      implicit val tagR = eR.tag
      weakTypeTag[BaseConverter[T, R]]
    }
  }

  // state representation type
  type BaseConverterData[T, R] = T => R

  // 3) Iso for concrete class
  class BaseConverterIso[T, R](implicit eT: Elem[T], eR: Elem[R])
    extends EntityIso[BaseConverterData[T, R], BaseConverter[T, R]] with Def[BaseConverterIso[T, R]] {
    override def from(p: Rep[BaseConverter[T, R]]) =
      p.convFun
    override def to(p: Rep[T => R]) = {
      val convFun = p
      BaseConverter(convFun)
    }
    lazy val eFrom = element[T => R]
    lazy val eTo = new BaseConverterElem[T, R](self)
    lazy val selfType = new BaseConverterIsoElem[T, R](eT, eR)
    def productArity = 2
    def productElement(n: Int) = (eT, eR).productElement(n)
  }
  case class BaseConverterIsoElem[T, R](eT: Elem[T], eR: Elem[R]) extends Elem[BaseConverterIso[T, R]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new BaseConverterIso[T, R]()(eT, eR))
    lazy val tag = {
      implicit val tagT = eT.tag
      implicit val tagR = eR.tag
      weakTypeTag[BaseConverterIso[T, R]]
    }
  }
  // 4) constructor and deconstructor
  class BaseConverterCompanionAbs extends CompanionDef[BaseConverterCompanionAbs] with BaseConverterCompanion {
    def selfType = BaseConverterCompanionElem
    override def toString = "BaseConverter"

    @scalan.OverloadId("fromFields")
    def apply[T, R](convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R]): Rep[BaseConverter[T, R]] =
      mkBaseConverter(convFun)

    def unapply[T, R](p: Rep[Converter[T, R]]) = unmkBaseConverter(p)
  }
  lazy val BaseConverterRep: Rep[BaseConverterCompanionAbs] = new BaseConverterCompanionAbs
  lazy val BaseConverter: BaseConverterCompanionAbs = proxyBaseConverterCompanion(BaseConverterRep)
  implicit def proxyBaseConverterCompanion(p: Rep[BaseConverterCompanionAbs]): BaseConverterCompanionAbs = {
    proxyOps[BaseConverterCompanionAbs](p)
  }

  implicit case object BaseConverterCompanionElem extends CompanionElem[BaseConverterCompanionAbs] {
    lazy val tag = weakTypeTag[BaseConverterCompanionAbs]
    protected def getDefaultRep = BaseConverter
  }

  implicit def proxyBaseConverter[T, R](p: Rep[BaseConverter[T, R]]): BaseConverter[T, R] =
    proxyOps[BaseConverter[T, R]](p)

  implicit class ExtendedBaseConverter[T, R](p: Rep[BaseConverter[T, R]])(implicit eT: Elem[T], eR: Elem[R]) {
    def toData: Rep[BaseConverterData[T, R]] = isoBaseConverter(eT, eR).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoBaseConverter[T, R](implicit eT: Elem[T], eR: Elem[R]): Iso[BaseConverterData[T, R], BaseConverter[T, R]] =
    reifyObject(new BaseConverterIso[T, R]()(eT, eR))

  // 6) smart constructor and deconstructor
  def mkBaseConverter[T, R](convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R]): Rep[BaseConverter[T, R]]
  def unmkBaseConverter[T, R](p: Rep[Converter[T, R]]): Option[(Rep[T => R])]

  abstract class AbsPairConverter[A1, A2, B1, B2]
      (conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends PairConverter[A1, A2, B1, B2](conv1, conv2) with Def[PairConverter[A1, A2, B1, B2]] {
    lazy val selfType = element[PairConverter[A1, A2, B1, B2]]
  }
  // elem for concrete class
  class PairConverterElem[A1, A2, B1, B2](val iso: Iso[PairConverterData[A1, A2, B1, B2], PairConverter[A1, A2, B1, B2]])(implicit val eA1: Elem[A1], val eA2: Elem[A2], val eB1: Elem[B1], val eB2: Elem[B2])
    extends ConverterElem[(A1, A2), (B1, B2), PairConverter[A1, A2, B1, B2]]
    with ConcreteElem[PairConverterData[A1, A2, B1, B2], PairConverter[A1, A2, B1, B2]] {
    override lazy val parent: Option[Elem[_]] = Some(converterElement(pairElement(element[A1],element[A2]), pairElement(element[B1],element[B2])))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("A1" -> Left(eA1), "A2" -> Left(eA2), "B1" -> Left(eB1), "B2" -> Left(eB2))
    }

    override def convertConverter(x: Rep[Converter[(A1, A2), (B1, B2)]]) = // Converter is not generated by meta
!!!("Cannot convert from Converter to PairConverter: missing fields List(conv1, conv2)")
    override def getDefaultRep = PairConverter(element[Converter[A1, B1]].defaultRepValue, element[Converter[A2, B2]].defaultRepValue)
    override lazy val tag = {
      implicit val tagA1 = eA1.tag
      implicit val tagA2 = eA2.tag
      implicit val tagB1 = eB1.tag
      implicit val tagB2 = eB2.tag
      weakTypeTag[PairConverter[A1, A2, B1, B2]]
    }
  }

  // state representation type
  type PairConverterData[A1, A2, B1, B2] = (Converter[A1, B1], Converter[A2, B2])

  // 3) Iso for concrete class
  class PairConverterIso[A1, A2, B1, B2](implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends EntityIso[PairConverterData[A1, A2, B1, B2], PairConverter[A1, A2, B1, B2]] with Def[PairConverterIso[A1, A2, B1, B2]] {
    override def from(p: Rep[PairConverter[A1, A2, B1, B2]]) =
      (p.conv1, p.conv2)
    override def to(p: Rep[(Converter[A1, B1], Converter[A2, B2])]) = {
      val Pair(conv1, conv2) = p
      PairConverter(conv1, conv2)
    }
    lazy val eFrom = pairElement(element[Converter[A1, B1]], element[Converter[A2, B2]])
    lazy val eTo = new PairConverterElem[A1, A2, B1, B2](self)
    lazy val selfType = new PairConverterIsoElem[A1, A2, B1, B2](eA1, eA2, eB1, eB2)
    def productArity = 4
    def productElement(n: Int) = (eA1, eA2, eB1, eB2).productElement(n)
  }
  case class PairConverterIsoElem[A1, A2, B1, B2](eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]) extends Elem[PairConverterIso[A1, A2, B1, B2]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new PairConverterIso[A1, A2, B1, B2]()(eA1, eA2, eB1, eB2))
    lazy val tag = {
      implicit val tagA1 = eA1.tag
      implicit val tagA2 = eA2.tag
      implicit val tagB1 = eB1.tag
      implicit val tagB2 = eB2.tag
      weakTypeTag[PairConverterIso[A1, A2, B1, B2]]
    }
  }
  // 4) constructor and deconstructor
  class PairConverterCompanionAbs extends CompanionDef[PairConverterCompanionAbs] with PairConverterCompanion {
    def selfType = PairConverterCompanionElem
    override def toString = "PairConverter"
    @scalan.OverloadId("fromData")
    def apply[A1, A2, B1, B2](p: Rep[PairConverterData[A1, A2, B1, B2]])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[PairConverter[A1, A2, B1, B2]] =
      isoPairConverter(eA1, eA2, eB1, eB2).to(p)
    @scalan.OverloadId("fromFields")
    def apply[A1, A2, B1, B2](conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[PairConverter[A1, A2, B1, B2]] =
      mkPairConverter(conv1, conv2)

    def unapply[A1, A2, B1, B2](p: Rep[Converter[(A1, A2), (B1, B2)]]) = unmkPairConverter(p)
  }
  lazy val PairConverterRep: Rep[PairConverterCompanionAbs] = new PairConverterCompanionAbs
  lazy val PairConverter: PairConverterCompanionAbs = proxyPairConverterCompanion(PairConverterRep)
  implicit def proxyPairConverterCompanion(p: Rep[PairConverterCompanionAbs]): PairConverterCompanionAbs = {
    proxyOps[PairConverterCompanionAbs](p)
  }

  implicit case object PairConverterCompanionElem extends CompanionElem[PairConverterCompanionAbs] {
    lazy val tag = weakTypeTag[PairConverterCompanionAbs]
    protected def getDefaultRep = PairConverter
  }

  implicit def proxyPairConverter[A1, A2, B1, B2](p: Rep[PairConverter[A1, A2, B1, B2]]): PairConverter[A1, A2, B1, B2] =
    proxyOps[PairConverter[A1, A2, B1, B2]](p)

  implicit class ExtendedPairConverter[A1, A2, B1, B2](p: Rep[PairConverter[A1, A2, B1, B2]])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]) {
    def toData: Rep[PairConverterData[A1, A2, B1, B2]] = isoPairConverter(eA1, eA2, eB1, eB2).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoPairConverter[A1, A2, B1, B2](implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Iso[PairConverterData[A1, A2, B1, B2], PairConverter[A1, A2, B1, B2]] =
    reifyObject(new PairConverterIso[A1, A2, B1, B2]()(eA1, eA2, eB1, eB2))

  // 6) smart constructor and deconstructor
  def mkPairConverter[A1, A2, B1, B2](conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[PairConverter[A1, A2, B1, B2]]
  def unmkPairConverter[A1, A2, B1, B2](p: Rep[Converter[(A1, A2), (B1, B2)]]): Option[(Rep[Converter[A1, B1]], Rep[Converter[A2, B2]])]

  abstract class AbsSumConverter[A1, A2, B1, B2]
      (conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends SumConverter[A1, A2, B1, B2](conv1, conv2) with Def[SumConverter[A1, A2, B1, B2]] {
    lazy val selfType = element[SumConverter[A1, A2, B1, B2]]
  }
  // elem for concrete class
  class SumConverterElem[A1, A2, B1, B2](val iso: Iso[SumConverterData[A1, A2, B1, B2], SumConverter[A1, A2, B1, B2]])(implicit val eA1: Elem[A1], val eA2: Elem[A2], val eB1: Elem[B1], val eB2: Elem[B2])
    extends ConverterElem[$bar[A1, A2], $bar[B1, B2], SumConverter[A1, A2, B1, B2]]
    with ConcreteElem[SumConverterData[A1, A2, B1, B2], SumConverter[A1, A2, B1, B2]] {
    override lazy val parent: Option[Elem[_]] = Some(converterElement(sumElement(element[A1],element[A2]), sumElement(element[B1],element[B2])))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("A1" -> Left(eA1), "A2" -> Left(eA2), "B1" -> Left(eB1), "B2" -> Left(eB2))
    }

    override def convertConverter(x: Rep[Converter[$bar[A1, A2], $bar[B1, B2]]]) = // Converter is not generated by meta
!!!("Cannot convert from Converter to SumConverter: missing fields List(conv1, conv2)")
    override def getDefaultRep = SumConverter(element[Converter[A1, B1]].defaultRepValue, element[Converter[A2, B2]].defaultRepValue)
    override lazy val tag = {
      implicit val tagA1 = eA1.tag
      implicit val tagA2 = eA2.tag
      implicit val tagB1 = eB1.tag
      implicit val tagB2 = eB2.tag
      weakTypeTag[SumConverter[A1, A2, B1, B2]]
    }
  }

  // state representation type
  type SumConverterData[A1, A2, B1, B2] = (Converter[A1, B1], Converter[A2, B2])

  // 3) Iso for concrete class
  class SumConverterIso[A1, A2, B1, B2](implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends EntityIso[SumConverterData[A1, A2, B1, B2], SumConverter[A1, A2, B1, B2]] with Def[SumConverterIso[A1, A2, B1, B2]] {
    override def from(p: Rep[SumConverter[A1, A2, B1, B2]]) =
      (p.conv1, p.conv2)
    override def to(p: Rep[(Converter[A1, B1], Converter[A2, B2])]) = {
      val Pair(conv1, conv2) = p
      SumConverter(conv1, conv2)
    }
    lazy val eFrom = pairElement(element[Converter[A1, B1]], element[Converter[A2, B2]])
    lazy val eTo = new SumConverterElem[A1, A2, B1, B2](self)
    lazy val selfType = new SumConverterIsoElem[A1, A2, B1, B2](eA1, eA2, eB1, eB2)
    def productArity = 4
    def productElement(n: Int) = (eA1, eA2, eB1, eB2).productElement(n)
  }
  case class SumConverterIsoElem[A1, A2, B1, B2](eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]) extends Elem[SumConverterIso[A1, A2, B1, B2]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new SumConverterIso[A1, A2, B1, B2]()(eA1, eA2, eB1, eB2))
    lazy val tag = {
      implicit val tagA1 = eA1.tag
      implicit val tagA2 = eA2.tag
      implicit val tagB1 = eB1.tag
      implicit val tagB2 = eB2.tag
      weakTypeTag[SumConverterIso[A1, A2, B1, B2]]
    }
  }
  // 4) constructor and deconstructor
  class SumConverterCompanionAbs extends CompanionDef[SumConverterCompanionAbs] with SumConverterCompanion {
    def selfType = SumConverterCompanionElem
    override def toString = "SumConverter"
    @scalan.OverloadId("fromData")
    def apply[A1, A2, B1, B2](p: Rep[SumConverterData[A1, A2, B1, B2]])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[SumConverter[A1, A2, B1, B2]] =
      isoSumConverter(eA1, eA2, eB1, eB2).to(p)
    @scalan.OverloadId("fromFields")
    def apply[A1, A2, B1, B2](conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[SumConverter[A1, A2, B1, B2]] =
      mkSumConverter(conv1, conv2)

    def unapply[A1, A2, B1, B2](p: Rep[Converter[$bar[A1, A2], $bar[B1, B2]]]) = unmkSumConverter(p)
  }
  lazy val SumConverterRep: Rep[SumConverterCompanionAbs] = new SumConverterCompanionAbs
  lazy val SumConverter: SumConverterCompanionAbs = proxySumConverterCompanion(SumConverterRep)
  implicit def proxySumConverterCompanion(p: Rep[SumConverterCompanionAbs]): SumConverterCompanionAbs = {
    proxyOps[SumConverterCompanionAbs](p)
  }

  implicit case object SumConverterCompanionElem extends CompanionElem[SumConverterCompanionAbs] {
    lazy val tag = weakTypeTag[SumConverterCompanionAbs]
    protected def getDefaultRep = SumConverter
  }

  implicit def proxySumConverter[A1, A2, B1, B2](p: Rep[SumConverter[A1, A2, B1, B2]]): SumConverter[A1, A2, B1, B2] =
    proxyOps[SumConverter[A1, A2, B1, B2]](p)

  implicit class ExtendedSumConverter[A1, A2, B1, B2](p: Rep[SumConverter[A1, A2, B1, B2]])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]) {
    def toData: Rep[SumConverterData[A1, A2, B1, B2]] = isoSumConverter(eA1, eA2, eB1, eB2).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoSumConverter[A1, A2, B1, B2](implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Iso[SumConverterData[A1, A2, B1, B2], SumConverter[A1, A2, B1, B2]] =
    reifyObject(new SumConverterIso[A1, A2, B1, B2]()(eA1, eA2, eB1, eB2))

  // 6) smart constructor and deconstructor
  def mkSumConverter[A1, A2, B1, B2](conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[SumConverter[A1, A2, B1, B2]]
  def unmkSumConverter[A1, A2, B1, B2](p: Rep[Converter[$bar[A1, A2], $bar[B1, B2]]]): Option[(Rep[Converter[A1, B1]], Rep[Converter[A2, B2]])]

  abstract class AbsFunctorConverter[A, B, F[_]]
      (itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F])
    extends FunctorConverter[A, B, F](itemConv) with Def[FunctorConverter[A, B, F]] {
    lazy val selfType = element[FunctorConverter[A, B, F]]
  }
  // elem for concrete class
  class FunctorConverterElem[A, B, F[_]](val iso: Iso[FunctorConverterData[A, B, F], FunctorConverter[A, B, F]])(implicit val eA: Elem[A], val eB: Elem[B], val F: Functor[F])
    extends ConverterElem[F[A], F[B], FunctorConverter[A, B, F]]
    with ConcreteElem[FunctorConverterData[A, B, F], FunctorConverter[A, B, F]] {
    override lazy val parent: Option[Elem[_]] = Some(converterElement(element[F[A]], element[F[B]]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("A" -> Left(eA), "B" -> Left(eB))
    }

    override def convertConverter(x: Rep[Converter[F[A], F[B]]]) = // Converter is not generated by meta
!!!("Cannot convert from Converter to FunctorConverter: missing fields List(itemConv)")
    override def getDefaultRep = FunctorConverter(element[Converter[A, B]].defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      implicit val tagB = eB.tag
      weakTypeTag[FunctorConverter[A, B, F]]
    }
  }

  // state representation type
  type FunctorConverterData[A, B, F[_]] = Converter[A, B]

  // 3) Iso for concrete class
  class FunctorConverterIso[A, B, F[_]](implicit eA: Elem[A], eB: Elem[B], F: Functor[F])
    extends EntityIso[FunctorConverterData[A, B, F], FunctorConverter[A, B, F]] with Def[FunctorConverterIso[A, B, F]] {
    override def from(p: Rep[FunctorConverter[A, B, F]]) =
      p.itemConv
    override def to(p: Rep[Converter[A, B]]) = {
      val itemConv = p
      FunctorConverter(itemConv)
    }
    lazy val eFrom = element[Converter[A, B]]
    lazy val eTo = new FunctorConverterElem[A, B, F](self)
    lazy val selfType = new FunctorConverterIsoElem[A, B, F](eA, eB, F)
    def productArity = 3
    def productElement(n: Int) = (eA, eB, F).productElement(n)
  }
  case class FunctorConverterIsoElem[A, B, F[_]](eA: Elem[A], eB: Elem[B], F: Functor[F]) extends Elem[FunctorConverterIso[A, B, F]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new FunctorConverterIso[A, B, F]()(eA, eB, F))
    lazy val tag = {
      implicit val tagA = eA.tag
      implicit val tagB = eB.tag
      weakTypeTag[FunctorConverterIso[A, B, F]]
    }
  }
  // 4) constructor and deconstructor
  class FunctorConverterCompanionAbs extends CompanionDef[FunctorConverterCompanionAbs] with FunctorConverterCompanion {
    def selfType = FunctorConverterCompanionElem
    override def toString = "FunctorConverter"

    @scalan.OverloadId("fromFields")
    def apply[A, B, F[_]](itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F]): Rep[FunctorConverter[A, B, F]] =
      mkFunctorConverter(itemConv)

    def unapply[A, B, F[_]](p: Rep[Converter[F[A], F[B]]]) = unmkFunctorConverter(p)
  }
  lazy val FunctorConverterRep: Rep[FunctorConverterCompanionAbs] = new FunctorConverterCompanionAbs
  lazy val FunctorConverter: FunctorConverterCompanionAbs = proxyFunctorConverterCompanion(FunctorConverterRep)
  implicit def proxyFunctorConverterCompanion(p: Rep[FunctorConverterCompanionAbs]): FunctorConverterCompanionAbs = {
    proxyOps[FunctorConverterCompanionAbs](p)
  }

  implicit case object FunctorConverterCompanionElem extends CompanionElem[FunctorConverterCompanionAbs] {
    lazy val tag = weakTypeTag[FunctorConverterCompanionAbs]
    protected def getDefaultRep = FunctorConverter
  }

  implicit def proxyFunctorConverter[A, B, F[_]](p: Rep[FunctorConverter[A, B, F]]): FunctorConverter[A, B, F] =
    proxyOps[FunctorConverter[A, B, F]](p)

  implicit class ExtendedFunctorConverter[A, B, F[_]](p: Rep[FunctorConverter[A, B, F]])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F]) {
    def toData: Rep[FunctorConverterData[A, B, F]] = isoFunctorConverter(eA, eB, F).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoFunctorConverter[A, B, F[_]](implicit eA: Elem[A], eB: Elem[B], F: Functor[F]): Iso[FunctorConverterData[A, B, F], FunctorConverter[A, B, F]] =
    reifyObject(new FunctorConverterIso[A, B, F]()(eA, eB, F))

  // 6) smart constructor and deconstructor
  def mkFunctorConverter[A, B, F[_]](itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F]): Rep[FunctorConverter[A, B, F]]
  def unmkFunctorConverter[A, B, F[_]](p: Rep[Converter[F[A], F[B]]]): Option[(Rep[Converter[A, B]])]

  abstract class AbsNaturalConverter[A, F[_], G[_]]
      (convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G])
    extends NaturalConverter[A, F, G](convFun) with Def[NaturalConverter[A, F, G]] {
    lazy val selfType = element[NaturalConverter[A, F, G]]
  }
  // elem for concrete class
  class NaturalConverterElem[A, F[_], G[_]](val iso: Iso[NaturalConverterData[A, F, G], NaturalConverter[A, F, G]])(implicit val eA: Elem[A], val cF: Cont[F], val cG: Cont[G])
    extends ConverterElem[F[A], G[A], NaturalConverter[A, F, G]]
    with ConcreteElem[NaturalConverterData[A, F, G], NaturalConverter[A, F, G]] {
    override lazy val parent: Option[Elem[_]] = Some(converterElement(element[F[A]], element[G[A]]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("A" -> Left(eA), "F" -> Right(cF.asInstanceOf[SomeCont]), "G" -> Right(cG.asInstanceOf[SomeCont]))
    }

    override def convertConverter(x: Rep[Converter[F[A], G[A]]]) = NaturalConverter(x.convFun)
    override def getDefaultRep = NaturalConverter(constFun[F[A], G[A]](element[G[A]].defaultRepValue))
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[NaturalConverter[A, F, G]]
    }
  }

  // state representation type
  type NaturalConverterData[A, F[_], G[_]] = F[A] => G[A]

  // 3) Iso for concrete class
  class NaturalConverterIso[A, F[_], G[_]](implicit eA: Elem[A], cF: Cont[F], cG: Cont[G])
    extends EntityIso[NaturalConverterData[A, F, G], NaturalConverter[A, F, G]] with Def[NaturalConverterIso[A, F, G]] {
    override def from(p: Rep[NaturalConverter[A, F, G]]) =
      p.convFun
    override def to(p: Rep[F[A] => G[A]]) = {
      val convFun = p
      NaturalConverter(convFun)
    }
    lazy val eFrom = element[F[A] => G[A]]
    lazy val eTo = new NaturalConverterElem[A, F, G](self)
    lazy val selfType = new NaturalConverterIsoElem[A, F, G](eA, cF, cG)
    def productArity = 3
    def productElement(n: Int) = (eA, cF, cG).productElement(n)
  }
  case class NaturalConverterIsoElem[A, F[_], G[_]](eA: Elem[A], cF: Cont[F], cG: Cont[G]) extends Elem[NaturalConverterIso[A, F, G]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new NaturalConverterIso[A, F, G]()(eA, cF, cG))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[NaturalConverterIso[A, F, G]]
    }
  }
  // 4) constructor and deconstructor
  class NaturalConverterCompanionAbs extends CompanionDef[NaturalConverterCompanionAbs] {
    def selfType = NaturalConverterCompanionElem
    override def toString = "NaturalConverter"

    @scalan.OverloadId("fromFields")
    def apply[A, F[_], G[_]](convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G]): Rep[NaturalConverter[A, F, G]] =
      mkNaturalConverter(convFun)

    def unapply[A, F[_], G[_]](p: Rep[Converter[F[A], G[A]]]) = unmkNaturalConverter(p)
  }
  lazy val NaturalConverterRep: Rep[NaturalConverterCompanionAbs] = new NaturalConverterCompanionAbs
  lazy val NaturalConverter: NaturalConverterCompanionAbs = proxyNaturalConverterCompanion(NaturalConverterRep)
  implicit def proxyNaturalConverterCompanion(p: Rep[NaturalConverterCompanionAbs]): NaturalConverterCompanionAbs = {
    proxyOps[NaturalConverterCompanionAbs](p)
  }

  implicit case object NaturalConverterCompanionElem extends CompanionElem[NaturalConverterCompanionAbs] {
    lazy val tag = weakTypeTag[NaturalConverterCompanionAbs]
    protected def getDefaultRep = NaturalConverter
  }

  implicit def proxyNaturalConverter[A, F[_], G[_]](p: Rep[NaturalConverter[A, F, G]]): NaturalConverter[A, F, G] =
    proxyOps[NaturalConverter[A, F, G]](p)

  implicit class ExtendedNaturalConverter[A, F[_], G[_]](p: Rep[NaturalConverter[A, F, G]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G]) {
    def toData: Rep[NaturalConverterData[A, F, G]] = isoNaturalConverter(eA, cF, cG).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoNaturalConverter[A, F[_], G[_]](implicit eA: Elem[A], cF: Cont[F], cG: Cont[G]): Iso[NaturalConverterData[A, F, G], NaturalConverter[A, F, G]] =
    reifyObject(new NaturalConverterIso[A, F, G]()(eA, cF, cG))

  // 6) smart constructor and deconstructor
  def mkNaturalConverter[A, F[_], G[_]](convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G]): Rep[NaturalConverter[A, F, G]]
  def unmkNaturalConverter[A, F[_], G[_]](p: Rep[Converter[F[A], G[A]]]): Option[(Rep[F[A] => G[A]])]

  registerModule(Converters_Module)
}

// Std -----------------------------------
trait ConvertersStd extends ConvertersDsl {
  self: ScalanStd =>
  lazy val Converter: Rep[ConverterCompanionAbs] = new ConverterCompanionAbs {
  }

  case class StdBaseConverter[T, R]
      (override val convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R])
    extends AbsBaseConverter[T, R](convFun) {
  }

  def mkBaseConverter[T, R]
    (convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R]): Rep[BaseConverter[T, R]] =
    new StdBaseConverter[T, R](convFun)
  def unmkBaseConverter[T, R](p: Rep[Converter[T, R]]) = p match {
    case p: BaseConverter[T, R] @unchecked =>
      Some((p.convFun))
    case _ => None
  }

  case class StdPairConverter[A1, A2, B1, B2]
      (override val conv1: Conv[A1, B1], override val conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends AbsPairConverter[A1, A2, B1, B2](conv1, conv2) {
  }

  def mkPairConverter[A1, A2, B1, B2]
    (conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[PairConverter[A1, A2, B1, B2]] =
    new StdPairConverter[A1, A2, B1, B2](conv1, conv2)
  def unmkPairConverter[A1, A2, B1, B2](p: Rep[Converter[(A1, A2), (B1, B2)]]) = p match {
    case p: PairConverter[A1, A2, B1, B2] @unchecked =>
      Some((p.conv1, p.conv2))
    case _ => None
  }

  case class StdSumConverter[A1, A2, B1, B2]
      (override val conv1: Conv[A1, B1], override val conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends AbsSumConverter[A1, A2, B1, B2](conv1, conv2) {
  }

  def mkSumConverter[A1, A2, B1, B2]
    (conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[SumConverter[A1, A2, B1, B2]] =
    new StdSumConverter[A1, A2, B1, B2](conv1, conv2)
  def unmkSumConverter[A1, A2, B1, B2](p: Rep[Converter[$bar[A1, A2], $bar[B1, B2]]]) = p match {
    case p: SumConverter[A1, A2, B1, B2] @unchecked =>
      Some((p.conv1, p.conv2))
    case _ => None
  }

  case class StdFunctorConverter[A, B, F[_]]
      (override val itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F])
    extends AbsFunctorConverter[A, B, F](itemConv) {
  }

  def mkFunctorConverter[A, B, F[_]]
    (itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F]): Rep[FunctorConverter[A, B, F]] =
    new StdFunctorConverter[A, B, F](itemConv)
  def unmkFunctorConverter[A, B, F[_]](p: Rep[Converter[F[A], F[B]]]) = p match {
    case p: FunctorConverter[A, B, F] @unchecked =>
      Some((p.itemConv))
    case _ => None
  }

  case class StdNaturalConverter[A, F[_], G[_]]
      (override val convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G])
    extends AbsNaturalConverter[A, F, G](convFun) {
  }

  def mkNaturalConverter[A, F[_], G[_]]
    (convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G]): Rep[NaturalConverter[A, F, G]] =
    new StdNaturalConverter[A, F, G](convFun)
  def unmkNaturalConverter[A, F[_], G[_]](p: Rep[Converter[F[A], G[A]]]) = p match {
    case p: NaturalConverter[A, F, G] @unchecked =>
      Some((p.convFun))
    case _ => None
  }
}

// Exp -----------------------------------
trait ConvertersExp extends ConvertersDsl {
  self: ScalanExp =>
  lazy val Converter: Rep[ConverterCompanionAbs] = new ConverterCompanionAbs {
  }

  case class ExpBaseConverter[T, R]
      (override val convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R])
    extends AbsBaseConverter[T, R](convFun)

  object BaseConverterMethods {
    object apply {
      def unapply(d: Def[_]): Option[(Rep[BaseConverter[T, R]], Rep[T]) forSome {type T; type R}] = d match {
        case MethodCall(receiver, method, Seq(x, _*), _) if receiver.elem.isInstanceOf[BaseConverterElem[_, _]] && method.getName == "apply" =>
          Some((receiver, x)).asInstanceOf[Option[(Rep[BaseConverter[T, R]], Rep[T]) forSome {type T; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[BaseConverter[T, R]], Rep[T]) forSome {type T; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `equals`: Overrides Object method
  }

  object BaseConverterCompanionMethods {
  }

  def mkBaseConverter[T, R]
    (convFun: Rep[T => R])(implicit eT: Elem[T], eR: Elem[R]): Rep[BaseConverter[T, R]] =
    new ExpBaseConverter[T, R](convFun)
  def unmkBaseConverter[T, R](p: Rep[Converter[T, R]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: BaseConverterElem[T, R] @unchecked =>
      Some((p.asRep[BaseConverter[T, R]].convFun))
    case _ =>
      None
  }

  case class ExpPairConverter[A1, A2, B1, B2]
      (override val conv1: Conv[A1, B1], override val conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends AbsPairConverter[A1, A2, B1, B2](conv1, conv2)

  object PairConverterMethods {
    object apply {
      def unapply(d: Def[_]): Option[(Rep[PairConverter[A1, A2, B1, B2]], Rep[(A1, A2)]) forSome {type A1; type A2; type B1; type B2}] = d match {
        case MethodCall(receiver, method, Seq(x, _*), _) if receiver.elem.isInstanceOf[PairConverterElem[_, _, _, _]] && method.getName == "apply" =>
          Some((receiver, x)).asInstanceOf[Option[(Rep[PairConverter[A1, A2, B1, B2]], Rep[(A1, A2)]) forSome {type A1; type A2; type B1; type B2}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[PairConverter[A1, A2, B1, B2]], Rep[(A1, A2)]) forSome {type A1; type A2; type B1; type B2}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object PairConverterCompanionMethods {
  }

  def mkPairConverter[A1, A2, B1, B2]
    (conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[PairConverter[A1, A2, B1, B2]] =
    new ExpPairConverter[A1, A2, B1, B2](conv1, conv2)
  def unmkPairConverter[A1, A2, B1, B2](p: Rep[Converter[(A1, A2), (B1, B2)]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: PairConverterElem[A1, A2, B1, B2] @unchecked =>
      Some((p.asRep[PairConverter[A1, A2, B1, B2]].conv1, p.asRep[PairConverter[A1, A2, B1, B2]].conv2))
    case _ =>
      None
  }

  case class ExpSumConverter[A1, A2, B1, B2]
      (override val conv1: Conv[A1, B1], override val conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2])
    extends AbsSumConverter[A1, A2, B1, B2](conv1, conv2)

  object SumConverterMethods {
    object apply {
      def unapply(d: Def[_]): Option[(Rep[SumConverter[A1, A2, B1, B2]], Rep[$bar[A1, A2]]) forSome {type A1; type A2; type B1; type B2}] = d match {
        case MethodCall(receiver, method, Seq(x, _*), _) if receiver.elem.isInstanceOf[SumConverterElem[_, _, _, _]] && method.getName == "apply" =>
          Some((receiver, x)).asInstanceOf[Option[(Rep[SumConverter[A1, A2, B1, B2]], Rep[$bar[A1, A2]]) forSome {type A1; type A2; type B1; type B2}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[SumConverter[A1, A2, B1, B2]], Rep[$bar[A1, A2]]) forSome {type A1; type A2; type B1; type B2}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object SumConverterCompanionMethods {
  }

  def mkSumConverter[A1, A2, B1, B2]
    (conv1: Conv[A1, B1], conv2: Conv[A2, B2])(implicit eA1: Elem[A1], eA2: Elem[A2], eB1: Elem[B1], eB2: Elem[B2]): Rep[SumConverter[A1, A2, B1, B2]] =
    new ExpSumConverter[A1, A2, B1, B2](conv1, conv2)
  def unmkSumConverter[A1, A2, B1, B2](p: Rep[Converter[$bar[A1, A2], $bar[B1, B2]]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: SumConverterElem[A1, A2, B1, B2] @unchecked =>
      Some((p.asRep[SumConverter[A1, A2, B1, B2]].conv1, p.asRep[SumConverter[A1, A2, B1, B2]].conv2))
    case _ =>
      None
  }

  case class ExpFunctorConverter[A, B, F[_]]
      (override val itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F])
    extends AbsFunctorConverter[A, B, F](itemConv)

  object FunctorConverterMethods {
    object convFun {
      def unapply(d: Def[_]): Option[Rep[FunctorConverter[A, B, F]] forSome {type A; type B; type F[_]}] = d match {
        case MethodCall(receiver, method, _, _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FunctorConverterElem[_, _, _] => true; case _ => false }) && method.getName == "convFun" =>
          Some(receiver).asInstanceOf[Option[Rep[FunctorConverter[A, B, F]] forSome {type A; type B; type F[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[FunctorConverter[A, B, F]] forSome {type A; type B; type F[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object apply {
      def unapply(d: Def[_]): Option[(Rep[FunctorConverter[A, B, F]], Rep[F[A]]) forSome {type A; type B; type F[_]}] = d match {
        case MethodCall(receiver, method, Seq(xs, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FunctorConverterElem[_, _, _] => true; case _ => false }) && method.getName == "apply" =>
          Some((receiver, xs)).asInstanceOf[Option[(Rep[FunctorConverter[A, B, F]], Rep[F[A]]) forSome {type A; type B; type F[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FunctorConverter[A, B, F]], Rep[F[A]]) forSome {type A; type B; type F[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `equals`: Overrides Object method
  }

  object FunctorConverterCompanionMethods {
  }

  def mkFunctorConverter[A, B, F[_]]
    (itemConv: Conv[A, B])(implicit eA: Elem[A], eB: Elem[B], F: Functor[F]): Rep[FunctorConverter[A, B, F]] =
    new ExpFunctorConverter[A, B, F](itemConv)
  def unmkFunctorConverter[A, B, F[_]](p: Rep[Converter[F[A], F[B]]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: FunctorConverterElem[A, B, F] @unchecked =>
      Some((p.asRep[FunctorConverter[A, B, F]].itemConv))
    case _ =>
      None
  }

  case class ExpNaturalConverter[A, F[_], G[_]]
      (override val convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G])
    extends AbsNaturalConverter[A, F, G](convFun)

  object NaturalConverterMethods {
    object apply {
      def unapply(d: Def[_]): Option[(Rep[NaturalConverter[A, F, G]], Rep[F[A]]) forSome {type A; type F[_]; type G[_]}] = d match {
        case MethodCall(receiver, method, Seq(xs, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: NaturalConverterElem[_, _, _] => true; case _ => false }) && method.getName == "apply" =>
          Some((receiver, xs)).asInstanceOf[Option[(Rep[NaturalConverter[A, F, G]], Rep[F[A]]) forSome {type A; type F[_]; type G[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[NaturalConverter[A, F, G]], Rep[F[A]]) forSome {type A; type F[_]; type G[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `equals`: Overrides Object method
  }

  def mkNaturalConverter[A, F[_], G[_]]
    (convFun: Rep[F[A] => G[A]])(implicit eA: Elem[A], cF: Cont[F], cG: Cont[G]): Rep[NaturalConverter[A, F, G]] =
    new ExpNaturalConverter[A, F, G](convFun)
  def unmkNaturalConverter[A, F[_], G[_]](p: Rep[Converter[F[A], G[A]]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: NaturalConverterElem[A, F, G] @unchecked =>
      Some((p.asRep[NaturalConverter[A, F, G]].convFun))
    case _ =>
      None
  }

  object ConverterMethods {
    object convFun {
      def unapply(d: Def[_]): Option[Rep[Converter[T, R]] forSome {type T; type R}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[ConverterElem[_, _, _]] && method.getName == "convFun" =>
          Some(receiver).asInstanceOf[Option[Rep[Converter[T, R]] forSome {type T; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Converter[T, R]] forSome {type T; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object apply {
      def unapply(d: Def[_]): Option[(Rep[Converter[T, R]], Rep[T]) forSome {type T; type R}] = d match {
        case MethodCall(receiver, method, Seq(x, _*), _) if receiver.elem.isInstanceOf[ConverterElem[_, _, _]] && method.getName == "apply" =>
          Some((receiver, x)).asInstanceOf[Option[(Rep[Converter[T, R]], Rep[T]) forSome {type T; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Converter[T, R]], Rep[T]) forSome {type T; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `toString`: Overrides Object method
  }

  object ConverterCompanionMethods {
  }
}

object Converters_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAAO1YS2wbRRietePYjkMTWlpUojZtZB6t2rg1SD1EqLJTJ6RyHvLmgEzVaLyepFv2xe44sjkUThGCG+LCgUMFCCFVSIgjEieQKoQ4oAohwYUDp1KEcmjFAcQ/sw977d213ZALqg+jnfU//+P7v2+0M7fuoYRlomcsCStYm1UJxbMify5YNCuWNCrT1rJebyjkEtnM3J87+cvO/FIMTVTR6DVsXbKUKkrbD6Wm4T2LtF5GaaxJxKK6aVF0sswj5CRdUYhEZV3LyaraoLimkFxZtuhcGY3U9HrrNXQDCWU0KemaZBJKxHkFWxaxnPcpwjKSvXmaz1urRjuGlmNV5DqqWDexTCF9iDFp21eIIbY0XWupFB1wUls1WFpgk5RVQzepGyIJ7q7pdXc6omF4gQ6Wr+NtnIMQWzmRmrK2BSszBpZexVtkBUyY+QgkbBFlc71l8HkcTCxaB4CWVEPhHuNNAyEELcjzLGbbAM16AM0ygLIiMWWsyK9j9ueaqTdbyP4JcYSaBrg408eF64GUtHr27SvSKw/EjBpji5sslSRPaBQcTYfQgfcCgLxdedfaXbx5IYbGqmhMtgo1i5pYop09d+DKYE3TKc/ZQxCbW9CumbB28SgFsOniRFrSVQNr4MnBchwapciSTJkxezfutCcE+yQ1iGsqNA3Bq/dESL2cOPNYUdbuHj379O+ll2Mo5g+RBpciMN90nVKUnte1bWJSYjr+2ThBkbDeBplNK3zKhnSzPSYj0vGAefbuH/VvzqErMQ9OJ/pgHQQXCeunHzN3Tl2MoVSVE35BwVtVQNQqKURdNaEGWkUpHQqx/0luY4U9BXY0WSebuKFQB+dOgOIAEEUnQqVpEIbeHFeB4AKQsYm8omsku7CWvS9++94txlMTjdv/2Fr9R77w988HNimnMEVJ2DO2Fxqai3AcVO4BcjysxQaBJdKdpfcPTRzb+JU3eLSuq1jmLJsqo4QJGuflTDkAD9XMMTtjUVfJ4zO78tWb71DeNqHp30RWa9dBtHN83fGIDrqb2ec7O4f//GjjENdgqiZTFRvZc0Mo0BXMPioMeajYSD3ZnrNhGpp2pIgt4ilmvjP+dMfCDik9JbhM4UYUxci624cRxt5A2dlNCXNQiXLQ21uKHvOlzf14VDsWTjXA5kil/IRy7+JXMZS4jBKbICkLOFbTG1rdBR14TEmTFt13gh90ABmbWPVA5r8TqA2Zn57zgQaVblQygr/snm0sgpIGWW8YCnnhy7+uvvXmSwbnd8/O6HcfK5z3KShWyHdntNy1oti1oti5old6PbxBXW1PsP3ivNd5Vm+fHJ0MOrkU6DQf6TQ/UBm2wWk+nh1ESGtYNvcmpDgptOHoFYILR5SUwEU+0kVPlwNcFCOzCGhCgIvILAIgB0n7AOyUdKiKfAQJscj3syj29VHsAa2vWN3iszVsBncxoiUhCx/pL0p/h8WG+kh+Dy+/8U78/h/qExa65oUApfUYFfemqpRMierRPlgDTiJ9Qg6rgKPsQxo+PPf6NVeI4E4vhAEOilEOin0dOB0B46RTUXdbA+g72V38QBTubESwQbGfwULfGBs96e4DixcjjB6CxRFnOTZm9zG5duP2maeSxzMmURpBsjAHi1EOFoNYuoJpw8TKf83SPiSEQTjTXU+wp8XhPPWwetTZrvxgxOHAPCDbBzrY+3jsWQy7YR6M2il9N0mDtWmoQ583CqYPXmY41gaHQkR+0nNqM9FMyClQdK4CAOobDz5YOf39F7/x25Qxdqmga0TzblTbh9nuU+Go7a8jTeA2u2Pwop8Ki07r7E6VqBDHvuvYvfxpeuqz2of8NJpi165EUizvLir4KnoZG3P8LvW5iLtUMMqWVIO22MPzX7/4wxvfffIxv4T6FyoqTr1PFwAA"
}
}

