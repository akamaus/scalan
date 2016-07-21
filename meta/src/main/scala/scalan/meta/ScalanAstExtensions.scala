package scalan.meta
/**
 * Created by slesarenko on 23/02/15.
 */
import ScalanAst._
import PrintExtensions._

trait ScalanAstExtensions {
  implicit class SMethodOrClassArgsOps(as: SMethodOrClassArgs) {
    def argNames = as.args.map(a => a.name)
    def argNamesAndTypes(config: CodegenConfig) = {
      if (config.isAlreadyRep)
        as.args.map(a => s"${a.name}: ${a.tpe}")
      else
        as.args.map(a => s"${a.name}: Rep[${a.tpe}]")
    }

    def argUnrepTypes(module: SEntityModuleDef, config: CodegenConfig) = {
      if (config.isAlreadyRep) {
        as.args.map { a =>
          val tpe = a.tpe
          tpe.unRep(module, config) match {
            case Some(unrepped) => (unrepped, true)
            case None => (tpe, false)
          }
        }
      } else
        as.args.map(a => (a.tpe, true))
    }
  }

  implicit class STpeArgsOps(args: STpeArgs) {
    def decls = args.map(_.declaration)
    def names = args.map(_.name)

    def declString = decls.asTypeParams()
    def useString = names.asTypeParams()

    def getBoundedTpeArgString(withTags: Boolean = false) =
      args.asTypeParams { t =>
        (if (t.isHighKind) s"${t.declaration}:Cont" else s"${t.name}:Elem") + withTags.opt(":WeakTypeTag")
      }
  }

  implicit class STpeDefOps(td: STpeDef) {
    def declaration = s"type ${td.name}${td.tpeArgs.declString} = Rep[${td.rhs}}]"
  }

  implicit class SMethodDefOps(md: SMethodDef) {
    def explicitReturnType = md.tpeRes.getOrElse(throw new IllegalStateException(s"Explicit return type required for method $this"))
    def declaration(config: CodegenConfig, includeOverride: Boolean) = {
      val typesDecl = md.tpeArgs.getBoundedTpeArgString(false)
      val argss = md.argSections.rep(sec => s"(${sec.argNamesAndTypes(config).rep()})", "")
      s"${includeOverride.opt("override ")}def ${md.name}$typesDecl$argss: $explicitReturnType"
    }
  }
}
