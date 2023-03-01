# More cats effects 
## Fiber and error handling

### Fiber
Cats effect includes a `Fiber` type class that represents a computation that can happen in parallel.
Starting a new `Fiber` can be thought of as starting a lightweight thread.
A `Fiber` can be created by calling `start` on an `IO`

This example is extended from the cats effect documentation. 
It illustrates starting a `Fiber`, concurrency and error handling.

```scala mdoc
import cats.effect._
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

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

```scala mdoc
launch(tripUp = false).attempt.unsafeRunSync()
```

Running the program tripping up on the way to the bunker, cancelling the launch

```scala mdoc
launch(tripUp = true).attempt.unsafeRunSync()
```

### Guaranteeing actions in the face of errors

We can ensure that the special handling of an object in various ways

### Guarantee

The guarantee takes an `IO[Unit]` that is always executed, even if there is an error
```scala mdoc
val ops = stdOut("Starting ...") >> raiseError("Halting !!") >> stdOut("never happens")
ops.guarantee(stdOut("definitely happens")).attempt.unsafeRunSync()
```

### Bracket

The `bracket` takes 2 functions. The first operates on the enclosed type, but suspending the result in `IO`.
The second function "releases" the type with a side effecting result of `IO[Unit]`.

```scala mdoc
val bracketEx = IO.pure("a value").bracket(v => raiseError(s"Halting $v !!"))(v => stdOut(s"release $v"))
bracketEx.attempt.unsafeRunSync()
```

This example nests 2 brackets, so we can operate both on a java `File` and a `FileWriter`.
```scala mdoc
import java.io.{File, FileWriter}

val writeFile = IO(new File("/tmp","bracket-test ")).bracket { file =>
  IO(new FileWriter(file)).bracket(f =>
    IO(f.write("Hello")) *> IO(f.flush())
  )(f => IO(f.close()))
}(file => IO(println("File Length "+file.length())) *> IO(file.delete())
)
writeFile.unsafeRunSync()

```

### Resource
`Resource` combines acquisition and release of a type. 
There is a `Monad` instance for `Resource`, so it is easier to compose than `Bracket`.

Here we compose a `File` and a `FileWriter`.
They are both released automatically in the right order.

```scala mdoc

val fileResource = Resource.make(IO(new File("/tmp","file-test ")) <* stdOut("creating file") )(file => IO(file.delete()) >> stdOut("deleting file"))
def writerResource(file:File) =  Resource.make(IO(new FileWriter(file)) <* stdOut("creating writer"))(w => IO(w.close()) >> stdOut("closing writer"))

val writerWithDeletingFile =  for {
    file <- fileResource
    writer <- writerResource(file)
} yield writer

val resourceEx = writerWithDeletingFile.use(f =>
    IO(f.write("Hello")) *> stdOut("Wrote file") *> IO(f.flush())
)

resourceEx.unsafeRunSync()
```
