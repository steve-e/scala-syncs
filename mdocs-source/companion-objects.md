# More on Companion Objects
Companion objects are one of the characteristic language features of Scala.

Here we will look at access control, apply and unapply methods, and implicits

## Access to private members

A class can have private members, as can an object
```scala mdoc
class SimpleOne {
    private val secret:Int = 42
}

object AlsoSimpleOne {
    private val secretBoolean:Boolean = true
}
```
A class cannot access the private members of an object

```scala mdoc:fail
class SimpleBad {
    private val secret:Int = 42
    private val badBoolean = AlsoSimpleTwo.secretBoolean    
}

object AlsoSimpleTwo {
    private val secretBoolean:Boolean = true
}
```

An object cannot access the private members of a class

```scala mdoc:fail
class SimpleThree {
    private val secret:Int = 42
}

object AlsoSimpleBadThree {
    private val badInt:Int = (new SimpleThree).secret
    private val alsoSecret:Boolean = true
}
```
If the class and object have the same name and are in the same file, we have a companion object.
Now the class and object have access to each other's private members

```scala mdoc
class ClassWithCompanion {
    private val secret:Int = 42
    private val goodBoolean = ClassWithCompanion.alsoSecret    
}

object ClassWithCompanion {
    val publicInt:Int = (new ClassWithCompanion).secret
    private val alsoSecret:Boolean = true
    
    def doSomething(c:ClassWithCompanion) = c.secret
}
```

## Apply method

An apply method on an object can be called without using its name.
The call looks like a constructor call without `new`.
This technique is usually used for exactly that purpose, as a succinct factory method. 

```scala mdoc
class Hours(hours:Int) {

    override def toString:String = s"Hours : $hours"
    
    val time:String = {
        if(hours >= 0) {
            if(hours < 13) s"$hours am"
            else if (hours < 24 ) s"${hours - 12} pm"
            else "the future"
        }
        else "the past"
    }
}

object Hours {
    def apply(hours:Int):Hours = new Hours(hours)
}
val early = Hours(3)
early.time
val late = Hours(23)
late.time
```
## Unapply Method

An unapply method can be defined on the companion object.
It can be used to extract values from an object and is used in match expressions

A successful match returns a Some, a failed match returns a None
```scala mdoc
class Holder(val s:String, val i:Int) {
    override def toString:String = s"Holder($s, $i)"
}

object Holder {
     def unapply(holder:Holder):Option[(String,Int)] = {
        if(holder.i > 0) Some((holder.s,holder.i)) else None
     }
}
```
The unapply method is not usually called explicitly but is used in a partial function, in a case expression

```scala mdoc
val example = new Holder("Hodor", 19)
val badExample = new Holder("Circe", -1)

val Holder(exName, exAge) = example

// use a variable defined in the pattern above
exName.toUpperCase

val Some((a, i)) = Holder.unapply(example)


def extractString(ar:AnyRef):String = ar match {
    case Some(a) => a.toString
    case Holder(name, age) => s"$name $age"
    case _ => "whatever"
}
 
extractString(example)
extractString(badExample)
extractString(Some(true))
extractString(List(true)) 

```
## Implicits resolution and companion objects

A companion object is considered in the language spec to be "derived" from its companion class.
This means that the compiler can find an implicit  type for a class if one is defined in the companion object.

We will test with a simple implicit parameter. 
First we will create a trait.
```scala mdoc
trait Printer[T] {
    def print(t:T):String
    override def toString:String = "Printer"
}
```
We can define a  method to use it.
```scala mdoc

def showIt[T](t:T)(implicit p:Printer[T]):String
    = p.print(t)
```

Then we can try it out with an implicit defined in the same scope
```scala mdoc
case class Foo(value:String)

implicit val fooPrinter = new Printer[Foo] {
    def print(f:Foo) = f.value.toUpperCase
}

val foo1 = Foo("keep the noise down")

showIt(foo1)
```

If the implicit is defined in some other object instead of locally, it cannot be found.


```scala mdoc:fail
case class Bar(value:String)

object Another {
    implicit val barPrinter = new Printer[Bar] {
        def print(b:Bar) = b.value.toUpperCase
    }
}

val bar1 = Bar("please be quiet")
showIt(bar1)


```

Define the implicit in the companion object, and then it can be found
```scala mdoc
case class Bar(value:String)

object Bar {
    implicit val barPrinter = new Printer[Bar] {
        def print(b:Bar) = b.value.toUpperCase
    }
}

val bar1 = Bar("please be quiet")
showIt(bar1)

```

We can import an implicit from another scope


```scala mdoc
case class Bar2(value:String)

object Another2 {
    implicit val barPrinter = new Printer[Bar2] {
        def print(b:Bar2) = b.value.toUpperCase
    }
}

val bar2 = Bar2("don't make a noise")
import Another2._
showIt(bar2)

```
or use `showIt` without an implicit by passing a `Printer` explicitly

```scala mdoc
case class Bar3(value:String)

object Another3 {
    implicit val barPrinter = new Printer[Bar3] {
        def print(b:Bar3) = b.value.toUpperCase
    }
}

val bar3 = Bar3("whisper")

showIt(bar3)(Another3.barPrinter)

```
