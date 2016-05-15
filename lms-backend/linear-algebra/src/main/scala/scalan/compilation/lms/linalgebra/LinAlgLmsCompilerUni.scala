package scalan.compilation.lms.linalgebra

import scalan.compilation.lms.cxx.sharedptr.CxxCoreCodegen
import scalan.compilation.lms.scalac.ScalaCoreCodegen
import scalan.compilation.lms.uni.{LmsBackendUni, LmsCompilerUni}
import scalan.linalgebra.LADslExp
import scalan.{JNIExtractorOpsExp, ScalanDslExp}

class LinAlgLmsBackendUni extends LmsBackendUni with VectorOpsExp { self =>
  override val codegen = new ScalaCoreCodegen[self.type](self) with ScalaGenVectorOps
  override val nativeCodegen = new CxxCoreCodegen[self.type](self) with CxxShptrGenVectorOps
}

class LinAlgLmsCompilerUni[+ScalanCake <: ScalanDslExp with LADslExp with JNIExtractorOpsExp](_scalan: ScalanCake) extends LmsCompilerUni[ScalanCake](_scalan) with LinAlgLmsBridge {
  override val lms = new LinAlgLmsBackendUni
}
