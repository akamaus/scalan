package scalan.effects
package impl

import scalan._
import scala.reflect.runtime.universe._
import scalan.monads.{MonadsDslExp, MonadsDslSeq, MonadsDsl}
import scala.reflect.runtime.universe._
import scala.reflect._
import scalan.common.Default

// Abs -----------------------------------
trait FreeStatesAbs extends FreeStates with Scalan {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyStateF[S, A](p: Rep[StateF[S, A]]): StateF[S, A] = {
    proxyOps[StateF[S, A]](p)(classTag[StateF[S, A]])
  }

  // familyElem
  class StateFElem[S, A, To <: StateF[S, A]](implicit val eS: Elem[S], val eA: Elem[A])
    extends EntityElem[To] {
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateF[S, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Reifiable[_]]) = {
      val conv = fun {x: Rep[StateF[S, A]] =>  convertStateF(x) }
      tryConvert(element[StateF[S, A]], this, x, conv)
    }

    def convertStateF(x : Rep[StateF[S, A]]): Rep[To] = {
      assert(x.selfType1 match { case _: StateFElem[_, _, _] => true; case _ => false })
      x.asRep[To]
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def stateFElement[S, A](implicit eS: Elem[S], eA: Elem[A]): Elem[StateF[S, A]] =
    new StateFElem[S, A, StateF[S, A]]

  implicit object StateFCompanionElem extends CompanionElem[StateFCompanionAbs] {
    lazy val tag = weakTypeTag[StateFCompanionAbs]
    protected def getDefaultRep = StateF
  }

  abstract class StateFCompanionAbs extends CompanionBase[StateFCompanionAbs] with StateFCompanion {
    override def toString = "StateF"
  }
  def StateF: Rep[StateFCompanionAbs]
  implicit def proxyStateFCompanion(p: Rep[StateFCompanion]): StateFCompanion = {
    proxyOps[StateFCompanion](p)
  }

  // elem for concrete class
  class StateGetElem[S, A](val iso: Iso[StateGetData[S, A], StateGet[S, A]])(implicit eS: Elem[S], eA: Elem[A])
    extends StateFElem[S, A, StateGet[S, A]]
    with ConcreteElem[StateGetData[S, A], StateGet[S, A]] {
    override def convertStateF(x: Rep[StateF[S, A]]) = // Converter is not generated by meta
!!!("Cannot convert from StateF to StateGet: missing fields List(f)")
    override def getDefaultRep = super[ConcreteElem].getDefaultRep
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateGet[S, A]]
    }
  }

  // state representation type
  type StateGetData[S, A] = S => A

  // 3) Iso for concrete class
  class StateGetIso[S, A](implicit eS: Elem[S], eA: Elem[A])
    extends Iso[StateGetData[S, A], StateGet[S, A]] {
    override def from(p: Rep[StateGet[S, A]]) =
      p.f
    override def to(p: Rep[S => A]) = {
      val f = p
      StateGet(f)
    }
    lazy val defaultRepTo = Default.defaultVal[Rep[StateGet[S, A]]](StateGet(fun { (x: Rep[S]) => element[A].defaultRepValue }))
    lazy val eTo = new StateGetElem[S, A](this)
  }
  // 4) constructor and deconstructor
  abstract class StateGetCompanionAbs extends CompanionBase[StateGetCompanionAbs] with StateGetCompanion {
    override def toString = "StateGet"

    def apply[S, A](f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
      mkStateGet(f)
  }
  object StateGetMatcher {
    def unapply[S, A](p: Rep[StateF[S, A]]) = unmkStateGet(p)
  }
  def StateGet: Rep[StateGetCompanionAbs]
  implicit def proxyStateGetCompanion(p: Rep[StateGetCompanionAbs]): StateGetCompanionAbs = {
    proxyOps[StateGetCompanionAbs](p)
  }

  implicit object StateGetCompanionElem extends CompanionElem[StateGetCompanionAbs] {
    lazy val tag = weakTypeTag[StateGetCompanionAbs]
    protected def getDefaultRep = StateGet
  }

  implicit def proxyStateGet[S, A](p: Rep[StateGet[S, A]]): StateGet[S, A] =
    proxyOps[StateGet[S, A]](p)

  implicit class ExtendedStateGet[S, A](p: Rep[StateGet[S, A]])(implicit eS: Elem[S], eA: Elem[A]) {
    def toData: Rep[StateGetData[S, A]] = isoStateGet(eS, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoStateGet[S, A](implicit eS: Elem[S], eA: Elem[A]): Iso[StateGetData[S, A], StateGet[S, A]] =
    new StateGetIso[S, A]

  // 6) smart constructor and deconstructor
  def mkStateGet[S, A](f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]]
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]): Option[(Rep[S => A])]

  // elem for concrete class
  class StatePutElem[S, A](val iso: Iso[StatePutData[S, A], StatePut[S, A]])(implicit eS: Elem[S], eA: Elem[A])
    extends StateFElem[S, A, StatePut[S, A]]
    with ConcreteElem[StatePutData[S, A], StatePut[S, A]] {
    override def convertStateF(x: Rep[StateF[S, A]]) = // Converter is not generated by meta
!!!("Cannot convert from StateF to StatePut: missing fields List(s, a)")
    override def getDefaultRep = super[ConcreteElem].getDefaultRep
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StatePut[S, A]]
    }
  }

  // state representation type
  type StatePutData[S, A] = (S, A)

  // 3) Iso for concrete class
  class StatePutIso[S, A](implicit eS: Elem[S], eA: Elem[A])
    extends Iso[StatePutData[S, A], StatePut[S, A]]()(pairElement(implicitly[Elem[S]], implicitly[Elem[A]])) {
    override def from(p: Rep[StatePut[S, A]]) =
      (p.s, p.a)
    override def to(p: Rep[(S, A)]) = {
      val Pair(s, a) = p
      StatePut(s, a)
    }
    lazy val defaultRepTo = Default.defaultVal[Rep[StatePut[S, A]]](StatePut(element[S].defaultRepValue, element[A].defaultRepValue))
    lazy val eTo = new StatePutElem[S, A](this)
  }
  // 4) constructor and deconstructor
  abstract class StatePutCompanionAbs extends CompanionBase[StatePutCompanionAbs] with StatePutCompanion {
    override def toString = "StatePut"
    def apply[S, A](p: Rep[StatePutData[S, A]])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      isoStatePut(eS, eA).to(p)
    def apply[S, A](s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      mkStatePut(s, a)
  }
  object StatePutMatcher {
    def unapply[S, A](p: Rep[StateF[S, A]]) = unmkStatePut(p)
  }
  def StatePut: Rep[StatePutCompanionAbs]
  implicit def proxyStatePutCompanion(p: Rep[StatePutCompanionAbs]): StatePutCompanionAbs = {
    proxyOps[StatePutCompanionAbs](p)
  }

  implicit object StatePutCompanionElem extends CompanionElem[StatePutCompanionAbs] {
    lazy val tag = weakTypeTag[StatePutCompanionAbs]
    protected def getDefaultRep = StatePut
  }

  implicit def proxyStatePut[S, A](p: Rep[StatePut[S, A]]): StatePut[S, A] =
    proxyOps[StatePut[S, A]](p)

  implicit class ExtendedStatePut[S, A](p: Rep[StatePut[S, A]])(implicit eS: Elem[S], eA: Elem[A]) {
    def toData: Rep[StatePutData[S, A]] = isoStatePut(eS, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoStatePut[S, A](implicit eS: Elem[S], eA: Elem[A]): Iso[StatePutData[S, A], StatePut[S, A]] =
    new StatePutIso[S, A]

  // 6) smart constructor and deconstructor
  def mkStatePut[S, A](s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]]
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]): Option[(Rep[S], Rep[A])]
}

// Seq -----------------------------------
trait FreeStatesSeq extends FreeStatesDsl with ScalanSeq {
  self: MonadsDslSeq =>
  lazy val StateF: Rep[StateFCompanionAbs] = new StateFCompanionAbs with UserTypeSeq[StateFCompanionAbs] {
    lazy val selfType = element[StateFCompanionAbs]
  }

  case class SeqStateGet[S, A]
      (override val f: Rep[S => A])
      (implicit eS: Elem[S], eA: Elem[A])
    extends StateGet[S, A](f)
        with UserTypeSeq[StateGet[S, A]] {
    lazy val selfType = element[StateGet[S, A]]
  }
  lazy val StateGet = new StateGetCompanionAbs with UserTypeSeq[StateGetCompanionAbs] {
    lazy val selfType = element[StateGetCompanionAbs]
  }

  def mkStateGet[S, A]
      (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
      new SeqStateGet[S, A](f)
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]) = p match {
    case p: StateGet[S, A] @unchecked =>
      Some((p.f))
    case _ => None
  }

  case class SeqStatePut[S, A]
      (override val s: Rep[S], override val a: Rep[A])
      (implicit eS: Elem[S], eA: Elem[A])
    extends StatePut[S, A](s, a)
        with UserTypeSeq[StatePut[S, A]] {
    lazy val selfType = element[StatePut[S, A]]
  }
  lazy val StatePut = new StatePutCompanionAbs with UserTypeSeq[StatePutCompanionAbs] {
    lazy val selfType = element[StatePutCompanionAbs]
  }

  def mkStatePut[S, A]
      (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      new SeqStatePut[S, A](s, a)
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]) = p match {
    case p: StatePut[S, A] @unchecked =>
      Some((p.s, p.a))
    case _ => None
  }
}

// Exp -----------------------------------
trait FreeStatesExp extends FreeStatesDsl with ScalanExp {
  self: MonadsDslExp =>
  lazy val StateF: Rep[StateFCompanionAbs] = new StateFCompanionAbs with UserTypeDef[StateFCompanionAbs] {
    lazy val selfType = element[StateFCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  case class ExpStateGet[S, A]
      (override val f: Rep[S => A])
      (implicit eS: Elem[S], eA: Elem[A])
    extends StateGet[S, A](f) with UserTypeDef[StateGet[S, A]] {
    lazy val selfType = element[StateGet[S, A]]
    override def mirror(t: Transformer) = ExpStateGet[S, A](t(f))
  }

  lazy val StateGet: Rep[StateGetCompanionAbs] = new StateGetCompanionAbs with UserTypeDef[StateGetCompanionAbs] {
    lazy val selfType = element[StateGetCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  object StateGetMethods {
  }

  object StateGetCompanionMethods {
  }

  def mkStateGet[S, A]
    (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
    new ExpStateGet[S, A](f)
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: StateGetElem[S, A] @unchecked =>
      Some((p.asRep[StateGet[S, A]].f))
    case _ =>
      None
  }

  case class ExpStatePut[S, A]
      (override val s: Rep[S], override val a: Rep[A])
      (implicit eS: Elem[S], eA: Elem[A])
    extends StatePut[S, A](s, a) with UserTypeDef[StatePut[S, A]] {
    lazy val selfType = element[StatePut[S, A]]
    override def mirror(t: Transformer) = ExpStatePut[S, A](t(s), t(a))
  }

  lazy val StatePut: Rep[StatePutCompanionAbs] = new StatePutCompanionAbs with UserTypeDef[StatePutCompanionAbs] {
    lazy val selfType = element[StatePutCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  object StatePutMethods {
  }

  object StatePutCompanionMethods {
  }

  def mkStatePut[S, A]
    (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
    new ExpStatePut[S, A](s, a)
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: StatePutElem[S, A] @unchecked =>
      Some((p.asRep[StatePut[S, A]].s, p.asRep[StatePut[S, A]].a))
    case _ =>
      None
  }

  object StateFMethods {
  }

  object StateFCompanionMethods {
    object unit {
      def unapply(d: Def[_]): Option[Rep[A] forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "unit" =>
          Some(a).asInstanceOf[Option[Rep[A] forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[A] forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object get {
      def unapply(d: Def[_]): Option[Unit forSome {type S}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem == StateFCompanionElem && method.getName == "get" =>
          Some(()).asInstanceOf[Option[Unit forSome {type S}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Unit forSome {type S}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object set {
      def unapply(d: Def[_]): Option[Rep[S] forSome {type S}] = d match {
        case MethodCall(receiver, method, Seq(s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "set" =>
          Some(s).asInstanceOf[Option[Rep[S] forSome {type S}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[S] forSome {type S}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object eval {
      def unapply(d: Def[_]): Option[(Rep[FreeState[S,A]], Rep[S]) forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(t, s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "eval" =>
          Some((t, s)).asInstanceOf[Option[(Rep[FreeState[S,A]], Rep[S]) forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeState[S,A]], Rep[S]) forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object run {
      def unapply(d: Def[_]): Option[(Rep[FreeState[S,A]], Rep[S]) forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(t, s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "run" =>
          Some((t, s)).asInstanceOf[Option[(Rep[FreeState[S,A]], Rep[S]) forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeState[S,A]], Rep[S]) forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}
