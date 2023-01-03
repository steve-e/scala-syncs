# Map and foreach

## A map recap
The map method is defined on Functor, and also directly on some core scala collections.

```scala
val nums: List[Int] = List(1, 2, 3)
// nums: List[Int] = List(1, 2, 3)
val doubled: List[Double] = nums.map(x => x * 2.0)
// doubled: List[Double] = List(2.0, 4.0, 6.0)

val maybeString:Option[String] = Some("")
// maybeString: Option[String] = Some("")
val maybeBoolean:Option[Boolean] = maybeString.map(_.isEmpty)
// maybeBoolean: Option[Boolean] = Some(true)
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

```scala
val moreNums: List[Int] = List(1, 2, 3, 4)
// moreNums: List[Int] = List(1, 2, 3, 4)
moreNums.foreach(x => println(x))
// 1
// 2
// 3
// 4
```

# De-sugaring for comprehensions

A very simple for comprehension that yields a result can be de-sugared to a map.

This:
```scala
for {
    i <- nums
} yield i * 2.0
// res1: List[Double] = List(2.0, 4.0, 6.0)
```
is equivalent to 
```scala
nums.map(_ * 2.0)
// res2: List[Double] = List(2.0, 4.0, 6.0)
```

A very simple for comprehension that does not yield a result can be de-sugared to a foreach.

This:
```scala
for {
  i <- nums
} println(i)
// 1
// 2
// 3
```
is equivalent to
```scala
nums.foreach(println)
// 1
// 2
// 3
```
