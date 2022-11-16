package syncs.two20221116

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CaseObjectsTest extends AnyFlatSpec  with Matchers {
  object Foo
  case object Bar

  case class FooBar(baz:String, x:Int)

  "object" should "have ugly default toString" in {
    Foo.toString should startWith ("syncs.two20221116.CaseObjectsTest$Foo$@")
  }

  "case object" should "have nice to string" in {
    Bar.toString shouldBe "Bar"
  }

  "case class" should "have nice to string" in {
    FooBar("bloop", 23).toString shouldBe "FooBar(bloop,23)"
  }

  "class" should "have ugly default toString" in {
    class Fred(name:String = "Smith", x:Int = 3) {}
    val fred = new Fred()
    fred.toString should include ("$Fred$")
    fred.toString should include ("@")
  }

  "case object" should "be a product with arity 0" in {
    Bar shouldBe a[Product]
    Bar.productArity shouldBe 0
  }

  "object" should "not be a product" in {
    Foo should not be a[Product]
  }

  "case class" should "be a product with arity the same size as parameter list" in {
    val fooBar = FooBar("bloop", 23)
    fooBar shouldBe a[Product]
    fooBar.productArity shouldBe 2
    // This is doable, but not very nice as this list is type List[Any]
    fooBar.productIterator.toList shouldBe List("bloop", 23)
  }

  "Some ways to make a 2 element case class instance from a compatible tuple" should "" in {
    //(1,2,"three").productIterator.toList
    val t = ("string",2)

    val expected = FooBar("string",2)

    FooBar(t._1, t._2) shouldBe expected

    (t match {
      case (x,y) => FooBar(x,y)
      case _ => FooBar("Default", -1)
    }) shouldBe expected

    val (x,y) = t // Less common and potentially less safe
    FooBar(x,y) shouldBe expected
  }


}
