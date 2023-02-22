# Creating a custom type class

It is common to create a new type class instance.
For example if your project creates a new type, it may be useful to create a Monoid instance for it.
If the type is some kind of container, it may be useful to create a Monad instance for it.

It is not common to create a new type class.

We will create some today primarily so that we can get 
a better understanding of how type classes and their syntax work.

## What is a type class anyway?

> "A type class is an abstract, parameterized type that lets you add new behavior to any closed data type without using sub-typing."

_from the [Scala3 Book](https://docs.scala-lang.org/scala3/book/types-type-classes.html)_

Add new general behaviour to types without modifying them.
Create a new family of types with analogous behaviours.

## Create by hand

We start by defining a trait for some useful behaviour.
We will create a monoid.
```scala mdoc
trait HandMadeMonoid[A] {
    def empty:A
    def combine(x:A, y:A):A
}
```
We want to be able to define instances

```scala mdoc
implicit val addition = new HandMadeMonoid[Int]{
    val empty = 0
    def combine(x:Int, y:Int):Int = x + y
}
```

We then want to be able use them in an API

```scala mdoc
 def combineWithHMM[A:HandMadeMonoid](l:List[A]):A = {
    val M = HandMadeMonoid[A]
    l.fold(M.empty)(M.combine)
 }

```
We want to be able to summon them and use our methods and operations
```scala mdoc:fail
HandMadeMonoid[Int].combine(1, 2)

```
```scala mdoc:fail
combineWithHMM(List(1,3,5))
```
We need to provide a companion object with the required methods and a syntax op `|*|`
```scala mdoc
object HandMadeMonoid {
    /**
    * summon instance
    */
    def apply[A](implicit self:HandMadeMonoid[A]):HandMadeMonoid[A] = self
    
    trait Ops[A] {
        def typeClassInstance: HandMadeMonoid[A]
        def self: A
        /**
        * We have defined some syntax
        */
        def |*|(x:A):A = typeClassInstance.combine(self, x) 
    }
    
    object ops {
        implicit def toHandMadeMonoidOps[A](target:A)(implicit tc: HandMadeMonoid[A]):Ops[A] = new Ops[A]{
            val self = target
            val typeClassInstance = tc
        }
    }
}
```
Having defined this we can now run the code that didn't compile before,
as long as we import the implicits
```scala mdoc
import HandMadeMonoid.ops._

HandMadeMonoid[Int].combine(1, 2)
combineWithHMM(List(1,3,5))
```

and we can use the syntax op we defined
```scala mdoc
3 |*| 4

```

## Create with annotation
The simulacrum project automates the creation of type classes using macros and annotations.
After configuring build.sbt, it is only necessary to create a suitable trait and add the annotation.

This example defines a symbolic syntax `|+|` as an alias for the `combine` method.
```scala
package syncs.typeclasses
import simulacrum._

@typeclass trait AnnotatedMonoid[A] {
  def empty:A
  @op("|+|", alias = true) def combine(x: A, y: A): A
}
```
That is what the definition looks like but the actual code is defined in AnnotatedMonoid.scala


The trait can then be used to define type class instances

```scala mdoc
import simulacrum._
import syncs.typeclasses._

implicit val Additive: AnnotatedMonoid[Int] = new AnnotatedMonoid[Int] {
    val empty = 0
    def combine(x: Int, y: Int): Int = x + y
}
```

The type class and instance can then be used as expected.

We can summon the instance and use combine method

```scala mdoc
AnnotatedMonoid[Int].combine(4, 5)
```

Or we can import the ops  and use implicit conversions 
to allow using scala infix notation and the declared alias

```scala mdoc
import AnnotatedMonoid.ops._

2 combine 3

1 |+| 2

```
and use our monoid in APIs
```scala mdoc
 def combineWithAM[A:AnnotatedMonoid](l:List[A]):A = {
    val M = AnnotatedMonoid[A]
    l.fold(M.empty)(M.combine)
 }

combineWithAM(List(2,3,4))

implicit val muliplicative = new AnnotatedMonoid[Double] {
    val empty = 1.0
    def combine(x: Double, y: Double): Double = x * y
}

combineWithAM(List(2.0, 3.0, 4.0))
 
 

```

We can see the implicit conversion happening if we really want to.
```scala mdoc
val ops1: AnnotatedMonoid.Ops[Int] = 1
ops1.combine(2)
ops1.|+|(3)
```
The macro generates the type class boilerplate code starting from the 
annotated trait.

The generated code would look something like this

```scala

trait AnnotatedMonoid[A] {
  def empty:A
  def combine(x: A, y: A): A
}

object AnnotatedMonoid {
  def apply[A](implicit instance: AnnotatedMonoid[A]): AnnotatedMonoid[A] = instance

  trait Ops[A] {
    def typeClassInstance: AnnotatedMonoid[A]
    def self: A
    def combine(y: A): A = typeClassInstance.combine(self, y)
    def |+|(y: A): A = combine(y)
  }

  trait ToAnnotatedMonoidOps {
    implicit def toAnnotatedMonoidOps[A](target: A)(implicit tc: AnnotatedMonoid[A]): Ops[A] = new Ops[A] {
      val self = target
      val typeClassInstance = tc
    }
  }

  object nonInheritedOps extends ToAnnotatedMonoidOps

  trait AllOps[A] extends Ops[A] {
    def typeClassInstance: AnnotatedMonoid[A]
  }

  object ops {
    implicit def toAllAnnotatedMonoidOps[A](target: A)(implicit tc: AnnotatedMonoid[A]): AllOps[A] = new AllOps[A] {
      val self = target
      val typeClassInstance = tc
    }
  }
}
```
The Ops and AllOps traits look similar here but are different when the trait is part of a hierarchy. 
This allows greater flexibility.
