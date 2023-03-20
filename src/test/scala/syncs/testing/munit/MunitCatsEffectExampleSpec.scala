package syncs.testing.munit

import cats.effect.IO
import munit.CatsEffectSuite


class MunitCatsEffectExampleSpec extends CatsEffectSuite {

  private val one = 1
  private val two = 2

  test("Success in the io") {
    IO {
      assert(one == one)
    }
  }

  test("Cannot work with ios directly in asserts. Should pass but fails") {
    assert(IO(one) == IO.pure(one))
  }

  test("Can work with ios directly in assetIOs. Passes") {
    assertIO(IO(one), one)
  }

  test("Can work with ios directly in assetIOs. Fails") {
    assertIO(IO(one), two)
  }

  test("can assertEqual") {
    IO(one).assertEquals(two)
  }

  test("Fail equality in the io") {
    IO {
      assert(one == two)
    }
  }

  test("Success in the mapped io") {
    IO {
      one
    }.map { x =>
      assert(x == one)
    }
  }

  test("Fail equality in the ios with clues") {
    IO {
      one
    }.map(x => assert(clue(x) == clue(two)))
  }
}
