# Testing

## Some documentation

### [Test Infected: Programmers Love Writing Tests](https://citeseerx.ist.psu.edu/document?repid=rep1&type=pdf&doi=14f22a35fdc5f919175438c7caa7bd75d7cb9ea3)

Classic introduction to Test Driven Development.
See also Kent Beck's book [Test Driven Development](https://www.amazon.co.uk/gp/product/B095SQ9WP4)
An alternative (top-down) approach is given by [Growing Object-Oriented Software, Guided by Tests](https://www.amazon.co.uk/Growing-Object-Oriented-Software-Addison-Wesley-Signature-ebook/dp/B002TIOYVW)

### [Writing tests for Datasets Platform pipelines](https://data.mpi-internal.com/150-products/100-list/datasets-platform/020-onboarding/050-create-a-pipeline/writing-pipeline-tests/)

Documentation on the Datasets Platform test facilities (not internet accessible!)

## Some scala test Frameworks    

### ScalaTest

Mature library with
- a choice of testing styles
- a choice of DSLs

Examples

- [FlatSpec using asserts](../src/test/scala/syncs/testing/scalatest/ScalaTestFlatAssertsSpec.scala)
- [FlatSpec using Matchers](../src/test/scala/syncs/testing/scalatest/ScalaTestFlatMatchersSpec.scala)
- [FlatSpec testing IO](../src/test/scala/syncs/testing/scalatest/ScalaTestIOSpec.scala) 
  Later versions of ScalaTest can have better integration with cats effect


### Munit

A newer library with
- no testing DSL
- enhanced output
- built in support for Future

Examples

- [Basic](../src/test/scala/syncs/testing/munit/MunitBasicExampleSpec.scala)
- [Extended](../src/test/scala/syncs/testing/munit/MunitExtendedExampleSpec.scala)
- [Futures](../src/test/scala/syncs/testing/munit/MunitFuturesExampleSpec.scala)
- [Integration with Cats Effect IO](../src/test/scala/syncs/testing/munit/MunitCatsEffectExampleSpec.scala)

## Test driven development example
    Develop a list

    Maybe look at a spark test?
