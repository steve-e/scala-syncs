# Scala Syncs
This repo holds some code examples from the scala mentoring sessions, and related information.

The Scala Syncs were knowlege sharing sessions to eperience data engineers that were new to scala.

Earlier meetings were based around ScalaTests showcasing particular features and techniques.
Later on the meetings were based around markdown pages generated with mdoc
## Build Docs
Build the docs by running
```bash
sbt docs/mdoc
```
## Sync example code
- [CaseClassTest](src/test/scala/syncs/one/CaseClassTest.scala)
- [CaseObjectsTest](src/test/scala/syncs/two20221116/CaseObjectsTest.scala)
- [SimpleErrorHandlingTest](src/test/scala/syncs/errors/SimpleErrorHandlingTest.scala)
- [CatsIntroTest](src/test/scala/syncs/catsintro/CatsIntroTest.scala)

## Sync Docs

### Docs with embedded scala execution
- [Implicits](docs/implicits.md) 
- [Polymorphism](docs/polymorphism.md)
- More on [Companion Objects](docs/companion-objects.md)
- [Map and foreach](docs/map-and-foreach.md)
- [Type Variance](docs/variance.md)
- [Monad](docs/monad.md)
- [More Monads](docs/more-monads.md)
- [Scala Concurrency Introduction](docs/scala-concurrency.md)
- More on [Cats type classes in Cats Effect](docs/more-cats-effect.md)
- Creating our [own custom type classes](docs/custom-typeclass.md)
- Cats Effect: [Fibers and Errors](docs/cats-effect-fibers-and-errors.md)
- [Validated and Applicative](docs/validated-and-applicative.md)
- [Testing](docs/testing.md)

### Absolute basics on of Category theory
- [What is a category](docs/category.md)
- [Functor](docs/functor.md)

## scala
- [Programming in Scala](https://people.cs.ksu.edu/~schmidt/705a/Scala/Programming-in-Scala.pdf) 
## Functional programming and Cats resources

- Cats [documentation](https://typelevel.org/cats/)
- Herding Cats [website](https://eed3si9n.com/herding-cats/index.html)
- Scala with Cats [book](https://www.scalawithcats.com/) in various formats
- Functional Programming in Scala [book](https://www.manning.com/books/functional-programming-in-scala) which is not free
