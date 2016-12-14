package scalan.collections

import scalan._
import scala.reflect.runtime.universe._
import scalan.util.Invariant

trait ArrayBuffers extends Base { self: Scalan =>
  trait ArrayBuffer[T] {
    implicit val eItem: Elem[T]
    def apply(i: Rep[Int]): Rep[T] 
    def length : Rep[Int]
    def mapBy[R: Elem](f: Rep[T => R]): Rep[ArrayBuffer[R]]
    def map[R: Elem](f: Rep[T] => Rep[R]): Rep[ArrayBuffer[R]] = mapBy(fun(f))
    def update(i: Rep[Int], v: Rep[T]): Rep[ArrayBuffer[T]]
    def insert(i: Rep[Int], v: Rep[T]): Rep[ArrayBuffer[T]]
    def +=(v: Rep[T]): Rep[ArrayBuffer[T]]
    def ++=(a: Arr[T]): Rep[ArrayBuffer[T]]
    def remove(i: Rep[Int], n: Rep[Int]): Rep[ArrayBuffer[T]]
    def reset():  Rep[ArrayBuffer[T]]     
    def toArray: Arr[T]
  }

  type ArrBuf[T] = Rep[ArrayBuffer[T]]

  object ArrayBuffer { 
    def apply[T: Elem](v: Rep[T]) = initArrayBuffer[T](v)
    def create[T: Elem](count: Rep[Int], f:Rep[Int]=>Rep[T]) = createArrayBuffer(count, f)
    def make[T](name: Rep[String])(implicit e:Elem[T]) = makeArrayBuffer[T](name)(e)
    def empty[T: Elem] = emptyArrayBuffer[T]
    def fromArray[T: Elem](arr: Arr[T]) = createArrayBufferFromArray(arr)
  }

  implicit val arrayBufferFunctor = new Functor[ArrayBuffer] {
    def tag[T](implicit tT: WeakTypeTag[T]) = weakTypeTag[ArrayBuffer[T]]
    def lift[T](implicit eT: Elem[T]) = element[ArrayBuffer[T]]
    def unlift[T](implicit eFT: Elem[ArrayBuffer[T]]) = eFT.eItem
    def getElem[T](fa: Rep[ArrayBuffer[T]]) = !!!("Operation is not supported by ArrayBuffer container " + fa)
    def unapply[T](e: Elem[_]) = e match {
      case e: ArrayBufferElem[_] => Some(e.asElem[ArrayBuffer[T]])
      case _ => None
    }
    def map[A:Elem,B:Elem](xs: Rep[ArrayBuffer[A]])(f: Rep[A] => Rep[B]) = xs.mapBy(fun(f))
  }

  case class ArrayBufferElem[A](override val eItem: Elem[A])
    extends EntityElem1[A, ArrayBuffer[A], ArrayBuffer](eItem, container[ArrayBuffer]) {
    def parent: Option[Elem[_]] = None
    override def isEntityType = eItem.isEntityType
    override lazy val typeArgs = TypeArgs("A" -> (eItem -> Invariant))

    lazy val tag = {
      implicit val tag1 = eItem.tag
      weakTypeTag[ArrayBuffer[A]]
    }
    protected def getDefaultRep = ArrayBuffer.empty[A](eItem)
  }

  implicit def arrayBufferElement[A](implicit eItem: Elem[A]): Elem[ArrayBuffer[A]] = new ArrayBufferElem[A](eItem)
  implicit def ArrayBufferElemExtensions[A](eArr: Elem[ArrayBuffer[A]]): ArrayBufferElem[A] = eArr.asInstanceOf[ArrayBufferElem[A]]

  implicit def resolveArrayBuffer[T: Elem](buf: Rep[ArrayBuffer[T]]): ArrayBuffer[T]

  def emptyArrayBuffer[T: Elem]: Rep[ArrayBuffer[T]]
  def initArrayBuffer[T: Elem](v: Rep[T]): Rep[ArrayBuffer[T]]
  def makeArrayBuffer[T](name: Rep[String])(implicit e:Elem[T]): Rep[ArrayBuffer[T]]
  def createArrayBuffer[T: Elem](count: Rep[Int], f:Rep[Int=>T]): Rep[ArrayBuffer[T]]
  def createArrayBufferFromArray[T: Elem](arr: Arr[T]): Rep[ArrayBuffer[T]]
}

trait ArrayBuffersStd extends ArrayBuffers { self: ScalanStd =>
  implicit class SeqArrayBuffer[T](val impl: scala.collection.mutable.ArrayBuffer[T])(implicit val eItem: Elem[T]) extends ArrayBuffer[T] {
    def apply(i: Rep[Int]): Rep[T] = impl.apply(i)
    def length : Rep[Int] = impl.length
    def mapBy[R: Elem](f: Rep[T => R]): Rep[ArrayBuffer[R]] = new SeqArrayBuffer[R](impl.map(f))
    def update(i: Rep[Int], v: Rep[T]): Rep[ArrayBuffer[T]] = { impl.update(i, v); this }
    def insert(i: Rep[Int], v: Rep[T]): Rep[ArrayBuffer[T]] = { impl.insert(i, v); this }
    def +=(v: Rep[T]): Rep[ArrayBuffer[T]] = { impl += v; this }
    def ++=(a: Arr[T]): Rep[ArrayBuffer[T]] = { impl ++= a; this }
    def remove(i: Rep[Int], n: Rep[Int]): Rep[ArrayBuffer[T]] = { impl.remove(i, n); this }
    def reset():  Rep[ArrayBuffer[T]] = { impl.clear(); this }
    def toArray: Arr[T] = impl.toArray(eItem.classTag)
  }  

  implicit def resolveArrayBuffer[T: Elem](buf: Rep[ArrayBuffer[T]]): ArrayBuffer[T] = buf

  def emptyArrayBuffer[T: Elem]: Rep[ArrayBuffer[T]] = scala.collection.mutable.ArrayBuffer.empty[T]
  def initArrayBuffer[T: Elem](v: Rep[T]): Rep[ArrayBuffer[T]] = scala.collection.mutable.ArrayBuffer(v)
  def makeArrayBuffer[T](name: Rep[String])(implicit e:Elem[T]): Rep[ArrayBuffer[T]] = scala.collection.mutable.ArrayBuffer.empty[T]
  def createArrayBuffer[T: Elem](count: Rep[Int], f:Rep[Int=>T]): Rep[ArrayBuffer[T]] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[T]
    for (i <- 0 until count) {
      buf += f(i)
    }
    buf
  }
  def createArrayBufferFromArray[T: Elem](arr: Arr[T]): Rep[ArrayBuffer[T]] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[T]
    buf ++= arr
    buf
  }
}

trait ArrayBuffersExp extends ArrayBuffers with ViewsDslExp { self: ScalanExp =>
  case class ViewArrayBuffer[A, B](source: Rep[ArrayBuffer[A]], override val innerIso: Iso[A, B])
    extends View1[A, B, ArrayBuffer](arrayBufferIso(innerIso)) {
    override def toString = s"ViewArrayBuffer[${innerIso.eTo.name}]($source)"
    override def equals(other: Any) = other match {
      case v: ViewArrayBuffer[_, _] => source == v.source && innerIso.eTo == v.innerIso.eTo
      case _ => false
    }
  }

  object UserTypeArrayBuffer {
    def unapply(s: Exp[_]): Option[Iso[_, _]] = {
      s.elem match {
        case ArrayBufferElem(UnpackableElem(iso)) => Some(iso)
        case _ => None
      }
    }
  }

  override def unapplyViews[T](s: Exp[T]): Option[Unpacked[T]] = (s match {
    case Def(view: ViewArrayBuffer[_, _]) =>
      Some((view.source, view.iso))
    case UserTypeArrayBuffer(iso: Iso[a, b]) =>
      val newIso = arrayBufferIso(iso)
      val repr = reifyObject(UnpackView(s.asRep[ArrayBuffer[b]], newIso))
      Some((repr, newIso))
    case _ =>
      super.unapplyViews(s)
  }).asInstanceOf[Option[Unpacked[T]]]


  protected def hasArrayBufferViewArg(s: Exp[_]): Boolean = s match {
    case Def(_: ViewArrayBuffer[_, _]) => true
    case _ => false
  }

  val HasArrayBufferViewArg = HasArg(hasArrayBufferViewArg)

  override def rewriteDef[T](d: Def[T]) = d match {
    //------------------------------------------------------------
    // Iso lifting rules
    case ArrayBufferFromElem(HasViews(v, Def(iso: IsoUR[a, b]))) =>
      implicit val eA = iso.eFrom
      val v1 = v.asRep[a]
      ViewArrayBuffer(ArrayBufferFromElem(v1), iso)

    case ArrayBufferUpdate(HasViews(buf, Def(iso: ArrayBufferIso[a, b])), i, v@HasViews(_, _)) =>
      implicit val eA = iso.innerIso.eFrom
      val buf1 = buf.asRep[ArrayBuffer[a]]
      val v1 = iso.innerIso.from(v.asRep[b])
      ViewArrayBuffer(buf1.update(i, v1), iso.innerIso)

    case ArrayBufferInsert(HasViews(buf, Def(iso: ArrayBufferIso[a, b])), i, v@HasViews(_, _)) =>
      implicit val eA = iso.innerIso.eFrom
      val buf1 = buf.asRep[ArrayBuffer[a]]
      val v1 = iso.innerIso.from(v.asRep[b])
      ViewArrayBuffer(buf1.insert(i, v1), iso.innerIso)

    case ArrayBufferAppend(HasViews(buf, Def(iso: ArrayBufferIso[a, b])), v@HasViews(_, _)) =>
      implicit val eA = iso.innerIso.eFrom
      val buf1 = buf.asRep[ArrayBuffer[a]]
      val v1 = iso.innerIso.from(v.asRep[b])
      ViewArrayBuffer(buf1 += v1, iso.innerIso)

    case ArrayBufferAppendArray(HasViews(buf, Def(iso: ArrayBufferIso[a, b])), a@HasViews(_, Def(_: ArrayIso[_, _]))) =>
      implicit val eA = iso.innerIso.eFrom
      val buf1 = buf.asRep[ArrayBuffer[a]]
      val arrIso = arrayIso(iso.innerIso)
      val a1 = arrIso.from(a.asRep[Array[b]])
      ViewArrayBuffer(buf1 ++= a1, iso.innerIso)

    case mk@MakeArrayBuffer(ctx) if UnpackableElem.unapply(mk.eItem).isDefined =>
       mk.eItem match {
         case UnpackableElem(iso: Iso[a, b]) =>
           implicit val eA = iso.eFrom
           ViewArrayBuffer(ArrayBuffer.make[a](ctx), iso)
       }
    case ArrayBufferLength(Def(ViewArrayBuffer(xs: ArrBuf[a], _))) =>
      val xs1 = xs.asRep[ArrBuf[a]]
      implicit val eA = xs.elem.eItem
      xs.length

    case (_: ArrayBufferMap[_,r]) && ArrayBufferMap(HasViews(xs_, Def(iso: ArrayBufferIso[a, b])), f_) =>
      val xs = xs_.asRep[ArrayBuffer[a]]
      val f = f_.asRep[b => r]
      val innerIso = iso.innerIso
      implicit val eA = innerIso.eFrom
      implicit val eR = f.elem.eRange
      xs.mapBy(innerIso.toFun >> f)

    case ArrayBufferMap(xs: Rep[ArrayBuffer[a]] @unchecked, f@Def(Lambda(_, _, _, HasViews(_, iso: Iso[c, b])))) =>
      val f1 = f.asRep[a => b]
      val xs1 = xs.asRep[ArrayBuffer[a]]
      implicit val eA = xs1.elem.eItem
      implicit val eC = iso.eFrom
      val s = xs1.mapBy(f1 >> iso.fromFun)
      ViewArrayBuffer(s, iso)

    case view1@ViewArrayBuffer(Def(view2@ViewArrayBuffer(arr, innerIso2)), innerIso1) =>
      val compIso = composeIso(innerIso1, innerIso2)
      implicit val eAB = compIso.eTo
      ViewArrayBuffer(arr, compIso)

    case ArrayBufferToArray(Def(view: ViewArrayBuffer[a, b])) =>
      val innerIso = view.innerIso
      implicit val eA = innerIso.eFrom
      implicit val eB = innerIso.eTo
      val res = ViewArray(view.source.toArray, innerIso)
      res

    case ArrayBufferFromArray(HasViews(xs, Def(iso: ArrayIso[a, b]))) =>
      val xs1 = xs.asRep[Array[a]]
      val innerIso = iso.innerIso
      implicit val eA = innerIso.eFrom
      implicit val eB = innerIso.eTo
      val res = ViewArrayBuffer(createArrayBufferFromArray(xs1), innerIso)
      res

    case ArrayBufferApply(Def(view: ViewArrayBuffer[a, b]), i) =>
      implicit val eA = view.innerIso.eFrom
      implicit val eB = view.innerIso.eTo
      val res = view.innerIso.to(view.source(i))
      res

    case ArrayBufferMap(xs, Def(IdentityLambda())) => xs
    case ArrayBufferRep(buf) => buf
    case ArrayBufferToArray(Def(ArrayBufferFromArray(arr))) => arr
    case ArrayBufferFromArray(Def(ArrayBufferToArray(buf))) => buf

    case _ =>
      super.rewriteDef(d)
  }

  abstract class ArrayBufferDef[T](implicit val eItem: Elem[T]) extends ArrayBuffer[T] with Def[ArrayBuffer[T]] {
    lazy val selfType = element[ArrayBuffer[T]]

    def apply(i: Rep[Int]): Rep[T] = ArrayBufferApply(this, i)
    def length : Rep[Int] = ArrayBufferLength(this)
    def mapBy[R: Elem](f: Rep[T => R]): Rep[ArrayBuffer[R]] = ArrayBufferMap(this, f)
    def update(i: Rep[Int], v: Rep[T]): Rep[ArrayBuffer[T]] = ArrayBufferUpdate(this, i, v)
    def insert(i: Rep[Int], v: Rep[T]): Rep[ArrayBuffer[T]] = ArrayBufferInsert(this, i, v)
    def +=(v: Rep[T]): Rep[ArrayBuffer[T]] = ArrayBufferAppend(this, v)
    def ++=(a: Arr[T]): Rep[ArrayBuffer[T]] = ArrayBufferAppendArray(this, a)
    def remove(i: Rep[Int], n: Rep[Int]): Rep[ArrayBuffer[T]] = ArrayBufferRemove(this, i, n)
    def reset():  Rep[ArrayBuffer[T]] = ArrayBufferReset(this)
    def toArray: Arr[T] = ArrayBufferToArray(this)
  }

  def emptyArrayBuffer[T: Elem]: Rep[ArrayBuffer[T]] = reflectMutable(ArrayBufferEmpty[T]())
  def initArrayBuffer[T: Elem](v: Rep[T]): Rep[ArrayBuffer[T]] = ArrayBufferFromElem(v)
  def makeArrayBuffer[T](name: Rep[String])(implicit e:Elem[T]): Rep[ArrayBuffer[T]] = MakeArrayBuffer(name)(e)
  def createArrayBuffer[T: Elem](count: Rep[Int], f:Rep[Int=>T]): Rep[ArrayBuffer[T]] = ArrayBufferUsingFunc(count, f)
  def createArrayBufferFromArray[T: Elem](arr: Arr[T]): Rep[ArrayBuffer[T]] = ArrayBufferFromArray(arr)

  case class ArrayBufferEmpty[T]()(implicit eItem: Elem[T]) extends ArrayBufferDef[T]()(concretizeElem(eItem).asElem[T]) {
    override def equals(other:Any) = {
      other match {
        case that:ArrayBufferEmpty[_] => (this.selfType equals that.selfType)
        case _ => false
      }
    }
  }

  case class MakeArrayBuffer[T](ctx: Rep[String])(implicit eItem: Elem[T])
     extends ArrayBufferDef[T]()(concretizeElem(eItem).asElem[T])

  case class ArrayBufferFromElem[T](v: Rep[T])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferUsingFunc[T](count: Rep[Int], f: Rep[Int=>T])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferApply[T](buf: Rep[ArrayBuffer[T]], i: Rep[Int])(implicit selfType: Elem[T]) extends BaseDef[T]

  case class ArrayBufferLength[T](buf: Rep[ArrayBuffer[T]])(implicit val eT: Elem[T]) extends BaseDef[Int]

  case class ArrayBufferMap[T, R](buf: Rep[ArrayBuffer[T]], f: Rep[T => R])(implicit val eT: Elem[T], val eR: Elem[R]) extends ArrayBufferDef[R]

  case class ArrayBufferUpdate[T](buf: Rep[ArrayBuffer[T]], i: Rep[Int], v: Rep[T])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferInsert[T](buf: Rep[ArrayBuffer[T]], i: Rep[Int], v: Rep[T])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferAppend[T](buf: Rep[ArrayBuffer[T]], v: Rep[T])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferAppendArray[T](buf: Rep[ArrayBuffer[T]], a: Arr[T])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferRemove[T](buf: Rep[ArrayBuffer[T]], i: Rep[Int], n: Rep[Int])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferReset[T](buf: Rep[ArrayBuffer[T]])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  case class ArrayBufferToArray[T](buf: Rep[ArrayBuffer[T]])(implicit eItem: Elem[T]) extends ArrayDef[T]

  case class ArrayBufferFromArray[T](arr: Rep[Array[T]]) extends ArrayBufferDef[T]()(arr.elem.eItem)

  case class ArrayBufferRep[T](buf: Rep[ArrayBuffer[T]])(implicit eItem: Elem[T]) extends ArrayBufferDef[T]

  implicit def resolveArrayBuffer[T: Elem](sym: Rep[ArrayBuffer[T]]): ArrayBuffer[T] = sym match  {
    case Def(d: ArrayBufferDef[_]) =>
      d.asInstanceOf[ArrayBuffer[T]]
    case s: Exp[_] =>
      val elem = s.elem
      elem match {
        case ae: ArrayBufferElem[_] => ArrayBufferRep(sym)(ae.asInstanceOf[ArrayBufferElem[T]].eItem)
        case _ =>
          !!!(s"Type mismatch: expected ArrayBufferElem but was $elem", sym)
      }
    case _ => ???("cannot resolve ArrayBuffer", sym)
  }

}
