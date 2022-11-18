package syncs.one

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CaseClassTest extends AnyFlatSpec  with Matchers{

  val someOpt: Option[String] = Some("hi")
  val noneOpt: Option[String] = None
  val defaultString: String = "goodbye"
  val otherString = "carlos"


  "Some.getOrElse" should "get value" in {
    someOpt.getOrElse(defaultString) shouldBe "hi"
  }

  "None.getOrElse" should "get default" in {
    noneOpt.getOrElse(defaultString) shouldBe defaultString
  }

  "Some.map" should "act on value" in {
    someOpt.map(x => x.toUpperCase) shouldBe Some("HI")
  }

  "None.map" should "be a no-op" in {
    noneOpt.map(x => x.toUpperCase) shouldBe None
  }

  "Some.fold" should "operate on the value" in {
    someOpt.fold(defaultString) {
      x => s"$x $otherString"
    } shouldBe "hi carlos"
  }
  "None.fold" should "return the default" in {
    noneOpt.fold(ifEmpty = defaultString) {
      x => x + otherString
    } shouldBe defaultString
  }

  "match" should "be able to deconstruct a Some" in {
    val result = someOpt match {
      case Some(x) => x
      case None => "I don't have a string"
    }
    result shouldBe "hi"
  }

  "match" should "be able to match a None" in {
    val result = noneOpt match {
      case Some(x) => x
      case None => "I don't have a string"
    }
    result shouldBe "I don't have a string"
  }

  def defaultLastName: String = "Silva"

  case class Person(firstName: String, lastName: String = defaultLastName)

  val person1: Person = Person("Carlos", "Guevara")
  val person2: Person = Person("Carlos")

  "match" should "deconstruct a case class and match a value" in {
    val greeting =  person2 match {
      case Person(_, "Silva") => "hello Mr. Silva"
      case _ => "Hello you"
    }
    greeting shouldBe "hello Mr. Silva"
  }

  "match" should "handle defaults for unmatched cases" in {
    val greeting =  person1 match {
      case Person(_, "Silva") => "hello Mr. Silva"
      case _ => "Hello you"
    }
    greeting shouldBe "Hello you"
  }

}
