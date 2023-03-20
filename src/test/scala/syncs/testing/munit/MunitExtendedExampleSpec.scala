package syncs.testing.munit

class MunitExtendedExampleSpec extends munit.FunSuite {

  private val one = 1
  private val two = 2

  test("Success assertEquals") {
    assertEquals(one, one)
  }
  test("assertEquals fails") {
    assertEquals(one, two)
  }

  test("assertNotEquals fails") {
    assertNotEquals(one, one)
  }

  test("assertNoDiff fails") {
    val expected =
      """
        |This
        |that
        |the other
        |""".stripMargin
    val actual =
      """
        |This
        |that,
        |the other
        |""".stripMargin

    assertNoDiff(expected, actual)
  }

  test("intercept succeeds") {
    intercept[Exception]{
      throw new RuntimeException()
    }
  }

  test("intercept fails") {
    intercept[RuntimeException]{
      throw new Exception()
    }
  }

  test("intercept interceptMessage succeeds") {
    interceptMessage[Exception]("the actual message"){
      throw new Exception("the actual message")
    }
  }

  test("intercept interceptMessage fails") {
    interceptMessage[Exception]("Not the actual message"){
      throw new Exception("the actual message")
    }
  }

  test("assertNoDiff compileErrors against empty string succeeds") {
    assertNoDiff(compileErrors("val x:Int = 3"), "")
  }

  test("assertNoDiff compileErrors against empty string  fails") {
    assertNoDiff(compileErrors("val x:String = 3"),"")
  }
}
