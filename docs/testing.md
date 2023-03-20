# Testing

## ScalaTest

Mature library with
- a choice of testing styles
- a choice of DSLs

Examples

- [FlatSpec using asserts](/src/test/scala/syncs/testing/scalatest/ScalaTestFlatAssertsSpec.scala)
- [FlatSpec using Matchers](/src/test/scala/syncs/testing/scalatest/ScalaTestFlatAssertsSpec.scala)


## Munit

A newer library with
- no testing DSL
- enhanced output
- built in support for Future

Examples

- [Basic](/src/test/scala/syncs/testing/munit/MunitBasicExampleSpec.scala)
- [Extended](/src/test/scala/syncs/testing/munit/MunitExtendedExampleSpec.scala)
- [Futures](/src/test/scala/syncs/testing/munit/MunitFuturesExampleSpec.scala)
- [Integration with Cats Effect IO](/src/test/scala/syncs/testing/munit/MunitCatsEffectExampleSpec.scala)
