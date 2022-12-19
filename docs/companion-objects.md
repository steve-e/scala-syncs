# More on Companion Objects
Companion objects are one of the characteristic language features of Scala.
Values defined in any object are only initialised once, so they can be used for defining constants.
The can also be used for defining methods that are not specific to an instance of a class.

Here we will look at access control, apply and unapply methods, and implicits

## Access to private members

A class can have private members, as can an object
```scala
class SimpleOne {
    private val secret:Int = 42
}

object AlsoSimpleOne {
    private val alsoSecret:Boolean = true
}
```
A class cannot access the private members of an object

```scala
class SimpleBad {
    private val secret:Int = 42
    private val badBoolean = AlsoSimpleTwo.alsoSecret    
}

object AlsoSimpleTwo {
    private val alsoSecret:Boolean = true
}
// error: value alsoSecret in object AlsoSimpleTwo cannot be accessed in object repl.MdocSession.MdocApp.AlsoSimpleTwo
//     private val goodBoolean = ClassWithCompanion.alsoSecret    
//                                                  ^^^^^^^^^^
```

An object cannot access the private members of a class

```scala
class SimpleThree {
    private val secret:Int = 42
}

object AlsoSimpleBadThree {
    private val badInt:Int = (new SimpleThree).secret
    private val alsoSecret:Boolean = true
}
// error: value secret in class SimpleThree cannot be accessed in repl.MdocSession.MdocApp.SimpleThree
//     private val badInt:Int = (new SimpleThree).secret
//                              ^^^^^^^^^^^^^^^^^^^^^^^^
```
If the class and object have the same name and are in the same file, we have a companion object.
Now the class and object have access to each other's private members

```scala
class ClassWithCompanion {
    private val secret:Int = 42
    private val goodBoolean = ClassWithCompanion.alsoSecret    
}

object ClassWithCompanion {
    val publicInt:Int = (new ClassWithCompanion).secret
    private val alsoSecret:Boolean = true
}
```

## Apply method

An apply method on an object can be called without using its name.
The call looks like a construct call without new.
This technique is usually used for exactly that purpose, as a succinct factory method. 

```scala
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
// early: Hours = Hours : 3
early.time
// res2: String = "3 am"
val late = Hours(23)
// late: Hours = Hours : 23
late.time
// res3: String = "11 pm"
```
## Unapply Method

An unapply method can be defined on the companion object.
It can be used to extract values from an object and is used in match expressions

```scala
class Holder(val s:String, val i:Int) {
    override def toString:String = s"Holder($s, $i)"
}

object Holder {
     def unapply(holder:Holder):Option[(String,Int)] = Some((holder.s, holder.i))
}
```
The unapply method is not called explicitly but is used in a partial function, in a case expression

```scala
def extractString(ar:AnyRef):String = ar match {
    case Some(a) => a.toString
    case Holder(name, age) => s"$name $age"
    case _ => "whatever"
}
 
val holder = new Holder("Hodor", 19)
// holder: Holder = Holder(Hodor, 19)
extractString(holder)
// res4: String = "Hodor 19"
extractString(Some(true))
// res5: String = "true"
extractString(List(true)) 
// res6: String = "whatever"
```
## Implicits resolution and companion objects

A companion object is considered in the language spec to be "derived" from its companion class.
This means that the compiler can find an implicit  type for a class if one is defined in the companion object.

We will test with a simple implicit parameter. 
First we will create a trait.
```scala
trait Printer[T] {
    def print(t:T):String
}
```
We can define a  method to use it.
```scala
def showIt[T](t:T)(implicit p:Printer[T]):String
    = p.print(t)
```

Then we can try it out with an implicit defined in the same scope
```scala
case class Foo(value:String)

implicit val fooPrinter = new Printer[Foo] {
    def print(f:Foo) = f.value.toUpperCase
}
// fooPrinter: AnyRef with Printer[Foo] = repl.MdocSession$MdocApp$$anon$1@41011500

val foo1 = Foo("keep the noise down")
// foo1: Foo = Foo("keep the noise down")

showIt(foo1)
// res7: String = "KEEP THE NOISE DOWN"
```

If the implicit is defined in some other object instead of locally, it cannot be found.


```scala
case class Bar(value:String)

object Another {
    implicit val barPrinter = new Printer[Bar] {
        def print(b:Bar) = b.value.toUpperCase
    }
}

val bar1 = Bar("please be quiet")
showIt(bar1)


// error: could not find implicit value for parameter p: repl.MdocSession.MdocApp.Printer[repl.MdocSession.MdocApp.Bar]
// showIt(bar1)
// ^^^^^^^^^^^^
```

Define the implicit in the companion object, and then it can be found
```scala
case class Bar(value:String)

object Bar {
    implicit val barPrinter = new Printer[Bar] {
        def print(b:Bar) = b.value.toUpperCase
    }
}

val bar1 = Bar("please be quiet")
// bar1: Bar = Bar("please be quiet")
showIt(bar1)
// res9: String = "PLEASE BE QUIET"
```
