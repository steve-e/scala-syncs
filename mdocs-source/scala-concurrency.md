# Scala concurrency introduction

This document gives a quick tour of some concurrency and parallelization options in scala.

The code snippets demonstrate JVM and scala features. 
They also demonstrate some basic features of concurrent programs.

These examples do not represent best practice. 
For example, it is not good practice to use Thread.sleep to encourage another thread to complete.


## Concurrency landscape in scala

### JVM
Scala runs on the Java Virtual Machine.
The basic concurrency model of the JVM therefore applies to Scala. 
At least for us. 
There is also Scala.js that compiles down to javascript and runs on a javascript engine.
The concurrency model is quite different. We will not discuss this further.

Java provides various inbuilt low level concurrency control methods.
These are made available in scala.
These are:
- call `wait` on any AnyRef (or Object in java). This causes the thread to block
- call `notify` on any AnyRef, this causes any threads waiting on the AnyRef to become runnable
- call `synchronized` on any AnyRef passing in a code block to be run exclusively
- call `synchronized` global function to synchronize on the enclosing instance
- `@volatile` annotation

Here is a small example showing use of `wait` and `notify`. 
These methods must be called in a synchronized block.

```scala mdoc
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.DurationInt

object LockC

val future = Future {
  println("Waiting on LockC")
  LockC.synchronized {
    LockC.wait()    
    println("Finished waiting on LockC")
  } 
  
}
 println("Notifying LockC")
LockC.synchronized {
  LockC.notify()
}
Await.ready(future, 2.second)
```
The output does not appear in declaration order. 
Instead, it prints
1. Waiting on LockC
2. Notifying LockC
3. Finished waiting on LockC
This is because the `wait` method blocks until `notify` is called on the object.

The `@volatile` annotation ensures that a thread gets the latest copy
of a variable, even if it is updated in a different thread.
This annotation is needed because the JVM memory model does not guarantee that
a thread will always read the latest updated value.

```scala mdoc:reset
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.DurationInt

var a = "init a"
@volatile var b = "init b"

object LockV
val f1 = Future {
  LockV.synchronized(LockV.wait())
  a = a + " updated in f1"
  b = b + " updated in f1"
}
val f2 = Future {
  LockV.synchronized(LockV.wait())  
  a = a + " updated in f2"
  b = b + " updated in f2"
}
LockV.synchronized(LockV.notifyAll())
Await.ready(f1, 3.second)
Await.ready(f2, 4.second)
println(a)
println(b)
```
This program is non-deterministic, but it has several times printed
1. init a updated in f2
2. init b updated in f1 updated in f2
The b variable is volatile and has got the latest update. 
The a variable is not volatile and has it seems f2 did not get the update from f1.


Usage of these utilities is little documented in scala.
This is probably because these are low level utils dating from the 1990s and better options are now available.
Probably the best way to understand them is to read java resources.

### Java concurrency utils

The java pacakge `java.util.concurrent` has a number of useful classes.
These were added in the early 2000s and are more useful than the low level utils.

All of these are directly usable in scala. 
Some are also thinly wrapped by the scala standard library.
Others are wrapped by features of cats effect.

There are some thread safe mutable collections such as ConcurrentHashMap.

There are also thread safe mutable variables such as AtomicInteger

Here is a simple example using a countdown latch.
This class allows you to await a fixed number of notifications.
```scala mdoc:reset
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.DurationInt
import java.util.concurrent.CountDownLatch

val latch = new CountDownLatch(2)

val future = Future {
 println("Waiting on latch")
 latch.await()  
 println("Got latch")
}

Thread.sleep(100)

println("Counting down once")
latch.countDown()

println("Counting down a second time")
latch.countDown()

Thread.sleep(100)

Await.ready(future, 2.second)
```

There are many other lock classes, such as Semaphore and CyclicBarrier.

### Scala Future and Promise

We have seen that `Future` can be used to run a block of code in a thread pool.
We have also seen that it forms a monad allowing us to map functions and sequence operations 
with `flatMap` or a `for` comprehension.


Promise is closely related to Future and allows communication across thread boundaries.

Inter thread communication with Promise
```scala mdoc:reset
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.DurationInt

val p = Promise[String]()
val f = p.future

val fx = Future {
    println("await result")
    val result = Await.result(f, 2000.millis)
    println(s"Got [$result]")
}

println("main completes future")
p.success("Hello from main thread")
println("main awaits future")
Await.ready(fx, 2400.millis)

```

### Deadlock
The following example shows an example of deadlock.
Two threads are waiting on locks. 
But each thread is awaiting the other thread to release a lock!
Neither thread can progress, and the future times out.

This example uses future, but similar programs can be written using Object locks or `java.util.concurrent` locks
```scala mdoc:reset
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.DurationInt

object LockA
object LockB

val p2 = Promise[String]()
val f2 = p2.future


Future {
    LockB.synchronized{
    println("future synchronized on LockB")
    LockA.synchronized{
      println("future synchronized on LockA")
      try {
        val result = Await.result(f2, 5.seconds)
        println(s"Got [$result]")
      } catch {
        case e:Throwable =>
          println(s"failed [${e.getMessage}]")
          throw e
      }
    }
    println("future  released LockA")
  }
  println("future  released LockB")
}

println("Future created")

LockA.synchronized{
  println("main synchronized on LockA")

  LockB.synchronized {
    println("main synchronized on LockB")
    p2.success("Hello from main thread")
  }
  print("main released LockB")

}
println("main released LockA")

```
## Mentions of higher level facilities

We won't have time to go through these in detail, but they deserve at least a mention.

### Cats effect IO

Cats effect is a library of functional utilities for concurrency and parallelism.
As the name suggests it is built on top of the cats library.
It uses many cats type classes such as Functor, Monad, Applicative, Monoid, etc 

Cats effect provides its own type classes such as `Sync`, `Async` and `Timer`.



### IO

Cats effect provides its own core of implementation which is `IO`.

This can be used as to define asynchronous tasks. 
It can be used to wrap Futures. 
By using `map`, `flatMap` etc, programs can be constructed.
These are then evaluated eg by calling `unsafeRunSync`.
This causes the program to be evaluated by an interpreter.
The interpreter can efficiently run tasks without submitting each to a thread pool.

We can explore IO more later on.

### Akka Actors

Akka provides a message passing model of concurrency.
This allows concurrent operations with very few threads.

### Monix

Monix is another functional library for concurrency. 
It has interfaces for cats. 
It also has some interfaces with java concurrency types such as the Observable interface for reactive streams.

### ZIO

Another functional library, with overlap with cats.

