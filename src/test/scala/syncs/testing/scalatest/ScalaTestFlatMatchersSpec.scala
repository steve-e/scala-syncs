package syncs.testing.scalatest

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues}

class ScalaTestFlatMatchersSpec extends AnyFlatSpec
  with Matchers
  with OptionValues
  with LoneElement {

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

  it should "fail if we say list have foo true" in {
    list should have('foo(true))
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

  "OptionValues" should "get the value" in {
    val option: Option[Int] = Some(3)
    option.value shouldBe 3
  }

  "OptionValues" should "fail on the wrong value" in {
    val option: Option[Int] = Some(3)
    option.value shouldBe 666
  }

  "OptionValues" should "fail on empty" in {
    val option: Option[Int] = None
    option.value shouldBe 3
  }

  "loneElement" should "get the only element" in {
    List(3).loneElement shouldBe 3
  }

  "loneElement" should "fail on empty" in {
    List.empty[Int].loneElement shouldBe 3
  }

  "loneElement" should "fail on non singleton list" in {
    List(1, 2).loneElement shouldBe 3
  }
}
