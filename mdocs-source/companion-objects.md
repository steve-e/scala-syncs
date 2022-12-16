# More on Companion Objects

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
