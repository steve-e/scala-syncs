# Example Monads

As mentioned in the monads doc, we can use different monads 
for different effects or contexts

Give this simple API
```scala
trait Api[F[_]] {    
    def get(key:String):F[Int]
    def repeat(n:Int):F[String]
}
```
We can write a small program

```scala
import cats.Monad
import cats.implicits._

def program[F[_]:Monad](api:Api[F])(key:String):F[String] = {
    for {
        i <- api.get(key)
        s <- api.repeat(i)
    } yield s
}
```

This program receives a key as a String, gets an F of Int and uses the 
Int to call repeat. It returns the result as an F of String.

We can implement the Api for various monads.

## Option

Implemented to maybe return an Int and maybe return a String
```scala
object ApiOption extends Api[Option] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):Option[Int] = store.get(key)
    def repeat(n:Int):Option[String] = if(n > 0) Some("repeat" * n) else None 
}
```
We can use this with our program
```scala
program(ApiOption)("a")
// res0: Option[String] = Some("repeatrepeat")
program(ApiOption)("b")
// res1: Option[String] = None
program(ApiOption)("c")
// res2: Option[String] = None
```

## List
```scala
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
```scala
program(ApiList)("a")
// res3: List[String] = List("repeat", "repeat", "repeat", "repeat", "repeat")
program(ApiList)("b")
// res4: List[String] = List()
program(ApiList)("c")
// res5: List[String] = List()
```

## Either


```scala
type Result[T] = Either[String,T]  
object ApiEither extends Api[Result] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):Result[Int] = store.get(key).toRight(s"No Result for $key")
    def repeat(n:Int):Result[String] = if(n > 0) Right("repeat" * n) else Left("Can't repeat negatively") 
}
```
We can use this with our program
```scala
program(ApiEither)("a")
// res6: Result[String] = Right("repeatrepeat")
program(ApiEither)("b")
// res7: Result[String] = Left("Can't repeat negatively")
program(ApiEither)("c")
// res8: Result[String] = Left("No Result for c")
```

## Future

We haven't looked at future yet. 
It allows computations to be executed in another thread, and so can
allow asynchronous computations

```scala
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
```scala
program(ApiFuture)("a")
// getting a
// res9: Future[String] = Future(Success(repeatrepeat))
program(ApiFuture)("b")
// res10: Future[String] = Future(Failure(java.lang.Exception: Can't repeat negatively))
program(ApiFuture)("c")
// getting b
// res11: Future[String] = Future(Failure(java.util.NoSuchElementException: key not found: c))
```
but calling toString on the Future does not await or complete it.
Also notice that the messages are printed out of sync with the program 
and with each other

Let's try again

```scala
import scala.concurrent.duration._
val timeout = 2.seconds
// timeout: FiniteDuration = 2 seconds

Await.result(program(ApiFuture)("a"), timeout)
// getting a
// res12: String = "repeatrepeat"
```
but it may through exceptions
```scala
Await.result(program(ApiFuture)("a"), 1.milli)
// java.util.concurrent.TimeoutException: Futures timed out after [1 millisecond]
// 	at scala.concurrent.impl.Promise$DefaultPromise.ready(Promise.scala:259)
// 	at scala.concurrent.impl.Promise$DefaultPromise.result(Promise.scala:263)
// 	at scala.concurrent.Await$.$anonfun$result$1(package.scala:223)
// 	at scala.concurrent.BlockContext$DefaultBlockContext$.blockOn(BlockContext.scala:57)
// 	at scala.concurrent.Await$.result(package.scala:146)
// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(more-monads.md:155)
// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(more-monads.md:155)
```

```scala
Await.result(program(ApiFuture)("b"), timeout)
// java.lang.Exception: Can't repeat negatively
// 	at repl.MdocSession$MdocApp$ApiFuture$.repeat(more-monads.md:123)
// 	at repl.MdocSession$MdocApp$ApiFuture$.repeat(more-monads.md:114)
// 	at repl.MdocSession$MdocApp$$anonfun$program$1.apply(more-monads.md:26)
// 	at repl.MdocSession$MdocApp$$anonfun$program$1.apply(more-monads.md:25)
// 	at scala.concurrent.Future.$anonfun$flatMap$1(Future.scala:307)
// 	at scala.concurrent.impl.Promise.$anonfun$transformWith$1(Promise.scala:41)
// 	at scala.concurrent.impl.CallbackRunnable.run(Promise.scala:64)
// 	at java.util.concurrent.ForkJoinTask$RunnableExecuteAction.exec(ForkJoinTask.java:1402)
// 	at java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:289)
// 	at java.util.concurrent.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1056)
// 	at java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1692)
// 	at java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:175)
```

```scala
Await.result(program(ApiFuture)("c"), timeout)
// java.util.NoSuchElementException: key not found: c
// 	at scala.collection.immutable.Map$Map2.apply(Map.scala:227)
// 	at repl.MdocSession$MdocApp$ApiFuture$$anonfun$get$2.apply$mcI$sp(more-monads.md:119)
// 	at repl.MdocSession$MdocApp$ApiFuture$$anonfun$get$2.apply(more-monads.md:116)
// 	at repl.MdocSession$MdocApp$ApiFuture$$anonfun$get$2.apply(more-monads.md:116)
// 	at scala.concurrent.Future$.$anonfun$apply$1(Future.scala:659)
// 	at scala.util.Success.$anonfun$map$1(Try.scala:255)
// 	at scala.util.Success.map(Try.scala:213)
// 	at scala.concurrent.Future.$anonfun$map$1(Future.scala:292)
// 	at scala.concurrent.impl.Promise.liftedTree1$1(Promise.scala:33)
// 	at scala.concurrent.impl.Promise.$anonfun$transform$1(Promise.scala:33)
// 	at scala.concurrent.impl.CallbackRunnable.run(Promise.scala:64)
// 	at java.util.concurrent.ForkJoinTask$RunnableExecuteAction.exec(ForkJoinTask.java:1402)
// 	at java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:289)
// 	at java.util.concurrent.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1056)
// 	at java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1692)
// 	at java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:175)
```

## Eval

Another monad we haven't looked at before is Eval.
This provides a type class to abstract evaluation.
We have discussed this before but let's recap.

In scala we have eager evaluation
```scala
val x = 3
// x: Int = 3
```
lazy or deferred evaluation
```scala
lazy val y = 3
```

and repeated evaluation

```scala
def z = 3
```

The Eval typeclass had instances for each case

```scala
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
```scala
program(ApiEval)("a")
// res13: Eval[String] = cats.Eval$$anon$4@77de01e6
program(ApiEval)("b")
// res14: Eval[String] = cats.Eval$$anon$4@5691b383
program(ApiEval)("c")
// res15: Eval[String] = cats.Eval$$anon$4@5186e427
```
Nothing is evaluated here. 
The program creates an Eval that we still need to evaluate.
The Eval is a description of a program in which some computations are suspended.
Unlike with ApiFuture, we did not see any printed messages yet.

```scala
program(ApiEval)("a").value
// getting a
// res16: String = "repeatrepeat"
```
```scala
program(ApiEval)("b").value
// java.lang.Exception: Can't repeat negatively
// 	at repl.MdocSession$MdocApp$ApiEval$$anonfun$repeat$3.apply(more-monads.md:214)
// 	at repl.MdocSession$MdocApp$ApiEval$$anonfun$repeat$3.apply(more-monads.md:214)
// 	at cats.Eval$.loop$1(Eval.scala:336)
// 	at cats.Eval$.cats$Eval$$evaluate(Eval.scala:368)
// 	at cats.Eval$FlatMap.value(Eval.scala:307)
// 	at repl.MdocSession$MdocApp$$anonfun$4.apply(more-monads.md:240)
// 	at repl.MdocSession$MdocApp$$anonfun$4.apply(more-monads.md:240)
```
```scala
program(ApiEval)("c").value
// java.util.NoSuchElementException: key not found: c
// 	at scala.collection.immutable.Map$Map2.apply(Map.scala:227)
// 	at repl.MdocSession$MdocApp$ApiEval$$anonfun$get$3.apply$mcI$sp(more-monads.md:210)
// 	at repl.MdocSession$MdocApp$ApiEval$$anonfun$get$3.apply(more-monads.md:207)
// 	at repl.MdocSession$MdocApp$ApiEval$$anonfun$get$3.apply(more-monads.md:207)
// 	at cats.Always.value(Eval.scala:173)
// 	at cats.Eval$.loop$1(Eval.scala:347)
// 	at cats.Eval$.cats$Eval$$evaluate(Eval.scala:368)
// 	at cats.Eval$FlatMap.value(Eval.scala:307)
// 	at repl.MdocSession$MdocApp$$anonfun$5.apply(more-monads.md:250)
// 	at repl.MdocSession$MdocApp$$anonfun$5.apply(more-monads.md:250)
```
## IO
IO is part of cats effect. 
It combines some of the capabilities of Either, Eval and Future (and more)
in a functional way.

For a change we will wrap an existing API.
By wrapping ApiFuture with IO we can make it a little better behaved
```scala
import cats.effect._

implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
// contextShift: ContextShift[IO] = cats.effect.internals.IOContextShift@55d5e587
  
object ApiIO extends Api[IO] {
    def get(key:String):IO[Int] = IO.fromFuture(IO(ApiFuture.get(key)))
    def repeat(n:Int):IO[String] = IO.fromFuture(IO(ApiFuture.repeat(n)))
        
}
```

We can use this with our program
```scala
program(ApiIO)("a")
// res17: IO[String] = Bind(
//   Async(
//     cats.effect.internals.IOBracket$$$Lambda$6086/1418597590@339494c6,
//     false
//   ),
//   <function1>
// )
program(ApiIO)("b")
// res18: IO[String] = Bind(
//   Async(
//     cats.effect.internals.IOBracket$$$Lambda$6086/1418597590@17f96805,
//     false
//   ),
//   <function1>
// )
program(ApiIO)("c")
// res19: IO[String] = Bind(
//   Async(cats.effect.internals.IOBracket$$$Lambda$6086/1418597590@3fdd0bf, false),
//   <function1>
// )
```
Nothing is evaluated here.
The program creates an IO that we still need to evaluate.

The IO is a description of a program in which some computations are suspended.

Unlike with ApiFuture, we did not see any printed messages yet.

Unlike Eval our program can run computations in another thread.

We have different options to evaluate the IO. 
The `attempt` function causes the IO executed with errors accumulated in an Either

```scala
program(ApiIO)("a").unsafeRunSync()
// getting a
// res20: String = "repeatrepeat"
program(ApiIO)("b").attempt.unsafeRunSync()
// getting b
// res21: Either[Throwable, String] = Left(
//   java.lang.Exception: Can't repeat negatively
// )
program(ApiIO)("c").attempt.unsafeRunSync()
// getting c
// res22: Either[Throwable, String] = Left(
//   java.util.NoSuchElementException: key not found: c
// )
```
## Using IO directly

We could also use IO directly instead of wrapping ApiFuture.
We use `IO.delay` to suspend a computation, similar to `Eval.defer`. Or just use `IO.apply` or call directly like a constructor `IO(...)`.
We use `IO.pure` for an immediate value, similar to `Eval.now`

```scala
object ApiIODirect extends Api[IO] {
    private val store:Map[String,Int] = Map("a" -> 2, "b" -> 0)
    def get(key:String):IO[Int] = IO{
        println(s"getting $key")
        Thread.sleep(1000)        
        store(key)
    }
    def repeat(n:Int):IO[String] = 
        if(n > 0) IO.pure("repeat" * n) 
        else IO.delay[String](throw new Exception("Can't repeat negatively")) 
}
```
We can use this with our program
```scala
program(ApiIODirect)("a")
// res23: IO[String] = Bind(Delay(<function0>), <function1>)
program(ApiIODirect)("b")
// res24: IO[String] = Bind(Delay(<function0>), <function1>)
program(ApiIODirect)("c")
// res25: IO[String] = Bind(Delay(<function0>), <function1>)
```
and evaluate as before

```scala
program(ApiIODirect)("a").unsafeRunSync()
// getting a
// res26: String = "repeatrepeat"
program(ApiIODirect)("b").attempt.unsafeRunSync()
// getting b
// res27: Either[Throwable, String] = Left(
//   java.lang.Exception: Can't repeat negatively
// )
program(ApiIODirect)("c").attempt.unsafeRunSync()
// getting c
// res28: Either[Throwable, String] = Left(
//   java.util.NoSuchElementException: key not found: c
// )
```


Note that the following four expressions are all identical
```scala
IO(println("suspended"))
// res29: IO[Unit] = Delay(<function0>)
IO{
    println("suspended")
}
// res30: IO[Unit] = Delay(<function0>)
IO.apply(println("suspended"))
// res31: IO[Unit] = Delay(<function0>)
IO.delay(println("suspended"))
// res32: IO[Unit] = Delay(<function0>)
```
