# Validated and Applicative

We have seen `Either` can be used for error handling.
`Either` has a `Monad` instance in cats but also supports monad style functions natively.
In scala `Either` is right biased, which means that `map` and `flatMap` operate on the right element.
If the either is a `Left` then `map` and `flatMap` are no-ops

## Multiple errors example with `Either`

We write a program that takes user input, and decides whether the user is entitled to a driver's insurance discount.
The user must be a female driver over 21 for this discount
```scala
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
```scala
val discountInput:Input = Map(
  "age" -> "29",
  "driver" -> "true",
  "sex" -> "female"
)
// discountInput: Input = Map(
//   "age" -> "29",
//   "driver" -> "true",
//   "sex" -> "female"
// )

insuranceDiscount(discountInput)
// res0: Either[String, Int] = Right(123)
```
Great this customer gets the discount.

Try with user input for someone who does not meet our criteria
```scala
val noDiscountInput:Input = Map(
  "age" -> "18",
  "driver" -> "true",
  "sex" -> "male"
)
// noDiscountInput: Input = Map(
//   "age" -> "18",
//   "driver" -> "true",
//   "sex" -> "male"
// )

insuranceDiscount(noDiscountInput)
// res1: Either[String, Int] = Left("No discount for customers under 21")
```
This customer does not get the discount.
One reason is given but really there are 2 reasons, age and sex



Run the program with invalid input
```scala
val wrongFormatInput:Input = Map(
  "age" -> "twenty nine",
  "driver" -> "yes",
  "sex" -> "lady"
)
// wrongFormatInput: Input = Map(
//   "age" -> "twenty nine",
//   "driver" -> "yes",
//   "sex" -> "lady"
// )

insuranceDiscount(wrongFormatInput)
// res2: Either[String, Int] = Left("driver status was not true or false")
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

However, it does not have a `Monad` instance, so we cannot use `flatMap` or a `for` comprhension.
It does have an `Applicative` and we can use that instead

```scala
import  cats.data._


def discount(driver:Boolean, age:Int, sex:Sex):Either[NonEmptyChain[String],Int] = {
  (Validated.cond(driver, 0, "No discount for non-drivers").toValidatedNec *>
    Validated.cond(age >= 21, 40, "No discount for customers under 21").toValidatedNec,
    Validated.cond(sex == Female, 83, "No discount for male customers").toValidatedNec).mapN((a,b) => a + b).toEither
}

def validatedInsuranceDiscount(input: Input):Either[String,Int] =
  (
    isDriver(input).toValidatedNec,
    getAge(input).toValidatedNec,
    getSex(input).toValidatedNec
    ).mapN[Either[NonEmptyChain[String],Int]](discount).toEither.flatten.leftMap(_.toList.mkString("\n"))
```


Run the program with valid input
```scala
validatedInsuranceDiscount(discountInput)
// res3: Either[String, Int] = Right(123)
```
Great this customer gets the discount.

```scala
validatedInsuranceDiscount(noDiscountInput)
// res4: Either[String, Int] = Left(
//   """No discount for customers under 21
// No discount for male customers"""
// )
```
This customer does not get the discount.
Both reasons are given


Run the program with invalid input
```scala
validatedInsuranceDiscount(wrongFormatInput)
// res5: Either[String, Int] = Left(
//   """driver status was not true or false
// Could not read [twenty nine] as an integer
// Sex not one of `male` or `female`"""
// )
```


