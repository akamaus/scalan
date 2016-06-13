package scalan

import org.scalactic.TripleEquals
import org.scalatest._
import org.scalatest.words.ResultOfStringPassedToVerb
import org.scalatest.{FlatSpec, Inside, Matchers}

import scalan.util.FileUtil

trait TestsUtil extends Suite with Matchers with Inside with TripleEquals {
  def testOutDir = "test-out"

  def testSuffixes = Seq("Suite", "Tests", "It", "_")

  lazy val prefix = {
    val suiteName = testSuffixes.foldLeft(getClass.getName)(_.stripSuffix(_))
    val pathComponents = suiteName.split('.')
    FileUtil.file(testOutDir, pathComponents: _*)
  }

  /* if runs in continuous integration environment */
  def isCI = sys.env.get("CI").flatMap(toBoolean).getOrElse(false)
  private def toBoolean(s: String): Option[Boolean] =
    scala.util.Try(s.toBoolean).toOption
  def pendingOnCI(): Unit = if (isCI) { pending }

  private val _currentTestName = new ThreadLocal[String]

  override def withFixture(test: NoArgTest) = {
    _currentTestName.set(test.name)
    val outcome = super.withFixture(test)
    _currentTestName.set(null)
    outcome
  }

  protected def currentTestName: String = {
    val testName = _currentTestName.get()
    assert(testName != null, "currentTestName called outside a test")
    testName
  }

  protected def currentTestNameAsFileName: String = FileUtil.cleanFileName(currentTestName)
}

/**
 * Standard base class for most test suites. See BaseNestedTests and BaseShouldTests for alternatives
 *
 * See <a>http://doc.scalatest.org/2.2.4/#org.scalatest.FunSuite</a>.
 */
abstract class BaseTests extends FunSuite with TestsUtil

/**
 * Standard base class for test suites with nested tests.
 *
 * See <a>http://doc.scalatest.org/2.2.4/#org.scalatest.FunSpec</a>.
 */
abstract class BaseNestedTests extends FunSpec with TestsUtil

/**
 * See <a>http://doc.scalatest.org/2.2.4/#org.scalatest.FlatSpec</a>.
 */
abstract class BaseShouldTests extends FlatSpec with TestsUtil {
  protected final class InAndIgnoreMethods2(resultOfStringPassedToVerb: ResultOfStringPassedToVerb) {

    import resultOfStringPassedToVerb.rest
    val _inner = new InAndIgnoreMethods(resultOfStringPassedToVerb)
    /**
     * Supports the registration of tests in shorthand form.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "A Stack" must "pop values in last-in-first-out order" in { ... }
     *                                                        ^
     * </pre>
     *
     * <p>
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait <code>FlatSpec</code>.
     * </p>
     */
    def beArgFor(testFun: String => Unit) {
      _inner.in(testFun(rest.trim))
    }
  }

  protected implicit def convertToInAndIgnoreMethods2(resultOfStringPassedToVerb: ResultOfStringPassedToVerb) =
    new InAndIgnoreMethods2(resultOfStringPassedToVerb)

}
