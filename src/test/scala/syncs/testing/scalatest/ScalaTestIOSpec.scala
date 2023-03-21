package syncs.testing.scalatest

import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues}

class ScalaTestIOSpec extends AnyFlatSpec
  with Matchers {

  val list: IO[List[Int]] = IO(List(1, 4, 6, 8))

  "A list with 4 elements when evaluated" should "have size 4" in {
    list.unsafeRunSync().length shouldBe 4
  }

  "A list mapped to its length when evaluated" should "have size 4" in {
    list.map(_.length).unsafeRunSync() shouldBe 4
  }


  "A list when evaluated" should "fail if we say list should be empty" in {
    list.unsafeRunSync() shouldBe empty
  }

}
