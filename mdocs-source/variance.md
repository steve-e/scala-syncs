# Variance in scala type parameters

## Invariant types

By default, a type parameter is invariant.

```scala mdoc
trait Holder[T] {
    def value:T
}
case class Box[T](value:T) extends Holder[T]
val stringBox:Box[String] = Box("stuff")
```

A `String` is a subtype of `AnyRef`, a `Box` of `String` is a `Holder` of `String`
```scala mdoc
val anyRef:AnyRef = "a string is an any ref"
val stringHolder:Holder[String] = stringBox
```

but a Box of String is not a Box of AnyRef
```scala mdoc:fail
val anyRefBox:Box[AnyRef] = stringBox
```

In the case of `Box`, it seems that we could have made this assignment safely.

## Covariance
We can make a box Covariant by adding a variance annotation

```scala mdoc
trait CoHolder[+T] {
    def value:T
}
case class CoBox[+T](value:T) extends CoHolder[T]
val stringCoBox:CoBox[String] = CoBox("stuff")
```
now we can use our `Box` of `String` in place of a `Box` of `AnyRef`
```scala mdoc
val anyRefBox:CoBox[AnyRef] = stringCoBox
```

Why not always have covariant types?
Consider if we have a `set` method that receives a `T`. 
This would not compile, but what if it did?
```scala
// does not compile
class BadBox[+T](var value:T) {
    def set(t:T):Unit = {
      value = t
    }
    def get:T = value  
}
```
Given the above, the following would presumably compile,
storing an `Int` in a `Box` of `String`.
``` scala
// if the BadBox compiled, so would this
val badString:BadBox[String] = new BadBox("bad")

// covariant assignment would be allowed
val anyBad:BadBox[AnyRef] = badString

// call set with an AnyRef eg Int
anyBad.set(123)

// get the stored value, which is typed at String but is an Int!
val badlySetString:String = badString.get

// badlySetString now holds Int 123 !!!
```
That would be very bad, and so BadBox is disallowed by the compiler

```scala mdoc:fail
class BadBox[+T](var value:T) {
    def set(t:T):Unit = {
      value = t
    }
    def get:T = value  
}
```

For a type parameter to be Covariant it must only appear as a "supplier" such as a value type or return type.

## Contravariance

If we want our `set` method but the box to not be invariant 
we can make it Contravariant.

```scala mdoc
class ContraBox[-T] {

 def set(t:T):Unit = {
      println(s"ContraBox receives [$t]")
    }
 override def toString:String = "ContraBox"   
}

val anyRefContraBox:ContraBox[AnyRef] = new ContraBox
anyRefContraBox.set(new Integer(123))

val stringContraBox:ContraBox[String] = anyRefContraBox
stringContraBox.set("I can set a string")

```
Notice that in this class there that `T` does not appear as a value or
as a return type. For a type to be contravariant it 
can only appear as a "receiver", such as a parameter type of a method.

*If a type must appear both as a supplier and as a receiver, it must be invariant.*

## Using both Covariant and Contravariant in the same definition

A common case where both covariant and contravariant types appear is in function definitions.
This is a simplified version of the scala Function1 trait

```scala mdoc
trait MyFunction1[-T1, +R]  {
  def apply(v1: T1): R
  override def toString = "MyFunction1"
}

val myHolderBoolToBoxString:MyFunction1[CoHolder[Boolean],CoBox[String]] = new MyFunction1[CoHolder[Boolean],CoBox[String]] {
    def apply(b:CoHolder[Boolean]):CoBox[String] = if(b.value) CoBox("Yes!") else CoBox("No!")
}
val holderIn:CoHolder[Boolean] = CoBox(true) 
val boxOut = myHolderBoolToBoxString(holderIn)
val myBoxBoolToHolderRef: MyFunction1[CoBox[Boolean],CoHolder[AnyRef]] = myHolderBoolToBoxString
val box = CoBox(false)
val holderOut = myBoxBoolToHolderRef(box)

```
This shows that
```scala
MyFunction1[CoHolder[Boolean],CoBox[String]]
```
is a subtype of 
```scala
MyFunction1[CoBox[Boolean],CoHolder[AnyRef]] 
```

because CoBox is a subtype of CoHolder and in particular CoBox[String] is a subtype of CoHolder[AnyRef].
In the contravariant first parameter pf MyFunction1 the subtype order is reversed.
