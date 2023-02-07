# A deeper look into Cats Effect

`IO` is a monad and supports monad syntax

`>>` is a synonym for flatMap, ignoring its input
`*>` has a similar functionality using `productR` from the `Apply` type class

```scala
import cats.effect._
import cats.implicits._
import scala.concurrent.ExecutionContext

implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
// contextShift: ContextShift[IO] = cats.effect.internals.IOContextShift@38f45903

val io1 = IO{Thread.sleep(1000)} >> IO(println("slept"))
// io1: IO[Unit] = Bind(
//   Delay(<function0>),
//   cats.syntax.FlatMapOps$$$Lambda$6000/705837762@7e921d44
// )
val io2 = IO{Thread.sleep(1000)} >> IO(println("slept again"))
// io2: IO[Unit] = Bind(
//   Delay(<function0>),
//   cats.syntax.FlatMapOps$$$Lambda$6000/705837762@1aaed4ee
// )
val program = (io1 *> io2)
// program: IO[Unit] = Bind(
//   Bind(
//     Delay(<function0>),
//     cats.syntax.FlatMapOps$$$Lambda$6000/705837762@7e921d44
//   ),
//   cats.FlatMap$$Lambda$6001/546788437@5c10fce8
// )

program.unsafeRunSync()
// slept
// slept again
```

One common use case is that a pure List of values needs to have an operation called on each element.

We can map the operation on the list, and the use sequence to reverse the monad order
```scala
def process(input:String):IO[Unit] = IO{ println(s"Processing: [$input]")}

val inputs:List[String] = "Foo bar BAZ".split("\\s+").toList
// inputs: List[String] = List("Foo", "bar", "BAZ")
val listOfIO:List[IO[Unit]] = inputs.map(process)
// listOfIO: List[IO[Unit]] = List(
//   Delay(<function0>),
//   Delay(<function0>),
//   Delay(<function0>)
// )
val ioOfList:IO[List[Unit]] = listOfIO.sequence  
// ioOfList: IO[List[Unit]] = Bind(
//   Delay(<function0>),
//   cats.FlatMap$$Lambda$6012/1727870143@33dcb0cd
// )  
ioOfList.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
// res1: List[Unit] = List((), (), ())
```

These 2 operations can be combined using `traverse`. 
This is more efficient as the list is traversed only once

```scala
val runner = inputs.traverse(process)
// runner: IO[List[Unit]] = Bind(
//   Delay(<function0>),
//   cats.FlatMap$$Lambda$6012/1727870143@7531dc7e
// )
runner.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
// res2: List[Unit] = List((), (), ())
```
We could run the effects in parallel. 
But we need to consider if the processor can handle very rapid requests.
We also need to know that our effects do not need to happen in order.
```scala
val parallel = inputs.parTraverse(process)
// parallel: IO[List[Unit]] = Async(<function2>, true)
parallel.unsafeRunSync()
// Processing: [BAZ]
// Processing: [Foo]
// Processing: [bar]
// res3: List[Unit] = List((), (), ())
```

Both `traverse` and `sequence` appear in the standard library on `Future`. 
The cats api version can work for any cats `Monad`.

Each of the previous examples of processing a list ended with a `List[Unit]`
indicating that each element was processed.
We could return just `Unit` indicating that all items have been processed.

```scala
val all = inputs.map(process).fold(IO.unit)(_ >> _)
// all: IO[Unit] = Bind(
//   Bind(
//     Bind(Pure(()), cats.syntax.FlatMapOps$$$Lambda$6000/705837762@6f633d43),
//     cats.syntax.FlatMapOps$$$Lambda$6000/705837762@344f7bfb
//   ),
//   cats.syntax.FlatMapOps$$$Lambda$6000/705837762@6243ef67
// )
all.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
```

It is possible to do the same more succinctly with `foldMapM` from the `Traverse` type class.
This makes use of an implicitly available for Monoid[IO]
```scala
val k = inputs.foldMapM(process)
// k: IO[Unit] = Bind(
//   Map(Delay(<function0>), scala.Function1$$Lambda$664/2039595716@19ea0d7d, 1),
//   cats.StackSafeMonad$$Lambda$6034/116256222@6f15b226
// )
k.unsafeRunSync()
// Processing: [Foo]
// Processing: [bar]
// Processing: [BAZ]
```
