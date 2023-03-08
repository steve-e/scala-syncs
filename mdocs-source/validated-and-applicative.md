# Validated and Applicative

We have seen `Either` can be used for error handling.
`Either` has a `Monad` instance in cats but also supports monad style functions natively.
In scala `Either` is right biased, which means that `map` and `flatMap` operate on the right element.
If the either is a `Left` then `map` and `flatMap` are no-ops

## Multiple errors example with `Either`

We write a program that takes user input, and decides whether the user is entitled to a driver's insurance discount.
The user must be a female driver over 21 for this discount
```scala mdoc
import cats._
import cats.implicits._

import scala.util.Try
type Input = Map[String,String]

sealed trait Sex
case object Male extends Sex
case object Female extends Sex

def getAge(input:Input):Either[String,Int] = 
  input.get("age").toRight("No age given")
  .flatMap(s => Try(s.toInt).toEither.leftMap(_ => s"Could not read [$s] as an integer") )
  
def getSex(input:Input):Either[String,Sex] =
  input.get("sex").toRight("No sex given").flatMap{
    case "male" => Male.asRight
    case "female" => Female.asRight
    case _ => "Sex not one of `male` or `female`".asLeft
  }
def isDriver(input:Input):Either[String,Boolean] =
  input.get("driver").toRight("driver status not given").flatMap{
    case "true" => true.asRight
    case "false" => false.asRight
    case _ => "driver status was not true or false".asLeft
  }

def calculateDiscount(driver:Boolean, age:Int, sex:Sex):Either[String,Int] = 
    for {
        a <- Either.cond(driver, 0, "No discount for non-drivers") >>
             Either.cond(age >= 21, 40, "No discount for customers under 21")
        b <- Either.cond(sex == Female, 83, "No discount for male customers")    
    } yield a + b

def insuranceDiscount(input: Input):Either[String,Int] = 
  for {  
    driver <- isDriver(input)
    age <- getAge(input)
    sex <- getSex(input)
    discount <-calculateDiscount(driver, age, sex)
} yield discount

```
Run the program with valid input
```scala mdoc
val discountInput:Input = Map(
  "age" -> "29",
  "driver" -> "true",
  "sex" -> "female"
)

insuranceDiscount(discountInput)
```
Great this customer gets the discount.

Try with user input for someone who does not meet our criteria
```scala mdoc
val noDiscountInput:Input = Map(
  "age" -> "18",
  "driver" -> "true",
  "sex" -> "male"
)

insuranceDiscount(noDiscountInput)
```
This customer does not get the discount.
One reason is given but really there are 2 reasons, age and sex



Run the program with invalid input
```scala mdoc
val wrongFormatInput:Input = Map(
  "age" -> "twenty nine",
  "driver" -> "yes",
  "sex" -> "lady"
)

insuranceDiscount(wrongFormatInput)
```
This customer filled in the form incorrectly and so does not get a discount.
But we only report one of the 3 errors.
If the user has filled in a web form, they want to see all the errors.
Otherwise, they would correct on error and re-submit and then see the next, and so on.

We can't accumulate errors with `Either` because it is a `Monad`.
A `Monad` sequences operations and short circuits on failure.
We need a different typeclass to be able to accumulate errors

## Validated

Cats has another type that can be used for error handling.
This is the `Validated[+E,+A]` type. It has two instances.

```scala
case class Valid[+A](a: A) extends Validated[Nothing, A]
case class Invalid[+E](e: E) extends Validated[E, Nothing]
```

`Validated` is similar to `Either` in that it has two type parameters, the left one for errors and the right for success.
```scala mdoc
import  cats.data._
import  cats.data.Validated._

  val event1:Validated[String,String] = Validated.catchOnly[Exception]("event 1 ok").leftMap(_.getMessage)
  val event2:Validated[String,String] = valid("event 2, definitely ok")
  val event3:Validated[String,String] = Validated.catchOnly[Exception]((1/0).toString).leftMap(_.getMessage)
  val event4:Validated[String,String] = invalid("fail")
```
However, it does not have a `Monad` instance, so we cannot use `flatMap` or a `for` comprehension.

No `flatMap`
```scala mdoc:fail
val event1after2 = event1.flatMap(e => event2)
```
or syntax for `Monad`
```scala mdoc:fail
val event1after2 = event1 >> event2
```
It does have an `Applicative` and we can use that instead
```scala mdoc

  val event1then2 = event1 *> event2
  val twoEvents = (event1, event2).mapN((a,b) => s"Got $a, $b" )
  val allEvents = (event1, event2, event3, event4).mapN((a, b, c, d) => s"Got $a $b $c $d")
```
Notice in the event of multiple failures that the errors are combined using a Semigroup for String.
It is not ideal to just concatenate error Strings.
Usually a collection is used for errors instead.
`Validated` provides built-in support for using `NonEmptyList` or `NonEmptyChain` instead.
```scala mdoc
  val combinedEventErrors = (event3.toValidatedNec, event4.toValidatedNec).mapN((a, b) => s"Got $a $b")
  combinedEventErrors.leftMap(_.toList.mkString("\n"))

```

We can rewrite the program using `Either` to use `Validated` internally instead
```scala mdoc

def discount(driver:Boolean, age:Int, sex:Sex):Either[NonEmptyChain[String],Int] = {
  (
    Validated.cond(driver, 0, "No discount for non-drivers").toValidatedNec *>
    Validated.cond(age >= 21, 40, "No discount for customers under 21").toValidatedNec,
    Validated.cond(sex == Female, 83, "No discount for male customers").toValidatedNec
  ).mapN((a,b) => a + b).toEither
}

def validatedInsuranceDiscount(input: Input):Either[String,Int] =
  (
    isDriver(input).toValidatedNec,
    getAge(input).toValidatedNec,
    getSex(input).toValidatedNec
  ).mapN[Either[NonEmptyChain[String],Int]](discount)
  .toEither.flatten.leftMap(_.toList.mkString("\n"))
```


Run the program with valid input
```scala mdoc
validatedInsuranceDiscount(discountInput)
```
Great this customer gets the discount.

```scala mdoc
validatedInsuranceDiscount(noDiscountInput)
```
This customer does not get the discount.
Both reasons are given


Run the program with invalid input
```scala mdoc
validatedInsuranceDiscount(wrongFormatInput)
```


