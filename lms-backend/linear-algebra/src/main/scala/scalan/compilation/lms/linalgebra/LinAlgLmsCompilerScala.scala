package scalan.compilation.lms.linalgebra

import scalan.compilation.lms.ScalaCoreLmsBackend
import scalan.compilation.lms.scalac.{ScalaCoreCodegen, LmsCompilerScala}
import scalan.linalgebra.LADslExp

class ScalaLinAlgLmsBackend extends ScalaCoreLmsBackend with VectorOpsExp { self =>
  override val codegen = new ScalaCoreCodegen[self.type](self) with ScalaGenVectorOps
}

class LinAlgLmsCompilerScala[+ScalanCake <: LADslExp](_scalan: ScalanCake) extends LmsCompilerScala(_scalan) with LinAlgLmsBridgeScala {
  override val lms = new ScalaLinAlgLmsBackend
}
