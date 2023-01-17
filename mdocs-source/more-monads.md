# Example Monads

As mentioned in the monads doc, we can use different monads 
for different effects or contexts

Give this simple API
```scala mdoc

trait Api[F[_]] {    
    def get(key:String):F[Int]
    def repeat(n:Int):F[String]
}
```
We can write a small program

```scala mdoc
import cats.Monad
import cats.implicits._

def program[F[_]:Monad](api:Api[F])(key:String):F[String] = {
    for {
        i <- api.get(key)
        s <- api.repeat(i)
    } yield s
}
```

This program receives a key as a String, get an F or Int and uses the 
Int to call repeat. It returns the result.

We can implement the Api for various monads.

## Option

Implemented to maybe return an Int and maybe return a String
```scala mdoc

object ApiOption extends Api[Option] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):Option[Int] = store.get(key)
    def repeat(n:Int):Option[String] = if(n > 0) Some("repeat" * n) else None 
}

```
We can use this with our program
```scala mdoc

program(ApiOption)("a")
program(ApiOption)("b")
program(ApiOption)("c")
```

## List
```scala mdoc

object ApiList extends Api[List] {
    private val store:Map[String,List[Int]] = Map(
        "a" -> List(2,3),
        "b" -> Nil
     )
    
    def get(key:String):List[Int] = store.get(key).toList.flatten
    
    def repeat(n:Int):List[String] = if(n > 0) List.fill(n)("repeat") else Nil 
}

```

We can use this with our program
```scala mdoc

program(ApiList)("a")
program(ApiList)("b")
program(ApiList)("c")
```

## Either


```scala mdoc

type Result[T] = Either[String,T] 
object ApiEither extends Api[Result] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):Result[Int] = store.get(key).toRight(s"No Result for $key")
    def repeat(n:Int):Result[String] = if(n > 0) Right("repeat" * n) else Left("Can't repeat negatively") 
}

```
We can use this with our program
```scala mdoc

program(ApiEither)("a")
program(ApiEither)("b")
program(ApiEither)("c")
```

## Future

We haven't looked at future yet. 
It allows computations to be executed in another thread, and so can
allow asynchronous computations

```scala mdoc
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object ApiFuture extends Api[Future] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):Future[Int] = Future{
        println(s"getting $key")
        Thread.sleep(1000)        
        store(key)
    }
    def repeat(n:Int):Future[String] = 
        if(n > 0) Future("repeat" * n) 
        else Future.failed(new Exception("Can't repeat negatively")) 
}

```
We can use this with our program
```scala mdoc

program(ApiFuture)("a")
program(ApiFuture)("b")
program(ApiFuture)("c")
```
but calling toString on the Future does not await or complete it.
Also notice that the messages are printed out of sync with the program 
and with each other

Let's try again

```scala mdoc
import scala.concurrent.duration._
val timeout = 2.seconds

Await.result(program(ApiFuture)("a"), timeout)
```
but it may through exceptions
```scala mdoc:crash
Await.result(program(ApiFuture)("a"), 1.milli)
```

```scala mdoc:crash
Await.result(program(ApiFuture)("b"), timeout)
```

```scala mdoc:crash
Await.result(program(ApiFuture)("c"), timeout)
```

## Eval

Another monad we haven't looked at before is Eval.
This provides a type class to abstract evaluation.
We have discussed this before but let's recap.

In scala we have eager evaluation
```scala mdoc
val x = 3
```
lazy or deferred evaluation
```scala mdoc
lazy val y = 3
```

and repeated evaluation

```scala mdoc
def z = 3
```

The Eval typeclass had instances for each case

```scala mdoc
import cats.Eval

object ApiEval extends Api[Eval] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):Eval[Int] = Eval.always{
        println(s"getting $key")
        Thread.sleep(1000)        
        store(key)
    }
    def repeat(n:Int):Eval[String] = 
        if(n > 0) Eval.now("repeat" * n) 
        else Eval.defer[String](throw new Exception("Can't repeat negatively")) 
}

```
We can use this with our program
```scala mdoc

program(ApiEval)("a")
program(ApiEval)("b")
program(ApiEval)("c")
```
Nothing is evaluated here. 
The program creates an Eval that we still need to evaluate.
The Eval is a description of a program in which some computations are suspended.
Unlike with ApiFuture, we did not see any printed messages yet.

```scala mdoc
program(ApiEval)("a").value
```
```scala mdoc:crash
program(ApiEval)("b").value
```
```scala mdoc:crash
program(ApiEval)("c").value
```
## IO
IO is part of cats effect. 
It combines some of the capabilities of Either, Eval and Future (and more)
in a functional way.

For a change we will wrap an existing API.
By wrapping ApiFuture with IO we can make it a little better behaved
```scala mdoc
import cats.effect._

implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  
object ApiIO extends Api[IO] {
    def get(key:String):IO[Int] = IO.fromFuture(IO(ApiFuture.get(key)))
    def repeat(n:Int):IO[String] = IO.fromFuture(IO(ApiFuture.repeat(n)))
        
}

```

We can use this with our program
```scala mdoc

program(ApiIO)("a")
program(ApiIO)("b")
program(ApiIO)("c")
```
Nothing is evaluated here.
The program creates an IO that we still need to evaluate.

The IO is a description of a program in which some computations are suspended.

Unlike with ApiFuture, we did not see any printed messages yet.

Unlike Eval our program can run computations in another thread.

We have different options to evaluate the IO. 
The `attempt` function causes the IO executed with errors accumulated in an Either

```scala mdoc

program(ApiIO)("a").unsafeRunSync()
program(ApiIO)("b").attempt.unsafeRunSync()
program(ApiIO)("c").attempt.unsafeRunSync()
```
