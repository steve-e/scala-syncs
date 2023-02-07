# A deeper look into Cats Effect

`IO` is a monad and supports monad syntax

`>>` is a synonym for flatMap, ignoring its input
`*>` has a similar functionality using `productR` from the `Apply` type class

```scala mdoc
import cats.effect._
import cats.implicits._
import scala.concurrent.ExecutionContext

implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

val io1 = IO{Thread.sleep(1000)} >> IO(println("slept"))
val io2 = IO{Thread.sleep(1000)} >> IO(println("slept again"))
val program = (io1 *> io2)

program.unsafeRunSync()

```

One common use case is that a pure List of values needs to have an operation called on each element.

We can map the operation on the list, and the use sequence to reverse the monad order
```scala mdoc
def process(input:String):IO[Unit] = IO{ println(s"Processing: [$input]")}

val inputs:List[String] = "Foo bar BAZ".split("\\s+").toList
val listOfIO:List[IO[Unit]] = inputs.map(process)
val ioOfList:IO[List[Unit]] = listOfIO.sequence  
ioOfList.unsafeRunSync()

```

These 2 operations can be combined using `traverse`. 
This is more efficient as the list is traversed only once

```scala mdoc
val runner = inputs.traverse(process)
runner.unsafeRunSync()
```
We could run the effects in parallel. 
But we need to consider if the processor can handle very rapid requests.
We also need to know that our effects do not need to happen in order.
```scala mdoc
val parallel = inputs.parTraverse(process)
parallel.unsafeRunSync()
```

Both `traverse` and `sequence` appear in the standard library on `Future`. 
The cats api version can work for any cats `Monad`.

Each of the previous examples of processing a list ended with a `List[Unit]`
indicating that each element was processed.
We could return just `Unit` indicating that all items have been processed.

```scala mdoc
val all = inputs.map(process).fold(IO.unit)(_ >> _)
all.unsafeRunSync()
```

It is possible to do the same more succinctly with `foldMapM` from the `Traverse` type class.
This makes use of an implicitly available for Monoid[IO]
```scala mdoc
val k = inputs.foldMapM(process)
k.unsafeRunSync()
```
