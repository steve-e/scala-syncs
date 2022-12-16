# More on Companion Objects

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
//     private val badBoolean = AlsoSimpleTwo.alsoSecret    
//                              ^^^^^^^^^^^^^^^^^^^^^^^^
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
}

object ClassWithCompanion {
    val publicInt:Int = (new ClassWithCompanion).secret
    private val alsoSecret:Boolean = true
}
```
