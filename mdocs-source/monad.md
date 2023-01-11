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

The `map` method allows us to transform the type in the functor context,
passing a simple function to `map`
```scala mdoc

val list = List("red", "green", "orange")

def wordLength(word:String):Int = word.length

list.map(wordLength)

```
Sometimes we have a function that itself returns a result in a functor context

Eg this function vowel returns a list of vowels in a word
```scala mdoc
val AnyVowel = "([aeiou])".r

def vowels(word:String):List[String] = 
    AnyVowel.findAllIn(word).toList

```
we can use that with `map`
```scala mdoc
list.map(vowels)
```
now we have a list of lists, which may not be what we wanted.

If we want to know the vowels in our list of words, 
then calling map is a little awkward.

If all we have is a functor, that is only a `map` method, then we are stuck.


List has some additional methods beyond just `map`.
There are two things we could do.

Call `flatten` on the result
```scala mdoc
list.map(vowels).flatten
```
or, equivalently, use `flatMap` instead of `map`
```scala mdoc
list.flatMap(vowels)
```

## Monads

Monads can be considered to be an extension of Functor.

A monad is a functor with two extra capabilities.
There is a function that puts any type into the context,
called `pure` (or it may be called `unit`).
There is a function `flatten` which takes a doubled context and makes it
into a single context, eg takes `List(List(1, 2), List(7, 8, 9))`
and flattens it to `List(1, 2, 7, 8, 9)`

As demonstrated above, `flatMap` is equivalent to calling `flatten` after `map`,
so monads also have a `flatMap`.

*Note that in cats there is a FlatMap type class that only has `flatMap` and not `pure`.*

Actually, `flatten` can be defined in terms of `flatMap`, by passing in identity.

```scala mdoc
List(List(1, 2, 3), List(4, 5)).flatten
List(List(true, true, true), List(false, false)).flatMap(l => l)
```

Monads can be used to model different concepts including
- containers, such as `List` or `Option`
- effects such as computations that may error with `Either`, or computations that are asynchronous with `IO`
- contexts such as being able to read from a config or write to a log

These might seem quite varied things, but we could consider them all 
as contexts, eg a `List` is a context of multiplicity, 
an `Either` is a context for possible failure.

They could also all be considered as effects, eg the effect of multiplicity, or the effect of reading from a config.

## Monads and for comprehensions

A simple for comprehension with multiple generators is de-sugared to a series of `flatMap` and `map` calls.

```scala mdoc
val words = List("Visa", "NASA")

for {
  word <- words  
  character <- word
} yield character.isUpper

```
is de-sugared to
```scala mdoc
words
  .flatMap(word =>
    word
      .map(character => character.isUpper)
  )

```
