package syncs.testing.munit

class MunitBasicExampleSpec extends munit.FunSuite {

  private val one = 1
  private val two = 2

  test("Success") {
    assert(one == one)
  }

  test("Fail equality") {
    assert(one == two)
  }

  test("Fail equality, with clue text") {
    assert(one == two, "one should be 2")
  }

  test("Fail equality, with clues") {
    assert(clue(one) == clue(two))
  }

  test("Fail equality, calculation with clues") {
    assert(clue(one * one) == clue(two))
  }

}
