package syncs.errors

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Try

class SimpleErrorHandlingTest extends AnyFlatSpec with Matchers {

  object Evaluation {
    lazy val lazyTime: Long = System.currentTimeMillis()

    val eagerTime: Long = System.currentTimeMillis()

    def methodTime: Long = System.currentTimeMillis()
  }

  /* Don't write code like this!!
   * However you may find external libraries behave like this.
   */
  object BadApi {



    def getStringOrNull(x: Int): String = {
      if (x < 0) null
      else if (x == 1) "one"
      else if (x == 2) "two"
      else null
    }

    def getBooleanOrThrow(x: Int): Boolean = {
      if (x < 0) throw new IllegalArgumentException("Less than zero")
      else if (x == 1) true
      else if (x == 2) false
      else throw new NotImplementedError("This mapping is not implemented yet")
    }
  }


  "evaluations" should "be interesting" in {

    import Evaluation._
    val eager = eagerTime
    eagerTime shouldBe eagerTime
    eagerTime shouldBe eager

    val lazyT = lazyTime
    lazyTime shouldBe lazyTime
    Thread.sleep(100)

    lazyTime shouldBe lazyT

    lazyTime should be > eagerTime

    val mt = methodTime
    Thread.sleep(100)
    methodTime should be > mt

  }

  "parameters" should "be interesting" in {

    // normal by value methods have the parameter evaluated before the method is called
    def byValue(x:Int): Int = x * 2

    // by name only evaluates parameter when it is referenced, here in the try block
    def byName(x : => Int): Int = try x * 2 catch {case _: Exception => -1}

    val a: Int = 2
    lazy val b: Int = throw new Exception()

    byValue(a) shouldBe 4
    /*
    This would throw an exception
    byValue(b) shouldBe 4
    */

    byName(b) shouldBe -1

  }

  "getStringOrNull" should "return a string" in {
    BadApi.getStringOrNull(1) shouldBe "one"
  }

  "getStringOrNull" should "return null" in {
    val str = BadApi.getStringOrNull(0)
    str shouldBe null

    a[NullPointerException] shouldBe thrownBy {
      // oh no! I have a variable that I can't use!
      str.isEmpty
    }
  }

  "getBooleanActionOrThrow" should "return a boolean for a valid input" in {
    BadApi.getBooleanOrThrow(1) shouldBe true
  }

  "getBooleanActionOrThrow" should "throw an exception if input is less than zero" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      // Oh no! I ask a simple question and my program blows up
      BadApi.getBooleanOrThrow(-1)
    }
  }


  /* Make BadApi easier to use. We return an Option instead of null or throwing an exception.
   * Return Some if there is a result. Return None otherwise
   */
  object OptionWrapper {
    def getString(x: Int): Option[String] = Option(BadApi.getStringOrNull(x))

    def getBoolean(x: Int): Option[Boolean] = Try(BadApi.getBooleanOrThrow(x)).toOption
  }

  "getString" should "return some string for valid input " in {
    val maybeString = OptionWrapper.getString(1)
    maybeString shouldBe Some("one")
    maybeString.getOrElse("unknown") shouldBe "one"

  }

  "getString" should "return None for invalid input" in {
    val maybeString = OptionWrapper.getString(-1)
    maybeString shouldBe None
    maybeString shouldBe empty
    maybeString.getOrElse("unknown") shouldBe "unknown"
  }


  "getBoolean" should "return a boolean for a valid input" in {
    val maybeBoolean = OptionWrapper.getBoolean(1)
    maybeBoolean shouldBe Some(true)
    maybeBoolean should contain(true)
    maybeBoolean.contains(true) shouldBe true
  }

  "getBoolean" should "return null if input is less than zero" in {
    val maybeBoolean = OptionWrapper.getBoolean(-1)
    maybeBoolean shouldBe empty
    maybeBoolean.contains(true) shouldBe false
  }

  /* Make BadApi easier to use. We return an Either instead of null or throwing an exception.
   * Return Right if there is a result. Return Left with an error value otherwise/
   */
  object EitherWrapper {
    def getString(x: Int): Either[String, String] = Option(BadApi.getStringOrNull(x)).toRight(left = "No String available")

    // scala 1.12 api.
    def getBoolean(x: Int): Either[String, Boolean] = Try(BadApi.getBooleanOrThrow(x)).toEither.left.map(_ => "No Boolean available")
  }

  "getString" should "return Right string for valid input " in {
    val eitherString = EitherWrapper.getString(1)
    eitherString shouldBe Right("one")
    eitherString.getOrElse("unknown") shouldBe "one"

  }

  "getString" should "return Left for invalid input" in {
    val eitherString = EitherWrapper.getString(-1)
    eitherString.isLeft shouldBe true
    eitherString shouldBe Left("No String available")
    eitherString.getOrElse("unknown") shouldBe "unknown"
  }


  "getBoolean" should "return a Right boolean for a valid input" in {
    val stringOrBoolean = EitherWrapper.getBoolean(1)
    stringOrBoolean shouldBe Right(true)
    stringOrBoolean.contains(true) shouldBe true
    stringOrBoolean.getOrElse(false) shouldBe true
  }

  "getBoolean" should "return Left if input is less than zero" in {
    val stringOrBoolean = EitherWrapper.getBoolean(-1)
    stringOrBoolean shouldBe Left("No Boolean available")
    stringOrBoolean.contains(true) shouldBe false
    stringOrBoolean.getOrElse(false) shouldBe false
  }

  "create Try" should "be lazy" in {

    def x = throw new Exception("")

    def y(v: Int) = v

    val t = Try[Int](x)
    val t2 = Try(y(3))

    t.isFailure shouldBe true
    t2.isSuccess shouldBe true
    t2.getOrElse(0) shouldBe 3
    t.getOrElse(0) shouldBe 0

    Try({
      val x = 3
      val y = 4
      x + y
    }).getOrElse(0) shouldBe 7
  }
}
