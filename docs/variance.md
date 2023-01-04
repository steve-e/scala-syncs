# Variance in scala type parameters

## Invariant types

By default, a type parameter is invariant.

```scala
trait Holder[T] {
    def value:T
}
case class Box[T](value:T) extends Holder[T]
val stringBox:Box[String] = Box("stuff")
// stringBox: Box[String] = Box("stuff")
```

A `String` is a subtype of `AnyRef`, a `Box` of `String` is a `Holder` of `String`
```scala
val anyRef:AnyRef = "a string is an any ref"
// anyRef: AnyRef = "a string is an any ref"
val stringHolder:Holder[String] = stringBox
// stringHolder: Holder[String] = Box("stuff")
```

but a Box of String is not a Box of AnyRef
```scala
val anyRefBox:Box[AnyRef] = stringBox
// error: type mismatch;
//  found   : repl.MdocSession.MdocApp.Box[String]
//  required: repl.MdocSession.MdocApp.Box[AnyRef]
// Note: String <: AnyRef, but class Box is invariant in type T.
// You may wish to define T as +T instead. (SLS 4.5)
// val anyRefBox:Box[AnyRef] = stringBox
//                             ^^^^^^^^^
```

In the case of `Box`, it seems that we could have made this assignment safely.
It would have meant we could get the value as an AnyRef, but it would really be a String.
A String is an AnyRef so there wouldn't be a problem.

## Covariance
The type parameter needs to be covariant for this kind of substitution to work
We can make a box Covariant by adding a variance annotation of `+`

```scala
trait CoHolder[+T] {
    def value:T
}
case class CoBox[+T](value:T) extends CoHolder[T]
val stringCoBox:CoBox[String] = CoBox("stuff")
// stringCoBox: CoBox[String] = CoBox("stuff")
```
now we can use our `Box` of `String` in place of a `Box` of `AnyRef`
```scala
val anyRefBox:CoBox[AnyRef] = stringCoBox
// anyRefBox: CoBox[AnyRef] = CoBox("stuff")

anyRefBox.value
// res1: AnyRef = "stuff"
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
anyBad.set(new Integer(123))

// get the stored value, which is typed at String but is an Integer!
val badlySetString:String = badString.get

// badlySetString now holds Integer 123 !!!
```
That would be very bad, and so BadBox is disallowed by the compiler

```scala
class BadBox[+T](var value:T) {
    def set(t:T):Unit = {
      value = t
    }
    def get:T = value  
}
// error: covariant type T occurs in contravariant position in type T of value value_=
// class BadBox[+T](var value:T) {
//                      ^^^^^
// error: covariant type T occurs in contravariant position in type T of value t
//     def set(t:T):Unit = {
//             ^^^
```

For a type parameter to be Covariant it must only appear as a "supplier" such as a value type or return type.

## Contravariance

If we want our `set` method but the box to not be invariant 
we can make it Contravariant.

```scala
class ContraBox[-T] {

 def set(t:T):Unit = {
      println(s"ContraBox receives [$t]")
    }
 override def toString:String = "ContraBox"   
}

val anyRefContraBox:ContraBox[AnyRef] = new ContraBox
// anyRefContraBox: ContraBox[AnyRef] = ContraBox
anyRefContraBox.set(new Integer(123))
// ContraBox receives [123]

val stringContraBox:ContraBox[String] = anyRefContraBox
// stringContraBox: ContraBox[String] = ContraBox
stringContraBox.set("I can set a string")
// ContraBox receives [I can set a string]
```
Notice that in this class that `T` does not appear as a value or
as a return type. 
For a type to be contravariant it 
can only appear as a "receiver", such as a parameter type of a method.

*If a type must appear both as a supplier and as a receiver, it must be invariant.*

## Using both Covariant and Contravariant in the same definition

A common case where both covariant and contravariant types appear is in function definitions.
This is a simplified version of the scala Function1 trait

```scala
trait MyFunction1[-T1, +R]  {
  def apply(v1: T1): R
  override def toString = "MyFunction1"
}
```
We can use it to define and declare functions
```scala
val myHolderBoolToBoxString:MyFunction1[CoHolder[Boolean],CoBox[String]] = new MyFunction1[CoHolder[Boolean],CoBox[String]] {
    def apply(b:CoHolder[Boolean]):CoBox[String] = if(b.value) CoBox("Yes!") else CoBox("No!")
}
// myHolderBoolToBoxString: MyFunction1[CoHolder[Boolean], CoBox[String]] = MyFunction1
val holderIn:CoHolder[Boolean] = CoBox(true) 
// holderIn: CoHolder[Boolean] = CoBox(true) 
val boxOut = myHolderBoolToBoxString(holderIn)
// boxOut: CoBox[String] = CoBox("Yes!")
```
We can assign our function to a new type, taking advantage of both
covariance and contravariance
```scala
val myBoxBoolToHolderRef: MyFunction1[CoBox[Boolean],CoHolder[AnyRef]] = myHolderBoolToBoxString
// myBoxBoolToHolderRef: MyFunction1[CoBox[Boolean], CoHolder[AnyRef]] = MyFunction1
val box = CoBox(false)
// box: CoBox[Boolean] = CoBox(false)
val holderOut = myBoxBoolToHolderRef(box)
// holderOut: CoHolder[AnyRef] = CoBox("No!")
```
This shows that
```scala
MyFunction1[CoBox[Boolean],CoHolder[AnyRef]]
```
is a supertype of 
```scala
MyFunction1[CoHolder[Boolean],CoBox[String]]
```

because CoBox is a subtype of CoHolder and in particular CoBox[String] is a subtype of CoHolder[AnyRef].
In the contravariant first parameter of MyFunction1 the subtype order is reversed.


Let's look at another example. Buying pizza

Payment methods
```scala
trait Payment {
def currency:String
}
trait Cash extends Payment

trait Card extends Payment {
    def number:String 
}

case class Visa(currency:String, number:String) extends Card      
case class MasterCard(currency:String, number:String) extends Card
```

Pizzas
```scala
trait Pizza
trait CheesePizza {
    def cheeses:List[String]
}
case object Margherita extends CheesePizza {
    val cheeses = List("mozerella","parmesan")
}
case object QuattroFormaggi extends CheesePizza {
    val cheeses = List("mozerella","parmesan","gorgonzola","taleggio")
}
```
Customer:
I will give you a Visa payment, if you will give me a cheese pizza

Pizzeria
I will accept a card payment and give you a margherita

```scala
object Customer {
 
    def buyPizzaWithVisa(buy:Visa => CheesePizza):Unit = {
        val pizza:CheesePizza = buy(Visa("gbp","1234 4321 3232 4411"))
        println(s"eating pizza with ${pizza.cheeses}")
    }
    
    def buyPizzaWithMasterCard(buy:MasterCard => CheesePizza):Unit = {
        val pizza:CheesePizza = buy(MasterCard("gbp","1234 4321 3232 4411"))
        println(s"eating pizza with ${pizza.cheeses}")
    }
}

object Pizzaria {
    def sellMargheritaPizza(card:Card):Margherita.type = {
        println(s"charging $card")
        Margherita
    }   
    
    def sellQuattroFormaggiPizza(card:Card):QuattroFormaggi.type = {
        println(s"charging $card")
        QuattroFormaggi
    }
}

Customer.buyPizzaWithVisa(Pizzaria.sellMargheritaPizza)
// charging Visa(gbp,1234 4321 3232 4411)
// eating pizza with List(mozerella, parmesan)
Customer.buyPizzaWithVisa(Pizzaria.sellQuattroFormaggiPizza)
// charging Visa(gbp,1234 4321 3232 4411)
// eating pizza with List(mozerella, parmesan, gorgonzola, taleggio)
Customer.buyPizzaWithMasterCard(Pizzaria.sellQuattroFormaggiPizza)
// charging MasterCard(gbp,1234 4321 3232 4411)
// eating pizza with List(mozerella, parmesan, gorgonzola, taleggio)
```
