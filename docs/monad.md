# FlatMap and Monads

## Functor recap and limitation

A functor is a type with a single parameter and a `map` function.

The cats definition is
```scala
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}
```
but we can consider types such as `List` and `Option` to be functors
directly

The map method allows us to transform the type in the functor context,
passing a simple function to `map`
```scala
val list = List("red", "green", "orange")
// list: List[String] = List("red", "green", "orange")

def wordLength(word:String):Int = word.length

list.map(wordLength)
// res0: List[Int] = List(3, 5, 6)
```
Sometimes we have a function that itself returns a functor

```scala
def vowels(word:String):List[String] = 
    "([aeiou])".r.findAllIn(word).toList
```
we can use that with `map` but the results may be difficult to use
```scala
list.map(vowels)
// res1: List[List[String]] = List(
//   List("e"),
//   List("e", "e"),
//   List("o", "a", "e")
// )
```



Monads can be considered to be an extension of Functor.
Functors and Monads can be used to model different ideas including
- containers, such as `List` or `Option`
- effects such as computations that may error with `Either`, or computations that are asynchronous with `IO`
- contexts such as being able to read from a config or write to a log

These might seem quite varied things, but we could consider them all 
as contexts, eg a `List` is a context of multiplicity, 
an `Either` is a context for possible failure.

A monad is a functor with two extra capabilities.
There is a function that put any type into the context, 
called `pure` (or it may be called `unit`).
There is a function flatten which takes a doubled context and makes it
into a single context, eg takes List(List(1, 2), List(7, 8, 9)) 
and flattens it to List(1, 2, 7, 8, 9)

