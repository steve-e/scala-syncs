# More cats effects - Fiber and error handling

Cats effect includes a `Fiber` type class that represents a computation that can happen in parallel.
Starting a new Fiber can be thought of as starting a lightweight thread.

This example is extended from the cats effect documentation. 
It illustrates starting Fibers, concurrency and error handling.

```scala
import cats.effect._
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
// timer: Timer[IO] = cats.effect.internals.IOTimer@54e4bd41
implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
// contextShift: ContextShift[IO] = cats.effect.internals.IOContextShift@467eea41

def stdOut(message: String): IO[Unit] = IO(println(message))
def raiseError(message:String): IO[Unit] = IO.raiseError(new Exception(message))

def launch(tripUp: Boolean): IO[Unit] = {
  val runToBunker = stdOut("To the bunker!!!")
  val launchMissiles: IO[Unit] = stdOut("launching ...") >> Timer[IO].sleep(1.second) >> raiseError("boom!")

  val trip = if (tripUp) raiseError("Whoops! I tripped")
  else IO.unit

  for {
    fiber <- launchMissiles.start
    _ <- runToBunker >> trip handleErrorWith { error =>
      stdOut(error.getMessage) >>
        stdOut("Cancel launch!!") >>
        fiber.cancel >>
        IO.raiseError(error)
    }
    aftermath <- fiber.join
  } yield aftermath
}
```

Running the program without tripping up on the way to the bunker, and successfully launch missiles

```scala
launch(tripUp = false).attempt.unsafeRunSync()
// launching ...
// To the bunker!!!
// res0: Either[Throwable, Unit] = Left(java.lang.Exception: boom!)
```
in worksheet prints the following but output is reduced in mdoc
```
launching ...
To the bunker!!!
java.lang.Exception: boom!
  at .launch(<console>:25)
  ... 37 elided
```

Running the program tripping up on the way to the bunker, cancelling the launch

```scala
launch(tripUp = true).attempt.unsafeRunSync()
// launching ...
// To the bunker!!!
// Whoops! I tripped
// Cancel launch!!
// res1: Either[Throwable, Unit] = Left(java.lang.Exception: Whoops! I tripped)
```
in worksheet prints the following but output is reduced in mdoc
```
To the bunker!!!
launching ...
Whoops! I tripped
Cancel launch!!
java.lang.Exception: Whoops! I tripped
  at .launch(<console>:27)
  ... 37 elided
```
## Guaranteeing actions in the face of errors

We can ensure that the special handling of an object in various ways

## Guarantee

The guarantee takes an `IO[Unit]` that is always executed, even if there is an error
```scala
val ops = stdOut("Starting ...") >> raiseError("Halting !!") >> stdOut("never happens")
// ops: IO[Unit] = Bind(
//   Bind(
//     Delay(<function0>),
//     cats.syntax.FlatMapOps$$$Lambda$5589/1063134298@3ff99291
//   ),
//   cats.syntax.FlatMapOps$$$Lambda$5589/1063134298@4bf4f852
// )
ops.guarantee(stdOut("definitely happens")).attempt.unsafeRunSync()
// Starting ...
// definitely happens
// res2: Either[Throwable, Unit] = Left(java.lang.Exception: Halting !!)
```

## Bracket

The `bracket` takes 2 functions. The first operates on the enclosed type, but suspending the result in `IO`.
The second function "releases" the type with a side effecting result of `IO[Unit]`.

```scala
val bracketEx = IO.pure("a value").bracket(v => raiseError(s"Halting $v !!"))(v => stdOut(s"release $v"))
// bracketEx: IO[Unit] = Async(
//   cats.effect.internals.IOBracket$$$Lambda$5627/561848488@7fa40e73,
//   false
// )
bracketEx.attempt.unsafeRunSync()
// release a value
// res3: Either[Throwable, Unit] = Left(
//   java.lang.Exception: Halting a value !!
// )
```

This example nests 2 brackets, so we can operate both on a java `File` and a `FileWriter`.
```scala
import java.io.{File, FileWriter}

val writeFile = IO(new File("/tmp","bracket-test ")).bracket { file =>
  IO(new FileWriter(file)).bracket(f =>
    IO(f.write("Hello")) *> IO(f.flush())
  )(f => IO(f.close()))
}(file => IO(println("File Length "+file.length())) *> IO(file.delete())
)
// writeFile: IO[Unit] = Async(
//   cats.effect.internals.IOBracket$$$Lambda$5627/561848488@2629949,
//   false
// )
writeFile.unsafeRunSync()
// File Length 5
```

## Resource
`Resource` allows combines acquisition and release of a type. 
There is a `Monad` instance for `Resource`, so it is easier to compose than `Bracket`

```scala
val fileResource = Resource.make(IO(new File("/tmp","file-test ")))(file => IO(file.delete()) >> stdOut("deleting file"))
// fileResource: Resource[IO, File] = Allocate(
//   Map(Delay(<function0>), scala.Function1$$Lambda$640/1938770829@396538f3, 1)
// )
def writerResource(file:File) =  Resource.make(IO(new FileWriter(file)))(w => IO(w.close()) >> stdOut("closing writer"))

val writerWithDeletingFile =  for {
    file <- fileResource
    writer <- writerResource(file)
} yield writer
// writerWithDeletingFile: Resource[[x]IO[x], FileWriter] = Bind(
//   Allocate(
//     Map(Delay(<function0>), scala.Function1$$Lambda$640/1938770829@396538f3, 1)
//   ),
//   <function1>
// )

val resourceEx = writerWithDeletingFile.use(f =>
    IO(f.write("Hello")) *> IO(f.flush())
)
// resourceEx: IO[Unit] = Async(
//   cats.effect.internals.IOBracket$$$Lambda$5627/561848488@6e7319e8,
//   false
// )

resourceEx.unsafeRunSync()
// closing writer
// deleting file
```
