# More cats effects 
## Fiber and error handling

### Fiber
Cats effect includes a `Fiber` type class that represents a computation that can happen in parallel.
Starting a new `Fiber` can be thought of as starting a lightweight thread.
A `Fiber` can be created by calling `start` on an `IO`

This example is extended from the cats effect documentation. 
It illustrates starting a `Fiber`, concurrency and error handling.

```scala
import cats.effect._
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
// timer: Timer[IO] = cats.effect.internals.IOTimer@30fd1b81
implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
// contextShift: ContextShift[IO] = cats.effect.internals.IOContextShift@16a425ba

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

Running the program without tripping up on the way to the bunker, and successfully launch missiles.
We use `attempt` to materialise the result as an `Either`, capturing an `Exception` instead of throwing it.

```scala
launch(tripUp = false).attempt.unsafeRunSync()
// launching ...
// To the bunker!!!
// res0: Either[Throwable, Unit] = Left(java.lang.Exception: boom!)
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

### Guaranteeing actions in the face of errors

We can ensure that the special handling of an object in various ways

### Guarantee

The guarantee takes an `IO[Unit]` that is always executed, even if there is an error
```scala
val ops = stdOut("Starting ...") >> raiseError("Halting !!") >> stdOut("never happens")
// ops: IO[Unit] = Bind(
//   Bind(
//     Delay(<function0>),
//     cats.syntax.FlatMapOps$$$Lambda$6561/220728228@41549240
//   ),
//   cats.syntax.FlatMapOps$$$Lambda$6561/220728228@43b80f86
// )
ops.guarantee(stdOut("definitely happens")).attempt.unsafeRunSync()
// Starting ...
// definitely happens
// res2: Either[Throwable, Unit] = Left(java.lang.Exception: Halting !!)
```

### Bracket

The `bracket` takes 2 functions. The first operates on the enclosed type, but suspending the result in `IO`.
The second function "releases" the type with a side effecting result of `IO[Unit]`.

```scala
val bracketEx = IO.pure("a value").bracket(v => raiseError(s"Halting $v !!"))(v => stdOut(s"release $v"))
// bracketEx: IO[Unit] = Async(
//   cats.effect.internals.IOBracket$$$Lambda$6599/676479398@3807afe4,
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
//   cats.effect.internals.IOBracket$$$Lambda$6599/676479398@241ecd10,
//   false
// )
writeFile.unsafeRunSync()
// File Length 5
```

### Resource
`Resource` combines acquisition and release of a type. 
There is a `Monad` instance for `Resource`, so it is easier to compose than `Bracket`.

Here we compose a `File` and a `FileWriter`.
They are both released automatically in the right order.

```scala
val fileResource = Resource.make(IO(new File("/tmp","file-test ")) <* stdOut("creating file") )(file => IO(file.delete()) >> stdOut("deleting file"))
// fileResource: Resource[IO, File] = Allocate(
//   Map(
//     Bind(Delay(<function0>), cats.FlatMap$$Lambda$6603/661068284@f839f55),
//     scala.Function1$$Lambda$622/1079821589@b3dad1b,
//     1
//   )
// )
def writerResource(file:File) =  Resource.make(IO(new FileWriter(file)) <* stdOut("creating writer"))(w => IO(w.close()) >> stdOut("closing writer"))

val writerWithDeletingFile =  for {
    file <- fileResource
    writer <- writerResource(file)
} yield writer
// writerWithDeletingFile: Resource[[x]IO[x], FileWriter] = Bind(
//   Allocate(
//     Map(
//       Bind(Delay(<function0>), cats.FlatMap$$Lambda$6603/661068284@f839f55),
//       scala.Function1$$Lambda$622/1079821589@b3dad1b,
//       1
//     )
//   ),
//   <function1>
// )

val resourceEx = writerWithDeletingFile.use(f =>
    IO(f.write("Hello")) *> stdOut("Wrote file") *> IO(f.flush())
)
// resourceEx: IO[Unit] = Async(
//   cats.effect.internals.IOBracket$$$Lambda$6599/676479398@8bc55d8,
//   false
// )

resourceEx.unsafeRunSync()
// creating file
// creating writer
// Wrote file
// closing writer
// deleting file
```
