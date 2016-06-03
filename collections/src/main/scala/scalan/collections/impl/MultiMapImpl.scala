package scalan.collections

import scalan._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait MultiMapsAbs extends scalan.ScalanDsl with MultiMaps {
  self: MultiMapsDsl =>

  // single proxy for each type family
  implicit def proxyMMultiMap[K, V](p: Rep[MMultiMap[K, V]]): MMultiMap[K, V] = {
    proxyOps[MMultiMap[K, V]](p)(scala.reflect.classTag[MMultiMap[K, V]])
  }

  // familyElem
  class MMultiMapElem[K, V, To <: MMultiMap[K, V]](implicit _elemKey: Elem[K], _elemValue: Elem[V])
    extends EntityElem[To] {
    def elemKey = _elemKey
    def elemValue = _elemValue
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("K" -> elemKey, "V" -> elemValue)
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagK = elemKey.tag
      implicit val tagV = elemValue.tag
      weakTypeTag[MMultiMap[K, V]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[MMultiMap[K, V]] => convertMMultiMap(x) }
      tryConvert(element[MMultiMap[K, V]], this, x, conv)
    }

    def convertMMultiMap(x: Rep[MMultiMap[K, V]]): Rep[To] = {
      x.selfType1 match {
        case _: MMultiMapElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have MMultiMapElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def mMultiMapElement[K, V](implicit elemKey: Elem[K], elemValue: Elem[V]): Elem[MMultiMap[K, V]] =
    cachedElem[MMultiMapElem[K, V, MMultiMap[K, V]]](elemKey, elemValue)

  implicit case object MMultiMapCompanionElem extends CompanionElem[MMultiMapCompanionAbs] {
    lazy val tag = weakTypeTag[MMultiMapCompanionAbs]
    protected def getDefaultRep = MMultiMap
  }

  abstract class MMultiMapCompanionAbs extends CompanionDef[MMultiMapCompanionAbs] with MMultiMapCompanion {
    def selfType = MMultiMapCompanionElem
    override def toString = "MMultiMap"
  }
  def MMultiMap: Rep[MMultiMapCompanionAbs]
  implicit def proxyMMultiMapCompanionAbs(p: Rep[MMultiMapCompanionAbs]): MMultiMapCompanionAbs =
    proxyOps[MMultiMapCompanionAbs](p)

  abstract class AbsHashMMultiMap[K, V]
      (map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V])
    extends HashMMultiMap[K, V](map) with Def[HashMMultiMap[K, V]] {
    lazy val selfType = element[HashMMultiMap[K, V]]
  }
  // elem for concrete class
  class HashMMultiMapElem[K, V](val iso: Iso[HashMMultiMapData[K, V], HashMMultiMap[K, V]])(implicit override val elemKey: Elem[K], override val elemValue: Elem[V])
    extends MMultiMapElem[K, V, HashMMultiMap[K, V]]
    with ConcreteElem[HashMMultiMapData[K, V], HashMMultiMap[K, V]] {
    override lazy val parent: Option[Elem[_]] = Some(mMultiMapElement(element[K], element[V]))
    override lazy val typeArgs = TypeArgs("K" -> elemKey, "V" -> elemValue)

    override def convertMMultiMap(x: Rep[MMultiMap[K, V]]) = HashMMultiMap(x.map)
    override def getDefaultRep = HashMMultiMap(element[MMap[K, ArrayBuffer[V]]].defaultRepValue)
    override lazy val tag = {
      implicit val tagK = elemKey.tag
      implicit val tagV = elemValue.tag
      weakTypeTag[HashMMultiMap[K, V]]
    }
  }

  // state representation type
  type HashMMultiMapData[K, V] = MMap[K, ArrayBuffer[V]]

  // 3) Iso for concrete class
  class HashMMultiMapIso[K, V](implicit elemKey: Elem[K], elemValue: Elem[V])
    extends EntityIso[HashMMultiMapData[K, V], HashMMultiMap[K, V]] with Def[HashMMultiMapIso[K, V]] {
    override def from(p: Rep[HashMMultiMap[K, V]]) =
      p.map
    override def to(p: Rep[MMap[K, ArrayBuffer[V]]]) = {
      val map = p
      HashMMultiMap(map)
    }
    lazy val eFrom = element[MMap[K, ArrayBuffer[V]]]
    lazy val eTo = new HashMMultiMapElem[K, V](self)
    lazy val selfType = new HashMMultiMapIsoElem[K, V](elemKey, elemValue)
    def productArity = 2
    def productElement(n: Int) = (elemKey, elemValue).productElement(n)
  }
  case class HashMMultiMapIsoElem[K, V](elemKey: Elem[K], elemValue: Elem[V]) extends Elem[HashMMultiMapIso[K, V]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new HashMMultiMapIso[K, V]()(elemKey, elemValue))
    lazy val tag = {
      implicit val tagK = elemKey.tag
      implicit val tagV = elemValue.tag
      weakTypeTag[HashMMultiMapIso[K, V]]
    }
    lazy val typeArgs = TypeArgs("K" -> elemKey, "V" -> elemValue)
  }
  // 4) constructor and deconstructor
  class HashMMultiMapCompanionAbs extends CompanionDef[HashMMultiMapCompanionAbs] with HashMMultiMapCompanion {
    def selfType = HashMMultiMapCompanionElem
    override def toString = "HashMMultiMap"

    @scalan.OverloadId("fromFields")
    def apply[K, V](map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V]): Rep[HashMMultiMap[K, V]] =
      mkHashMMultiMap(map)

    def unapply[K, V](p: Rep[MMultiMap[K, V]]) = unmkHashMMultiMap(p)
  }
  lazy val HashMMultiMapRep: Rep[HashMMultiMapCompanionAbs] = new HashMMultiMapCompanionAbs
  lazy val HashMMultiMap: HashMMultiMapCompanionAbs = proxyHashMMultiMapCompanion(HashMMultiMapRep)
  implicit def proxyHashMMultiMapCompanion(p: Rep[HashMMultiMapCompanionAbs]): HashMMultiMapCompanionAbs = {
    proxyOps[HashMMultiMapCompanionAbs](p)
  }

  implicit case object HashMMultiMapCompanionElem extends CompanionElem[HashMMultiMapCompanionAbs] {
    lazy val tag = weakTypeTag[HashMMultiMapCompanionAbs]
    protected def getDefaultRep = HashMMultiMap
  }

  implicit def proxyHashMMultiMap[K, V](p: Rep[HashMMultiMap[K, V]]): HashMMultiMap[K, V] =
    proxyOps[HashMMultiMap[K, V]](p)

  implicit class ExtendedHashMMultiMap[K, V](p: Rep[HashMMultiMap[K, V]])(implicit elemKey: Elem[K], elemValue: Elem[V]) {
    def toData: Rep[HashMMultiMapData[K, V]] = isoHashMMultiMap(elemKey, elemValue).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoHashMMultiMap[K, V](implicit elemKey: Elem[K], elemValue: Elem[V]): Iso[HashMMultiMapData[K, V], HashMMultiMap[K, V]] =
    reifyObject(new HashMMultiMapIso[K, V]()(elemKey, elemValue))

  // 6) smart constructor and deconstructor
  def mkHashMMultiMap[K, V](map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V]): Rep[HashMMultiMap[K, V]]
  def unmkHashMMultiMap[K, V](p: Rep[MMultiMap[K, V]]): Option[(Rep[MMap[K, ArrayBuffer[V]]])]

  registerModule(MultiMaps_Module)
}

// Std -----------------------------------
trait MultiMapsStd extends scalan.ScalanDslStd with MultiMapsDsl {
  self: MultiMapsDslStd =>
  lazy val MMultiMap: Rep[MMultiMapCompanionAbs] = new MMultiMapCompanionAbs {
  }

  case class StdHashMMultiMap[K, V]
      (override val map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V])
    extends AbsHashMMultiMap[K, V](map) {
  }

  def mkHashMMultiMap[K, V]
    (map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V]): Rep[HashMMultiMap[K, V]] =
    new StdHashMMultiMap[K, V](map)
  def unmkHashMMultiMap[K, V](p: Rep[MMultiMap[K, V]]) = p match {
    case p: HashMMultiMap[K, V] @unchecked =>
      Some((p.map))
    case _ => None
  }
}

// Exp -----------------------------------
trait MultiMapsExp extends scalan.ScalanDslExp with MultiMapsDsl {
  self: MultiMapsDslExp =>
  lazy val MMultiMap: Rep[MMultiMapCompanionAbs] = new MMultiMapCompanionAbs {
  }

  case class ExpHashMMultiMap[K, V]
      (override val map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V])
    extends AbsHashMMultiMap[K, V](map)

  object HashMMultiMapMethods {
    object union {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[MMultiMap[K, V]]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(that, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "union" =>
          Some((receiver, that)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[MMultiMap[K, V]]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[MMultiMap[K, V]]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object toMap {
      def unapply(d: Def[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "toMap" =>
          Some(receiver).asInstanceOf[Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object contains {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "contains" =>
          Some((receiver, key)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[K]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object apply {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "apply" =>
          Some((receiver, key)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[K]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object mapValueIfExistsBy {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V] => T], Rep[Unit => T]) forSome {type K; type V; type T}] = d match {
        case MethodCall(receiver, method, Seq(key, exists, otherwise, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "mapValueIfExistsBy" =>
          Some((receiver, key, exists, otherwise)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V] => T], Rep[Unit => T]) forSome {type K; type V; type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V] => T], Rep[Unit => T]) forSome {type K; type V; type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object add {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[V]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, value, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "add" =>
          Some((receiver, key, value)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[V]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[V]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object addAll {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V]]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, value, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "addAll" =>
          Some((receiver, key, value)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V]]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V]]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object reduceBy {
      def unapply(d: Def[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[Array[V] => T]) forSome {type K; type V; type T}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "reduceBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[HashMMultiMap[K, V]], Rep[Array[V] => T]) forSome {type K; type V; type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[HashMMultiMap[K, V]], Rep[Array[V] => T]) forSome {type K; type V; type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object keys {
      def unapply(d: Def[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "keys" =>
          Some(receiver).asInstanceOf[Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object values {
      def unapply(d: Def[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "values" =>
          Some(receiver).asInstanceOf[Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object toArray {
      def unapply(d: Def[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "toArray" =>
          Some(receiver).asInstanceOf[Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object size {
      def unapply(d: Def[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HashMMultiMapElem[_, _]] && method.getName == "size" =>
          Some(receiver).asInstanceOf[Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HashMMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object HashMMultiMapCompanionMethods {
    object empty {
      def unapply(d: Def[_]): Option[Unit forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem == HashMMultiMapCompanionElem && method.getName == "empty" =>
          Some(()).asInstanceOf[Option[Unit forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Unit forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object make {
      def unapply(d: Def[_]): Option[Rep[String] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(name, _*), _) if receiver.elem == HashMMultiMapCompanionElem && method.getName == "make" =>
          Some(name).asInstanceOf[Option[Rep[String] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[String] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object fromArray {
      def unapply(d: Def[_]): Option[Arr[(K, V)] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(arr, _*), _) if receiver.elem == HashMMultiMapCompanionElem && method.getName == "fromArray" =>
          Some(arr).asInstanceOf[Option[Arr[(K, V)] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Arr[(K, V)] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  def mkHashMMultiMap[K, V]
    (map: Rep[MMap[K, ArrayBuffer[V]]])(implicit elemKey: Elem[K], elemValue: Elem[V]): Rep[HashMMultiMap[K, V]] =
    new ExpHashMMultiMap[K, V](map)
  def unmkHashMMultiMap[K, V](p: Rep[MMultiMap[K, V]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: HashMMultiMapElem[K, V] @unchecked =>
      Some((p.asRep[HashMMultiMap[K, V]].map))
    case _ =>
      None
  }

  object MMultiMapMethods {
    object map {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "map" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object union {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[MMultiMap[K, V]]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(that, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "union" =>
          Some((receiver, that)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[MMultiMap[K, V]]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[MMultiMap[K, V]]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object isEmpty {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "isEmpty" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object contains {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "contains" =>
          Some((receiver, key)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[K]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object apply {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "apply" =>
          Some((receiver, key)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[K]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[K]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object mapValueIfExistsBy {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V] => T], Rep[Unit => T]) forSome {type K; type V; type T}] = d match {
        case MethodCall(receiver, method, Seq(key, exists, otherwise, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "mapValueIfExistsBy" =>
          Some((receiver, key, exists, otherwise)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V] => T], Rep[Unit => T]) forSome {type K; type V; type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V] => T], Rep[Unit => T]) forSome {type K; type V; type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object add {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[V]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, value, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "add" =>
          Some((receiver, key, value)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[V]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[V]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object addAll {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V]]) forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(key, value, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "addAll" =>
          Some((receiver, key, value)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V]]) forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[K], Rep[ArrayBuffer[V]]) forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object reduceBy {
      def unapply(d: Def[_]): Option[(Rep[MMultiMap[K, V]], Rep[Array[V] => T]) forSome {type K; type V; type T}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "reduceBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[MMultiMap[K, V]], Rep[Array[V] => T]) forSome {type K; type V; type T}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[MMultiMap[K, V]], Rep[Array[V] => T]) forSome {type K; type V; type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object keys {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "keys" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object values {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "values" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object toArray {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "toArray" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object size {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "size" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object toMap {
      def unapply(d: Def[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[MMultiMapElem[_, _, _]] && method.getName == "toMap" =>
          Some(receiver).asInstanceOf[Option[Rep[MMultiMap[K, V]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMultiMap[K, V]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object MMultiMapCompanionMethods {
    object empty {
      def unapply(d: Def[_]): Option[Unit forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem == MMultiMapCompanionElem && method.getName == "empty" =>
          Some(()).asInstanceOf[Option[Unit forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Unit forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object make {
      def unapply(d: Def[_]): Option[Rep[String] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(name, _*), _) if receiver.elem == MMultiMapCompanionElem && method.getName == "make" =>
          Some(name).asInstanceOf[Option[Rep[String] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[String] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object fromArray {
      def unapply(d: Def[_]): Option[Arr[(K, V)] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == MMultiMapCompanionElem && method.getName == "fromArray" =>
          Some(a).asInstanceOf[Option[Arr[(K, V)] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Arr[(K, V)] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object fromMap {
      def unapply(d: Def[_]): Option[Rep[MMap[K, ArrayBuffer[V]]] forSome {type K; type V}] = d match {
        case MethodCall(receiver, method, Seq(map, _*), _) if receiver.elem == MMultiMapCompanionElem && method.getName == "fromMap" =>
          Some(map).asInstanceOf[Option[Rep[MMap[K, ArrayBuffer[V]]] forSome {type K; type V}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[MMap[K, ArrayBuffer[V]]] forSome {type K; type V}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object MultiMaps_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVWS2wbRRietZNu/CCEAOUhgUPkAuJhl/RQpAhVaeIArZ1E2T6QqUDj3bGzZfbBzjjscigShx6AE0IckDhUAnGpkBC3HhAHKlUIIcGVM6dShHqgJxD/zD6863hTHmIPo3n88z++//v/ncs30DTz0KNMxxTbDYtw3NDkfIXxutayucmDjmMMKVkj/R+e/Nopv/3eRgHNddGBHczWGO2iUjhp+W4y17jRRiVs64Rxx2McPdKWFpq6QynRuenYTdOyhhz3KGm2TcaX22iq5xjB6+gCUtpoTnds3SOcaKsUM0ZYtD9DhEdmsi7JdbDpjmzYTRFFMxXFKQ+bHNwHG3Oh/DZxtcB27MDiaDZybdMVboFMhfguxPCi5VJppthGqmm5jsdjqypY2HGMeDllY9hA8+3zeBc3weqgqXHPtAdCmYv11/CAbICIEJ+CGBih/VOBSyLlFcaNjD3fRQhBVpakY40RZo0Es4bArK4Rz8TUfBOLwy3P8QMUfkoRId8FFU/dRkWsgbRso/7OOf3lW1rFKojLvnBFlQ4dAEW1HIbI9AC217bfZzefv3S0gMpdVDbZSo9xD+s8TYMIrgq2bYdLnxMEsTeADC7mZVBaWQGZMZqUdMdysQ2aIiyrkChq6iYXwmKvGqUnB3uVuyQWVXxXSeJdyIlXcmkVU7p1/YGnD/3SeqmAClkTJVCpQTF4sVKOSp3OkHKzg91Ivxjv5Eg5OQJZLM/IpRhK/mhU93EnAeax678aVw+jc4UEzsj638sgqJh/9qMrh8jW5wU005WEX6d4IHMp8FojTO+iGWeXeOG+uoupmE3Mp2qQPoaQI5TT8BQBHo4WcmvVJQK7ZVkDShx+JaTxhmOT+vpW/Xft2w8uC5Z6qBqehMX7p3n0j59m+1wSmKOiFQEO2Bah5LPgT3Vul4/Kiufh4Piw3yfemODkTI1nrRw6pzkWuWvxpvnKpXe5zI/iZ7vFZu88VOeyvPfwPqmKG9kXFy/e+9snr94ti22mZ3IItH74H5RaXBn/YymhLGKzq1FDl3Rbyh7e8QJmO0mNpKCNBeaTw9W0l2oabjHel+zKoQYkOJjRnbleS11MOfSgEjNPCnGkEkqskyRImNOC9UTmpLzeo6UktJzBdEj207OXWJPgqSVV/VAeVWQyDm6376E3jn1VQNMn0HQfypW10XTPGdpGnGX40XLi8+PxnpLNMmQVe9hKsiq/BTSCL1s1mxMF9gaVivqZMaYUgeDZnf/WPnP4IdeNiR5UBKvXsWXSYCnPk5xOnUPCHP6GALnjemspo5MB/ReIi/H0SHsDuNPI4c4a0Sn2iCFeJMSCF1PYQI58eOzsifvPnpYtrGpIofAk6eeT33cQ1rJ8jTy+z2sEhOoty4XXJkyOfPPcj29999mnspGP4BRAxUwAUCP3R+pYEtpiTmha1LOAYxdufbzxxPdf/ix/4GXR/eDvYidvvFER+GO0qyY+wKsthTMUtGiJKWLJ4Y2/AJuqMKJkCwAA"
}
}

