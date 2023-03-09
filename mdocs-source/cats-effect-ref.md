# Cats effect concurrency control

We have looked at scala concurrency and also cats effect several times including Fiber.
This time we will look at some advanced concurrency features in cats-effect 2.1.2.
Note that cats-effect is currently at 3.4.8 but Apache Spark 3.2.0 forces us to use a lower version.
The later versions of cats-effect include more concurrecny features

## Concurrent updates

The following program creates 10 futures that simultaneously append to a list, updating the reference.
We are using a `Thread.sleep` to encourage some non-deterministic behaviour.

This is program is broken and produces an incorrect result.

```scala mdoc
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

val ints = (1 to 10).toList

var l = List.empty[Int]
val bad = Future.traverse(ints){i => Future {
        Thread.sleep(1)
        l = i :: l
        }
    }
  
Await.ready(bad, 10.seconds)
l
l.size
```

The program is non-deterministic, but it usually produces a list with less than 10 elements even though the list was
appended 10 times.
This problem is called a "lost update". 
It happens because more than one thread gets the list at the same time.
The dangerous code is this
```scala
l = i :: l
```
Each thread gets the list, updates its local copy and then sets the new value to the reference.
The last one to write wins, overwriting the other concurrent updates.

There are various ways to fix this.
One way is to use an `AtomicReference`

## AtomicReference

Java includes some thread safe mutable objects in its concurrency utils.
One of these is `AtomicReference`.
This class holds a reference to an object.
The reference can be mutated in a thread safe way.

The `AtomicReference` ensures visibility between threads, so that each sees the latest updated value.
This behaviour is similar to a volatile reference.

It also allows safely updating the reference, by letting the client code determine if an update has occurred since the
last read.
This could be implemented with locks or synchronised blocks, but in fact uses a single check and set instruction, which
has better performance.

The basic operation of `AtomicReference` is `public final boolean compareAndSet(V expect, V update) `.
This method takes a value to set and an expected existing value.
It sets the reference to hold the `update` value if and only if the reference currently hold the `expect` value.
It returns true if the update succeeded.

This is usually executed in a while loop.
The implementation of `updateAndGet` that we use below is as follows
```java
    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }
```

The following re-writes the first program to use an `AtomicReference` to the list.

```scala mdoc:silent
import java.util.concurrent.atomic.AtomicReference

val listAtomicReference = new AtomicReference(List.empty[Int])
```
```scala mdoc
val f = Future.traverse(ints){i => Future {
    listAtomicReference.updateAndGet((list: List[Int]) => {
      Thread.sleep(1)
      i :: list
    })
  }}
Await.ready(f, 110.seconds)
listAtomicReference.get()
listAtomicReference.get().size
```

This now works as desired producing a list of 10 elements.
Due to the non-determinism of the program the list is not in order.

This is correct code, but it is not functional in style.
The `AtomicReference` allows mutation, and the `Futures` start executing as soon as they are created.

## Cats effect `Ref`

A `cats.effect.concurrent.Ref` is a functional wrapper around an `AtomicReference`.
We can use this to re-write our program in functional style.

```scala mdoc
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.effect._

implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

val program = for {
  ref <- Ref.of[IO,List[Int]](List.empty[Int])
  _ <- ints.parTraverse{ 
        i => ref.updateAndGet(list => i::list).start  
    }
  l <- ref.get
}  yield l
```
That defines a functional program, with no side effects, so far.

Now lets call a side-effecting unsafe method to calculate the result
```scala mdoc
val resultList = program.unsafeRunSync()
resultList.size
```
This works as expected

## Deferred

Cats effect includes another type for safely setting a value in a concurrent context.
This type is `Deferred` which is created without a value and can be set only once,
by calling `complete`.
If `complete` is called more than once it produces a failed `F[_]`.
The value can be retrieved by calling `get`.

`Deferred` can be thought of as a functional version of `Promise`

This code calls `complete` after each update of the `Ref`.
As most of these calls will fail, we recover by calling `attempt` which converts to an `IO` of `Either` with the failure as a `Left`.
We don't care at this point whether it is a success or not so we ignore the result using `void`.
```scala mdoc
import cats.effect.concurrent.Deferred

def updateAndComplete(i:Int,
                       d: Deferred[IO,List[Int]],
                       ref : Ref[IO,List[Int]]
                     ):IO[Unit] = {
  ref.updateAndGet(l => i::l)
    .flatMap(l => d.complete(l).attempt.void)  

}

val deferredExample = for {
  d <- Deferred[IO,List[Int]]
  ref <- Ref.of[IO,List[Int]](List.empty[Int])
  _ <- ints.parTraverse(updateAndComplete(_,d,ref))
  l <- d.get
}  yield l

deferredExample.unsafeRunSync()

```
This successfully returns a non-deterministic result.

