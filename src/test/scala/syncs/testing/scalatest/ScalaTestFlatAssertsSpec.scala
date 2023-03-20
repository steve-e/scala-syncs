package syncs.testing.scalatest

import org.scalatest.flatspec.AnyFlatSpec

class ScalaTestFlatAssertsSpec extends AnyFlatSpec {

  val list: List[Int] = List(1, 4, 6, 8)

  "A list with 4 elements" should "have size 4" in {
    assert(list.length == 4)
  }

  it should "fail if we assert it is empty" in {
    assert(list.isEmpty)
  }

  "expression" should "fail if we expect the wrong answer" in {
    assert(1 + 2 == 4)
  }

  "expression" should "fail if we expect the wrong answer, with clue" in {
    assert(1 + 2 == 4, "expected to add to 4")
  }

  "assertResult expression" should "fail if we expect the wrong answer" in {
    assertResult(4)(1 + 2)
  }

  "assertResult expression" should "fail if we expect the wrong answer with clue" in {
    assertResult(4, "The answer is meant to be four")(1 + 2)
  }

  "simple code" should "compile" in {
    assertCompiles("val x:Int = 3")
  }

  "simple code" should "compile - fails" in {
    assertCompiles("val x:String = 3")
  }

  "simple code" should "not compile" in {
    assertDoesNotCompile("val x:String = 3")
  }

  "simple code" should "not compile fails" in {
    assertDoesNotCompile("val x:Int = 3")
  }

}
