package syncs.testing.scalatest

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScalaTestFlatMatchersSpec extends AnyFlatSpec with Matchers {

  val list: List[Int] = List(1, 4, 6, 8)

  "A list with 4 elements" should "have size 4" in {
    list.length shouldBe 4
  }

  it should "fail if we say length shouldBe 0" in {
    list.length shouldBe 0
  }

  it should "fail if we say length shouldEqual 0" in {
    list.length shouldEqual 0
  }

  it should "fail if we say length should equal 0" in {
    list.length should equal (0)
  }

  it should "fail if we say list should have size 0" in {
    list should have size 0
  }

  it should "fail if we say list have isEmpty true" in {
    list should have('isEmpty(true))
  }

  it should "fail if we say list.isEmpty shouldBE true" in {
    list.isEmpty shouldBe true
  }

  it should "fail if we say list should be empty" in {
    list shouldBe empty
  }

  "bad code" should "compile, fails" in {
    """val x:String = false""" should compile
  }

  "correct code" should "not compile, fails" in {
    """val x:Boolean = false""" shouldNot compile
  }
}
