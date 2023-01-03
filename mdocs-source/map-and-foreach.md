# Map and foreach

## A map recap
The map method is defined on Functor, and also directly on some core scala collections.

```scala mdoc

val nums: List[Int] = List(1, 2, 3)
val doubled: List[Double] = nums.map(x => x * 2.0)

val maybeString:Option[String] = Some("")
val maybeBoolean:Option[Boolean] = maybeString.map(_.isEmpty)

```
The map function on Option is something like 
```scala 
class Option[+A] {
    def map[B](f: A => B): Option[B]
}
```
The Option of A has a map method that takes a function converting A to B and returns an Option of B.

The more general Functor trait has

```scala
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}
```
The  map takes an F of A and a function converting A to B and returns an F of B.

In all cases the map method returns a new value with the transformed elements.


## Foreach

Like map, foreach can be used to iterate over a collection.
However, it does not return a new object, but only unit.
Furthermore the function it takes also returns unit

```scala
def foreach[U](f: A => U):Unit = {
    if (!isEmpty) f(this.get)
  }
```

Foreach is useful only for side-effecting code. 

```scala mdoc
val moreNums: List[Int] = List(1, 2, 3, 4)
moreNums.foreach(x => println(x))
```

# De-sugaring for comprehensions

A very simple for comprehension that yields a result can be de-sugared to a map.

This:
```scala mdoc
for {
    i <- nums
} yield i * 2.0
```
is equivalent to 
```scala mdoc
nums.map(_ * 2.0)
```

A very simple for comprehension that does not yield a result can be de-sugared to a foreach.

This:
```scala mdoc
for {
  i <- nums
} println(i)
```
is equivalent to
```scala mdoc
nums.foreach(println)
```
