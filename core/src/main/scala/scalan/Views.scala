package scalan

import scala.language.higherKinds
import scala.collection.mutable.{Map => MutMap}
import scala.reflect.ClassTag
import scalan.common.Lazy
import scalan.staged.{BaseExp,Transforming}

trait Views extends Elems { self: Scalan =>

  // eFrom0 is used to avoid making eFrom implicit in subtypes
  // and support recursive types
  abstract class Iso[From, To](implicit eFrom0: Elem[From]) {
    def eFrom: Elem[From] = eFrom0
    def eTo: Elem[To]
    // ideally this could be replaced by eTo.getDefaultRep and all implementations
    // moved to the corresponding *Elem class, but this leads to ambiguous implicits
    def from(p: Rep[To]): Rep[From]
    def to(p: Rep[From]): Rep[To]
    override def toString = s"${eFrom.name} <-> ${eTo.name}"
    override def equals(other: Any) = other match {
      case i: Iso[_, _] => (this eq i) || (eFrom == i.eFrom && eTo == i.eTo)
      case _ => false
    }
    override def hashCode = 41 * eFrom.hashCode + eTo.hashCode
    def isIdentity: Boolean = false
    lazy val fromFun = fun { x: Rep[To] => from(x) }(Lazy(eTo), eFrom)
    lazy val toFun = fun { x: Rep[From] => to(x) }(Lazy(eFrom), eTo)

    if (isDebug) {
      debug$IsoCounter(this) += 1
    }
  }
  implicit class IsoOps[A,B](iso: Iso[A,B]) {
    def asIso[C,D] = iso.asInstanceOf[Iso[C,D]]
    def >>[C](iso2: Iso[B,C]): Iso[A,C] = composeIso(iso2, iso)
  }

  private val debug$IsoCounter = counter[Iso[_, _]]

  private val isoCache = MutMap.empty[(Class[_], Seq[AnyRef]), AnyRef]

  def cachedIso[I <: Iso[_, _]](args: AnyRef*)(implicit tag: ClassTag[I]) = {
    val clazz = tag.runtimeClass
    isoCache.getOrElseUpdate(
      (clazz, args), {
        val constructors = clazz.getDeclaredConstructors
        if (constructors.length != 1) {
          !!!(s"Iso class $clazz has ${constructors.length} constructors, 1 expected")
        } else {
          val constructor = constructors(0)
          val constructorArgs = self +: args
          constructor.newInstance(constructorArgs: _*).asInstanceOf[Iso[_, _]]
        }
      }).asInstanceOf[I]
  }

  abstract class Iso1[A, B, C[_]](val innerIso: Iso[A,B])(implicit cC: Cont[C])
    extends Iso[C[A], C[B]]()(cC.lift(innerIso.eFrom)) {
    implicit val eA = innerIso.eFrom
    implicit val eB = innerIso.eTo
    lazy val eTo = cC.lift(innerIso.eTo)
    override def isIdentity = innerIso.isIdentity
  }

  implicit def viewElement[From, To](implicit iso: Iso[From, To]): Elem[To] = iso.eTo // always ask elem from Iso

  trait ViewElem[From, To] extends Elem[To] { _: scala.Equals =>
    def iso: Iso[From, To]
    override def isEntityType = shouldUnpack(this)
  }

  object ViewElem {
    def unapply[From, To](ve: ViewElem[From, To]): Option[Iso[From, To]] = Some(ve.iso)
  }

  trait ViewElem1[A,From,To,C[_]] extends ViewElem[From, To] { _: scala.Equals =>
    def eItem: Elem[A]
    def cont: Cont[C]
  }

  object UnpackableElem {
    def unapply(e: Elem[_]) = {
      val iso = getIsoByElem(e)
      if (iso.isIdentity)
        None
      else
        Some(iso)
    }
  }

  def shouldUnpack(e: Elem[_]): Boolean

  trait IsoBuilder { def apply[T](e: Elem[T]): Iso[_,T] }

  object PairIsos {
    def fromElem[A,B](pe: PairElem[A,B]) = (getIsoByElem(pe.eFst), getIsoByElem(pe.eSnd))

    def unapply[T](e: Elem[T]): Option[(PairElem[_,_], Iso[_,_], Iso[_,_])] = e match {
      case pe: PairElem[a,b] =>
        fromElem(pe) match {
          case (iso1: Iso[s, a], iso2: Iso[t, b]) => Some((pe, iso1, iso2))
          case _ => None
        }
      case _ => None
    }
  }

  def getIsoByElem[T](e: Elem[T]): Iso[_, T] = {
    if (currentPass.config.shouldUnpackTuples) {
      buildIso(e, new IsoBuilder {
        def apply[S](e: Elem[S]) = {
          val res = e match {
            case PairIsos(_, iso1: Iso[s,a], iso2: Iso[t,b]) =>
              if (iso1.isIdentity && iso2.isIdentity) {
                // recursion base (structs)
                val sIso = structToPairIso[Any,s,t,a,b](iso1, iso2)
                val flatIso = flatteningIso(sIso.eFrom.asStructElem[Any])
                flatIso >> sIso.asIso[Any,S]
              }
              else {
                val pIso = pairIso(iso1, iso2)
                val deepIso = getIsoByElem(pIso.eFrom)
                deepIso >> pIso//.asIso[(s,t),S]
              }
            case _ =>
              getIsoByElem(e)
          }
          res.asIso[Any,S]
        }
      })
    }
    else {
      buildIso(e, new IsoBuilder {
        def apply[S](e: Elem[S]) = {
          val res = e match {
            case PairIsos(_, iso1: Iso[s,a], iso2: Iso[t,b]) =>
              if (iso1.isIdentity && iso2.isIdentity) {
                // recursion base
                pairIso(iso1, iso2)
              }
              else {
                getIsoByElem(e)
              }
            case _ =>
              getIsoByElem(e)
          }
          res.asIso[Any,S]
        }
      })
    }
  }

  def buildIso[T](e: Elem[T], builder: IsoBuilder): Iso[_, T] = isoCache.getOrElseUpdate(
    (classOf[Iso[_, _]], Seq(e)),
    e match {
      case ve: ViewElem[_,_] =>
        val eFrom = ve.iso.eFrom
        val deepIso = builder(eFrom)
        if (deepIso.isIdentity)
          ve.iso
        else
          deepIso >> ve.iso
      case pe: PairElem[a,b] =>
        (builder(pe.eFst), builder(pe.eSnd)) match {
          case (iso1: Iso[s,a], iso2: Iso[t,b]) =>
            val pIso = pairIso(iso1,iso2)
            val deepIso = builder(pIso.eFrom)
            deepIso >> pIso
        }
      case pe: SumElem[a,b] =>
        val iso1 = builder(pe.eLeft)
        val iso2 = builder(pe.eRight)
        sumIso(iso1,iso2)
      case fe: FuncElem[a,b] =>
        val iso1 = builder(fe.eDom)
        val iso2 = builder(fe.eRange)
        funcIso(iso1,iso2)
      case ae: ArrayElem[_] =>
        val iso = builder(ae.eItem)
        arrayIso(iso)
      case ae: ListElem[_] =>
        val iso = builder(ae.eItem)
        listIso(iso)
      case ae: ArrayBufferElem[_] =>
        val iso = builder(ae.eItem)
        arrayBufferIso(iso)
      case ae: ThunkElem[_] =>
        val iso = builder(ae.eItem)
        thunkIso(iso)
      case me: MMapElem[_,_] =>
        identityIso(me)

      case we: WrapperElem1[a, Def[ext], cbase, cw] @unchecked =>
        val eExt = we.eTo
        val iso = builder(eExt)
        iso
      case we: WrapperElem[Def[base],Def[ext]] @unchecked =>
        val eExt = we.eTo
        val iso = builder(eExt)
        iso

      //    case ee1: EntityElem1[_,_,_] =>
      //      val iso = getIsoByElem(ee1.eItem)
      //      TODO implement using ContainerIso
      case ee: EntityElem[_] =>
        identityIso(ee)
      case be: BaseElem[_] =>
        identityIso(be)
      case se: StructElem[_] =>
        identityIso(se)
      case _ => !!!(s"Don't know how to build iso for element $e")
    }
  ).asInstanceOf[Iso[_,T]]

  case class IdentityIso[A](eTo: Elem[A]) extends Iso[A, A]()(eTo) {
    def from(x: Rep[A]) = x
    def to(x: Rep[A]) = x
    override def isIdentity = true
  }
  def identityIso[A](implicit elem: Elem[A]): Iso[A, A] = cachedIso[IdentityIso[A]](elem)

  case class PairIso[A1, A2, B1, B2](iso1: Iso[A1, B1], iso2: Iso[A2, B2])
    extends Iso[(A1, A2), (B1, B2)]()(pairElement(iso1.eFrom, iso2.eFrom)) {
    implicit def eA1 = iso1.eFrom
    implicit def eA2 = iso2.eFrom
    implicit def eB1 = iso1.eTo
    implicit def eB2 = iso2.eTo
    lazy val eTo = element[(B1, B2)]

    var fromCacheKey:Option[Rep[(B1,B2)]] = None
    var fromCacheValue:Option[Rep[(A1,A2)]] = None
    var toCacheKey:Option[Rep[(A1,A2)]] = None
    var toCacheValue:Option[Rep[(B1,B2)]] = None

    def from(b: Rep[(B1, B2)]) = {
      if (fromCacheKey.isEmpty || b != fromCacheKey.get) {
        fromCacheKey = Some(b)
        fromCacheValue = Some((iso1.from(b._1), iso2.from(b._2)))
      }
      fromCacheValue.get
    }
    def to(a: Rep[(A1, A2)]) = {
      if (toCacheKey.isEmpty || a != toCacheKey.get) {
        toCacheKey = Some(a)
        toCacheValue = Some((iso1.to(a._1), iso2.to(a._2)))
      }
      toCacheValue.get
    }
    override def isIdentity = iso1.isIdentity && iso2.isIdentity
  }
  def pairIso[A1, A2, B1, B2](iso1: Iso[A1, B1], iso2: Iso[A2, B2]): Iso[(A1, A2), (B1, B2)] = 
    cachedIso[PairIso[A1, A2, B1, B2]](iso1, iso2)

  case class SumIso[A1, A2, B1, B2](iso1: Iso[A1, B1], iso2: Iso[A2, B2])
    extends Iso[A1 | A2, B1 | B2]()(sumElement(iso1.eFrom, iso2.eFrom)) {
//    implicit val eA1 = iso1.eFrom
//    implicit val eA2 = iso2.eFrom
    implicit def eB1 = iso1.eTo
    implicit def eB2 = iso2.eTo
    lazy val eTo = element[B1 | B2]
    def from(b: Rep[B1 | B2]) =
      b.mapSumBy(iso1.fromFun, iso2.fromFun)
    def to(a: Rep[A1 | A2]) =
      a.mapSumBy(iso1.toFun, iso2.toFun)
    override def isIdentity = iso1.isIdentity && iso2.isIdentity
  }
  def sumIso[A1, A2, B1, B2](iso1: Iso[A1, B1], iso2: Iso[A2, B2]): Iso[A1 | A2, B1 | B2] =
    cachedIso[SumIso[A1, A2, B1, B2]](iso1, iso2)

  case class ComposeIso[A,B,C](iso2: Iso[B, C], iso1: Iso[A, B]) extends Iso[A, C]()(iso1.eFrom) {
    def eTo = iso2.eTo
    def from(c: Rep[C]) = iso1.from(iso2.from(c))
    def to(a: Rep[A]) = iso2.to(iso1.to(a))
    override def isIdentity = iso1.isIdentity && iso2.isIdentity
  }

  def composeIso[A, B, C](iso2: Iso[B, C], iso1: Iso[A, B]): Iso[A, C] = {
    ((iso2, iso1) match {
      case (IdentityIso(_), _) => iso1
      case (_, IdentityIso(_)) => iso2
      case (PairIso(iso21, iso22), PairIso(iso11, iso12)) => 
        pairIso(composeIso(iso21, iso11), composeIso(iso22, iso12))
      case _ => cachedIso[ComposeIso[A, B, C]](iso2, iso1)
    }).asInstanceOf[Iso[A, C]]
  }

  case class FuncIso[A, B, C, D](iso1: Iso[A, B], iso2: Iso[C, D])
    extends Iso[A => C, B => D]()(funcElement(iso1.eFrom, iso2.eFrom)) {
    implicit def eA = iso1.eFrom
    implicit def eB = iso1.eTo
    implicit def eC = iso2.eFrom
    implicit def eD = iso2.eTo
    lazy val eTo = funcElement(eB, eD)
    def from(f: Rep[B => D]): Rep[A => C] = {
      fun { b => iso2.from(f(iso1.to(b))) }
    }
    def to(f: Rep[A => C]): Rep[B => D] = {
      fun { a => iso2.to(f(iso1.from(a))) }
    }
    override def isIdentity = iso1.isIdentity && iso2.isIdentity
  }
  def funcIso[A, B, C, D](iso1: Iso[A, B], iso2: Iso[C, D]): Iso[A => C, B => D] =
    cachedIso[FuncIso[A, B, C, D]](iso1, iso2)

  case class ConverterIso[A, B](convTo: Conv[A,B], convFrom: Conv[B,A])
    extends Iso[A,B]()(convTo.eT) {
    def eTo = convTo.eR
    def to(a: Rep[A]) = convTo(a)
    def from(b: Rep[B]) = convFrom(b)
    override lazy val toFun = convTo.convFun
    override lazy val fromFun = convFrom.convFun
    override def isIdentity = false
  }
  def converterIso[A, B](convTo: Conv[A,B], convFrom: Conv[B,A]): Iso[A,B] =
    cachedIso[ConverterIso[A, B]](convTo.asInstanceOf[AnyRef], convFrom.asInstanceOf[AnyRef])

  def convertBeforeIso[A, B, C](convTo: Conv[A,B], convFrom: Conv[B,A], iso: Iso[B,C]): Iso[A, C] = composeIso(iso, converterIso(convTo, convFrom))

  def convertAfterIso[A,B,C](iso: Iso[A,B], convTo: Conv[B,C], convFrom: Conv[C,B]): Iso[A, C] = composeIso(converterIso(convTo, convFrom), iso)

  def unifyIsos[A,B,C,D](iso1: Iso[A,C], iso2: Iso[B,D],
                         toD: Conv[C,D], toC: Conv[D,C]): (Iso[A,C], Iso[B,C]) = {
    val ea = iso1.eFrom
    val eb = iso2.eFrom
    implicit val ec = iso1.eTo
    val (i1, i2) =
      if (ec == iso2.eTo)
        (iso1, iso2.asInstanceOf[Iso[B,C]])
      else
        (iso1, convertAfterIso(iso2, toC, toD))
    (i1, i2)
  }
}

trait ViewsSeq extends Views { self: ScalanSeq =>
  def shouldUnpack(e: Elem[_]) = true
}

trait ViewsExp extends Views with BaseExp { self: ScalanExp =>
  case class MethodCallFromExp(clazzUT: Class[_], methodName: String) {
    def unapply[T](d: Def[T]): Option[(Exp[_], List[Exp[_]])] = d match {
      case MethodCall(obj, m, args, _) if m.getName == methodName =>
        Some((obj, args.asInstanceOf[List[Exp[_]]]))
      case _ => None
    }
  }

  type Unpacked[T] = (Rep[Source], Iso[Source, T]) forSome { type Source }
  type UnpackedLambdaResult[T,R] = (Rep[T => R], Iso[Source, R]) forSome { type Source }

  type UnpackTester = Element[_] => Boolean

  protected var unpackTesters: Set[UnpackTester] = Set.empty

  def addUnpackTester(tester: UnpackTester): Unit =
    unpackTesters += tester
  def removeUnpackTester(tester: UnpackTester): Unit =
    unpackTesters -= tester

  def shouldUnpack(e: Elem[_]) = unpackTesters.exists(_(e))

  def defaultUnpackTester(e: Elem[_]) = true //e match { case pe: PairElem[_,_] => false case _ => true }

  object HasViews {
    def unapply[T](s: Exp[T]): Option[Unpacked[T]] =
      if (!okRewrite)
        None
      else
        unapplyViews(s)
  }

  // for simplifying unapplyViews
  protected def trivialUnapply[T](s: Exp[T]) = (s, identityIso(s.elem))

  def unapplyViews[T](s: Exp[T]): Option[Unpacked[T]] = (s match {
    case Def(l @ SLeft(s)) =>
      (unapplyViews(s), UnpackableElem.unapply(l.eRight)) match {
        case (None, None) => None
        case (opt1, opt2) =>
          val (sv1, iso1) = opt1.getOrElse(trivialUnapply(s))
          val iso2 = opt2.getOrElse(identityIso(l.eRight))
          Some((sv1.asLeft(iso2.eFrom), sumIso(iso1, iso2)))
      }
    case Def(r @ SRight(s)) =>
      (UnpackableElem.unapply(r.eLeft), unapplyViews(s)) match {
        case (None, None) => None
        case (opt1, opt2) =>
          val (sv2, iso2) = opt2.getOrElse(trivialUnapply(s))
          val iso1 = opt1.getOrElse(identityIso(r.eLeft))
          Some((sv2.asRight(iso1.eFrom), sumIso(iso1, iso2)))
      }
    case _ =>
      UnpackableExp.unapply(s)
  }).asInstanceOf[Option[Unpacked[T]]]

  object UnpackableDef {
    def unapply[T](d: Def[T]): Option[Unpacked[T]] =
      d match {
        case view: View[a, T] => Some((view.source, view.iso))
        // TODO make UserTypeDef extend View with lazy iso/source?
        case _ =>
          val eT = d.selfType
          eT match {
            case UnpackableElem(iso: Iso[a, T @unchecked]) =>
              Some((noRW { iso.from(d.self) }, iso))
            case _ => None
          }
      }
  }

  object UnpackableExp {
    def unapply[T](e: Exp[T]): Option[Unpacked[T]] =
      e match {
        case Def(d) => UnpackableDef.unapply(d)
        case _ =>
          val eT = e.elem
          eT match {
            case UnpackableElem(iso: Iso[a, T @unchecked]) =>
              Some((noRW { iso.from(e) }, iso))
            case _ => None
          }
      }
  }

  object LambdaResultHasViews {
    def unapply[A,C](l: Rep[A => C]): Option[UnpackedLambdaResult[A,C]] = l match {
      case Def(Lambda(_, _, _, HasViews(_, iso: Iso[b, _]))) =>
        Some((l, iso))
      case _ => None
    }
  }

  abstract class View[From, To] extends Def[To] {
    def source: Rep[From]
    def iso: Iso[From, To]
    implicit def selfType = iso.eTo
  }

  case class UnpackView[A, B](view: Rep[B])(implicit iso: Iso[A, B]) extends Def[A] {
    implicit def selfType = iso.eFrom
  }

  abstract class View1[A, B, C[_]](val iso: Iso1[A,B,C]) extends View[C[A], C[B]] {
    def innerIso = iso.innerIso
  }

  abstract class View2[A1, A2, B1, B2, C[_, _]](implicit val iso1: Iso[A1, B1], val iso2: Iso[A2, B2]) extends View[C[A1, A2], C[B1, B2]]

  case class PairView[A1, A2, B1, B2](source: Rep[(A1, A2)])(implicit iso1: Iso[A1, B1], iso2: Iso[A2, B2]) extends View2[A1, A2, B1, B2, Tuple2] {
    lazy val iso = pairIso(iso1, iso2)
  }

  case class SumView[A1, A2, B1, B2](source: Rep[A1|A2])(implicit iso1: Iso[A1, B1], iso2: Iso[A2, B2]) extends View2[A1, A2, B1, B2, | ] {
    lazy val iso = sumIso(iso1, iso2)
  }

  override def rewriteDef[T](d: Def[T]) = d match {
    // Rule: (V(a, iso1), V(b, iso2)) ==> V((a,b), PairIso(iso1, iso2))
    case Tup(Def(UnpackableDef(a, iso1: Iso[a, c])), Def(UnpackableDef(b, iso2: Iso[b, d]))) =>
      PairView((a.asRep[a], b.asRep[b]))(iso1, iso2)

    // Rule: (V(a, iso1), b) ==> V((a,b), PairIso(iso1, id))
    case Tup(Def(UnpackableDef(a, iso1: Iso[a, c])), b: Rep[b]) =>
      PairView((a.asRep[a], b))(iso1, identityIso(b.elem)).self

    // Rule: (a, V(b, iso2)) ==> V((a,b), PairIso(id, iso2))
    case Tup(a: Rep[a], Def(UnpackableDef(b, iso2: Iso[b, d]))) =>
      PairView((a, b.asRep[b]))(identityIso(a.elem), iso2).self

    // Rule: V(a, iso1) ; V(b, iso2)) ==> iso2.to(a ; b)
    case block@Semicolon(Def(UnpackableDef(a, iso1: Iso[a, c])), Def(UnpackableDef(b, iso2: Iso[b, d]))) =>
      iso2.to(semicolon(a.asRep[a], b.asRep[b]))

    // Rule: a ; V(b, iso2)) ==> iso2.to(a ; b)
    case block@Semicolon(a: Rep[a], Def(UnpackableDef(b, iso2: Iso[b, d]))) =>
      iso2.to(semicolon(a, b.asRep[b]))

    // Rule: V(a, iso1) ; b ==> a ; b
    case block@Semicolon(Def(UnpackableDef(a, iso1: Iso[a, c])), b: Rep[b]) =>
      semicolon(a.asRep[a], b)

    // Rule: PairView(source, iso1, _)._1  ==> iso1.to(source._1)
    case First(Def(view@PairView(source))) =>
      view.iso1.to(source._1)

    // Rule: PairView(source, _, iso2)._2  ==> iso2.to(source._2)
    case Second(Def(view@PairView(source))) =>
      view.iso2.to(source._2)

    // Rule: PairView(PairView(source, i2), i1)  ==> PairView(source, PairIso(composeIso(i1.iso1, i2.iso1), composeIso(i1.iso2, i2.iso2)))
    case v1@PairView(Def(v2@PairView(source))) => {
      val pIso1 = composeIso(v1.iso1,v2.iso1)
      val pIso2 = composeIso(v1.iso2, v2.iso2)
      PairView(source)(pIso1, pIso2)
    }

    // Rule: UnpackView(V(source, iso))  ==> source
//    case UnpackView(Def(view: View[a, b])) => view.source

    // Rule: ParExec(nJobs, f @ i => ... V(_, iso)) ==> V(ParExec(nJobs, f >> iso.from), arrayIso(iso))
    case ParallelExecute(nJobs:Rep[Int], f@Def(Lambda(_, _, _, UnpackableExp(_, iso: Iso[a, b])))) =>
      implicit val ea = iso.eFrom
      val parRes = ParallelExecute(nJobs, fun { i => iso.from(f(i)) })(iso.eFrom)
      ViewArray(parRes)(arrayIso(iso))

    // Rule: ArrayFold(xs, V(init, iso), step) ==> iso.to(ArrayFold(xs, init, p => iso.from(step(iso.to(p._1), p._2)) ))
    case ArrayFold(xs: Rep[Array[t]] @unchecked, HasViews(init, iso: Iso[a, b]), step) =>
      val init1 = init.asRep[a]
      implicit val eT = xs.elem.asElem[Array[t]].eItem
      implicit val eA = iso.eFrom
      implicit val eB = iso.eTo
      val step1 = fun { (p: Rep[(a,t)]) =>
        val x_viewed = (iso.to(p._1), p._2)
        val res_viewed = step.asRep[((b,t)) => b](x_viewed)
        val res = iso.from(res_viewed)
        res
      }
      val foldRes = ArrayFold(xs, init1, step1)
      iso.to(foldRes)

    // Rule: loop(V(start, iso), step, isMatch) ==> iso.to(loop(start, iso.to >> step >> iso.from, iso.to >> isMatch))
    case LoopUntil(HasViews(startWithoutViews, iso: Iso[a, b]), step, isMatch) =>
      val start1 = startWithoutViews.asRep[a]
      implicit val eA = iso.eFrom
      implicit val eB = iso.eTo
      val step1 = fun { (x: Rep[a]) =>
        val x_viewed = iso.to(x)
        val res_viewed = step.asRep[b => b](x_viewed) // mirrorApply(step.asRep[b => b], x_viewed)
        val res = iso.from(res_viewed)
        res
      }
      val isMatch1 = fun { (x: Rep[a]) =>
        val x_viewed = iso.to(x)
        val res = isMatch.asRep[b => Boolean](x_viewed) // mirrorApply(isMatch.asRep[b => Boolean], x_viewed)
        res
      }
      val loopRes = LoopUntil(start1, step1, isMatch1)
      iso.to(loopRes)

    case call @ MethodCall(Def(obj), m, args, neverInvoke) =>
      call.tryInvoke match {
        // Rule: obj.m(args) ==> body(m).subst{xs -> args}
        case InvokeSuccess(res) => res
        case InvokeFailure(e) =>
          if (e.isInstanceOf[DelayInvokeException])
            super.rewriteDef(d)
          else
            !!!(s"Failed to invoke $call", e)
        case InvokeImpossible =>
          implicit val resultElem: Elem[T] = d.selfType
          // asRep[T] cast below should be safe
          // explicit resultElem to make sure both branches have the same type
          def copyMethodCall(newReceiver: Exp[_]) =
            mkMethodCall(newReceiver, m, args, neverInvoke, resultElem).asRep[T]

          obj match {
            // Rule: (if(c) t else e).m(args) ==> if (c) t.m(args) else e.m(args)
            case IfThenElse(cond, t, e) =>
              IF (cond) {
                copyMethodCall(t)
              } ELSE {
                copyMethodCall(e)
              }
            case _ =>
              super.rewriteDef(d)
          }
      }
    case _ => super.rewriteDef(d)
  }

//  override def rewriteVar[T](v: Exp[T]) = v.elem match {
//    case UnpackableElem(iso: Iso[a, T @unchecked]) =>
//      iso.to(fresh[a](Lazy(iso.eFrom)))
//    case _ => super.rewriteVar(v)
//  }
}
