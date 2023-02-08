# A deeper look into Cats and Cats Effect

The cats effect library and particularly `IO`, have instances for many type classes.
Some we haven't looked at yet.

## Apply and Applicative type class

`IO` is a monad and supports monad syntax.
`>>` is a synonym for flatMap, ignoring its input.

`IO` also has an `Apply` instance. This provides syntax for
`*>` which has a similar functionality to `>>` and is a synonym for `productR`.

```scala
import cats.effect._
import cats.implicits._
import scala.concurrent.ExecutionContext

implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
// contextShift: ContextShift[IO] = cats.effect.internals.IOContextShift@61c11657

val io1 = IO{Thread.sleep(1000)} >> IO(println("slept"))
// io1: IO[Unit] = Bind(
//   Delay(<function0>),
//   cats.syntax.FlatMapOps$$$Lambda$5992/955832901@53d0f6fc
// )
val io2 = IO{Thread.sleep(1000)} >> IO(println("slept again"))
// io2: IO[Unit] = Bind(
//   Delay(<function0>),
//   cats.syntax.FlatMapOps$$$Lambda$5992/955832901@361a33ac
// )
val program = (io1 *> io2)
// program: IO[Unit] = Bind(
//   Bind(
//     Delay(<function0>),
//     cats.syntax.FlatMapOps$$$Lambda$5992/955832901@53d0f6fc
//   ),
//   cats.FlatMap$$Lambda$5993/543588293@b9f059e
// )

program.unsafeRunSync()
// slept
// slept again
```


`Apply` allows the application of an effectful function to an effectful value.

```scala
val function:Option[String => Int] = Some(s => s.length)
// function: Option[String => Int] = Some(<function1>)
val someString = "This string is quite long.".some
// someString: Option[String] = Some("This string is quite long.")
val noString = none[String]
// noString: Option[String] = None
function.ap(someString)
// res1: Option[Int] = Some(26)
function <*> someString // Same using syntax
// res2: Option[Int] = Some(26) // Same using syntax
function.ap(noString)
// res3: Option[Int] = None
```
We can look a bit closer at `productR` and `productL`
```scala
val rightString:Option[String] = "right".some
// rightString: Option[String] = Some("right")

someString.productR(rightString)
// res4: Option[String] = Some("right")
someString *> rightString // Same, using syntax
// res5: Option[String] = Some("right") // Same, using syntax

someString.productL(rightString)
// res6: Option[String] = Some("This string is quite long.")
someString <* rightString
// res7: Option[String] = Some("This string is quite long.")

noString.productR(someString)
// res8: Option[String] = None
noString.productL(someString)
// res9: Option[String] = None
```
Both sides are evaluated, even if only the right is returned
```scala
val printer:String => IO[Unit] = i => IO(println(s"Evaluating: $i"))
// printer: String => IO[Unit] = <function1>

val left = IO.pure("left").flatTap(printer)
// left: IO[String] = Bind(
//   Pure("left"),
//   cats.FlatMap$$Lambda$5998/1860780971@6945a12e
// )
val right = IO.pure("right").flatTap(printer)
// right: IO[String] = Bind(
//   Pure("right"),
//   cats.FlatMap$$Lambda$5998/1860780971@34047249
// )
val pr = left *> right
// pr: IO[String] = Bind(
//   Bind(Pure("left"), cats.FlatMap$$Lambda$5998/1860780971@6945a12e),
//   cats.FlatMap$$Lambda$5993/543588293@268e401b
// )
pr.unsafeRunSync()
// Evaluating: left
// Evaluating: right
// res10: String = "right"
```
Apply also provides an enhanced set of `map` functions including `mapN`

```scala
val valid1 = "success!".some 
// valid1: Option[String] = Some("success!") 
val valid2 = "another success!".some
// valid2: Option[String] = Some("another success!")
val invalid = none[String]
// invalid: Option[String] = None


(valid1, valid2).mapN((a,b) => s"$a and $b") 
// res11: Option[String] = Some("success! and another success!") 
(valid1, valid1, valid2).mapN((a,b,c) => a.length + b.length + c.length) 
// res12: Option[Int] = Some(32)
```
The `Applicative` type class is an `Apply` with a `pure` method.
We have seen that aspect already as `Monad` gets its `pure` by extending `Applicative`

## Traverse type class

The `Traverse` type class provides `sequence` and `traverse` and other methods.

One common use case is that a pure `List` of values needs to have an operation called on each element.

We can `map` the operation on the `List`, and then use `sequence` to reverse the enclosing type order

```scala
def process(input:String):IO[Unit] = IO{ 
        println(s"Processing: [$input]")
    }

val inputs:List[String] = List("Foo", "bar", "BAZ")
// inputs: List[String] = List("Foo", "bar", "BAZ")

val listOfIO:List[IO[Unit]] = inputs.map(process)
// listOfIO: List[IO[Unit]] = List(
//   Delay(<function0>),
//   Delay(<function0>),
//   Delay(<function0>)
// )

// It is difficult to evaluate a List of IO, so lets flip the type order

val ioOfList:IO[List[Unit]] = listOfIO.sequence  
// ioOfList: IO[List[Unit]] = Bind(
//   Delay(<function0>),
//   cats.FlatMap$$Lambda$6014/1654196543@554e12a1
// )  

ioOfList.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
// res13: List[Unit] = List((), (), ())
```

These 2 operations can be combined using `traverse`. 
This is more efficient as the list is traversed only once.

```scala
val runner = inputs.traverse(process)
// runner: IO[List[Unit]] = Bind(
//   Delay(<function0>),
//   cats.FlatMap$$Lambda$6014/1654196543@11c695f7
// )
runner.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
// res14: List[Unit] = List((), (), ())
```

We could run the effects in parallel. 
But we need to consider if the processor (eg a webservice or database) can handle very rapid requests.
We also need to know that our effects do not need to happen in order.

```scala
val parallel = inputs.parTraverse(process)
// parallel: IO[List[Unit]] = Async(<function2>, true)
parallel.unsafeRunSync()
// Processing: [BAZ]
// Processing: [Foo]
// Processing: [bar]
// res15: List[Unit] = List((), (), ())
```

Both `traverse` and `sequence` appear in the standard library on `Future`. 
The cats api version can work for any cats with a `Traverse` instance.

Each of the previous examples of processing a list ended with a `List[Unit]`
indicating that each element was processed.

We could return just `Unit` indicating that all items have been processed.

One way is to use `List.map`, then `List.fold`, with `>>` as the combiner function
```scala
val all = inputs.map(process).fold(IO.unit)(_ >> _)
// all: IO[Unit] = Bind(
//   Bind(
//     Bind(Pure(()), cats.syntax.FlatMapOps$$$Lambda$5992/955832901@6473bbc),
//     cats.syntax.FlatMapOps$$$Lambda$5992/955832901@521d2df7
//   ),
//   cats.syntax.FlatMapOps$$$Lambda$5992/955832901@7763f60f
// )
all.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
```

It is possible to do the same more succinctly with `foldMapM` from the `Traverse` type class.
This makes use of an implicitly available for `Monoid[IO]`
```scala
val k = inputs.foldMapM(process)
// k: IO[Unit] = Bind(
//   Map(Delay(<function0>), scala.Function1$$Lambda$615/566922409@522fbc39, 1),
//   cats.StackSafeMonad$$Lambda$6036/1222805622@6d8af71b
// )
k.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
```

## Monoid

A monoid is an algebraic structure with an associative binary operation on a set together with a neutral element.
In the cats library, the operation is called `combine`.
The neutral or identity element is called `empty`

The basic idea is easy to understand with some examples.

### Additive monoid for Int
```scala
import cats.Monoid

val ints:List[Int] = (1 to 5).toList
// ints: List[Int] = List(1, 2, 3, 4, 5)

val addition = new Monoid[Int] {
                   val empty = 0
                   def combine(a:Int, b:Int):Int = a + b 
                }
// addition: AnyRef with Monoid[Int]{val empty: Int} = repl.MdocSession$MdocApp$$anon$1@56c31dc8

ints.combineAll(addition)
// res18: Int = 15
```

Actually there is an additive `Monoid[Int]` available implicitly.
You can use that if you want addition

```scala
ints.combineAll 
// res19: Int = 15
```
This implicit `Monoid[Int]` is defined as a `CommutativeGroup[Int]` which is also
a `Group[Int]` and a `Semigroup[Int]`. Groups are Monoids, Monoids are Semigroups.
The `Semigroup` declares the `combine` methods. 
I have worked on projects that often defined a `Monoid` or `Semigroup`.
But I haven't seen `Group` used on a project.


For multiplication, we must define our own `Monoid`.
This is because only one can be available in the same scope.
### Multiplicative monoid for Int
```scala
val multiplication = new Monoid[Int] {
                   val empty = 1
                   def combine(a:Int, b:Int):Int = a * b 
                }
// multiplication: AnyRef with Monoid[Int]{val empty: Int} = repl.MdocSession$MdocApp$$anon$2@3e784a91

ints.combineAll(multiplication)
// res20: Int = 120
```
### List monoid
A Monoid is not just for numeric types but can be created for IO, List, String, Unit, etc.
```scala
val lists:List[List[Int]] = List(List(1,2,3),List(4,5),List(6,7))
// lists: List[List[Int]] = List(List(1, 2, 3), List(4, 5), List(6, 7))

def concatination[A] = new Monoid[List[A]] {
                   val empty = Nil
                   def combine(a:List[A], b:List[A]):List[A] = a ++ b 
                }

lists.combineAll(concatination)
// res21: List[Int] = List(1, 2, 3, 4, 5, 6, 7)
```

If we don't have a monoid, we can use `fold`.
Subtraction is not associative, so we should not create a monoid instance for it.

```scala
ints.fold(0)((a,b) => a - b)
// res22: Int = -15
```
