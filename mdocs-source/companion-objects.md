# More on Companion Objects

## Access to private members

A class can have private members, as can an object
```scala mdoc
class SimpleOne {
    private val secret:Int = 42
}

object AlsoSimpleOne {
    private val alsoSecret:Boolean = true
}
```
A class cannot access the private members of an object

```scala mdoc:fail
class SimpleBad {
    private val secret:Int = 42
    private val badBoolean = AlsoSimpleTwo.alsoSecret    
}

object AlsoSimpleTwo {
    private val alsoSecret:Boolean = true
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
}

object ClassWithCompanion {
    val publicInt:Int = (new ClassWithCompanion).secret
    private val alsoSecret:Boolean = true
}
```

## Apply method

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
val early = new Hours(3)
early.time
val late = new Hours(23)
late.time
```
## Unapply Method

An unapply method can be defined on the companion object, to use in match expressions

```scala mdoc
class Holder(val s:String, val i:Int) {
    override def toString:String = s"Holder($s, $i)"
}

object Holder {
     def unapply(holder:Holder):Option[(String,Int)] = Some((holder.s, holder.i))
}
```
The unapply method is not called explicitly but is used in a partial function, in a case expression

```scala mdoc
def extractString(ar:AnyRef):String = ar match {
    case Some(a) => a.toString
    case Holder(name, age) => s"$name $age"
    case _ => "whatever"
}
 
val holder = new Holder("Hodor", 19)
extractString(holder)
extractString(Some(true))
extractString(List(true)) 

```
