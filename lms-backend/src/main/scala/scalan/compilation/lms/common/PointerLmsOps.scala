package scalan.compilation.lms.common

import scala.virtualization.lms.common._
import scala.virtualization.lms.epfl.test7.ArrayLoopsExp
import scala.virtualization.lms.internal.GenerationFailedException
import scalan.compilation.lms.cxx.sharedptr.CxxShptrCodegen

trait PointerLmsOps extends Base {
  trait Scalar[A]
  trait Pointer[A]
}

trait PointerLmsOpsExp
  extends PointerLmsOps
  with LoopsFatExp
  with ArrayLoopsExp
  with IfThenElseExp
  with EqualExpBridge
  with FunctionsExp
  with BaseExp {

  // note: m: Manifest[A] is needed to distinct CreateScalar[Int](0) and CreateScalar[Double](0.0), when 0 == 0.0
  case class CreateScalar[A](source: Exp[A], m: Manifest[A]) extends Def[Scalar[A]]
  def createScalar[A: Manifest](source: Exp[A]): Exp[Scalar[A]] = CreateScalar(source, manifest[A])

  case class NullPtr[A](m: Manifest[A]) extends Def[Pointer[A]]
  def nullPtr[A: Manifest]: Exp[Pointer[A]] = NullPtr(manifest[A])

  case class PtrScalar[A: Manifest](xScalar: Exp[Scalar[A]]) extends Def[Pointer[A]]
  def ptrScalar[A: Manifest](xScalar: Exp[Scalar[A]]): Exp[Pointer[A]] = PtrScalar(xScalar)
}

trait CxxShptrGenPointer extends CxxShptrCodegen {
  val IR: PointerLmsOpsExp
  import IR._

  def !!!(s: String) = throw new GenerationFailedException(s)

  override def remap[A](m: Manifest[A]): String = {
    def standartRemap[A](m: Manifest[A]) = remap(m.typeArguments(0))
    m.runtimeClass match {
      case c if c == classOf[Scalar[_]] => standartRemap(m)
      case c if c == classOf[Pointer[_]] => s"${remap(m.typeArguments(0))}*"
      case _ =>
        super.remap(m)
    }
  }

  override def wrapSharedPtr: PartialFunction[Manifest[_],Manifest[_]] = {
    case m if m.runtimeClass == classOf[Scalar[_]] => m
    case m if m.runtimeClass == classOf[Pointer[_]] => m
    case m => super.wrapSharedPtr(m)
  }

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case CreateScalar(x, _) =>
      emitValDef(sym, quote(x))

    case NullPtr(_) =>
      emitValDef(sym, "NULL")

    case _ =>
      super.emitNode(sym, rhs)
  }
}
