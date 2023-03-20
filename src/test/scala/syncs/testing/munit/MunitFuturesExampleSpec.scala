package syncs.testing.munit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MunitFuturesExampleSpec extends munit.FunSuite {

  private val one = 1
  private val two = 2

  test("Success in the future") {
    Future {
      assert(one == one)
    }
  }

  test("Cannot work with Futures directly in assertions. Should pass but fails") {
    assert(Future(one) == Future.successful(one))
  }

  test("Fail equality in the future") {
    Future {
      assert(one == two)
    }
  }

  test("Success in the mapped future") {
    Future {
      one
    }.map { x =>
      assert(x == one)
    }
  }

  test("Fail equality in the futures with clues") {
    Future {
      one
    }.map(x => assert(clue(x) == clue(two)))
  }
}
