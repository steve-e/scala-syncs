# Scala implicits

## Implicit or contextual parameters

### Without implicits
First a function without implicit arguments

```scala mdoc
case class Greeter(greeting:String)

def functionWith2ArgumentLists(name:String)(greeter:Greeter):String = s"${greeter.greeting} $name"
```

We can call this like so

```scala mdoc
val casual = Greeter("Hello")
functionWith2ArgumentLists("Steve")(casual)
functionWith2ArgumentLists("Fred")(casual)
functionWith2ArgumentLists("Mary")(casual)
```

We have to pass the greeting each time explicitly. 
This is repetitive, and worse, it also means if we want to change the greeting we need change it everywhere it is referenced.

### Reimplemented with implicits
```scala mdoc
def functionWithImplicit(name:String)(implicit greeter:Greeter):String = s"${greeter.greeting} $name"
```

We can use this as follows

```scala mdoc
implicit val informal = Greeter("Hi")

functionWithImplicit("Leo")
functionWithImplicit("Carlos")
functionWithImplicit("Eduard")
```

## Implicit conversions

### Implicit conversion methods
Define a new type `Identifiable` and a function to use it `sameIdentitiy` 
```scala mdoc

trait Identifiable { def id:String }

def sameIdentitiy(idA:Identifiable, idB:Identifiable):Boolean = idA.id == idB.id

```

We can use them like so

```scala mdoc
case class Address(number:String, street:String, town:String, postcode:String)  extends Identifiable {
    override def id = s"postcode number"
}


val whinging = Address("1","High St", "Little Whinging", "LW1 0RB")
val wallop = Address("1","High St", "Little Wallop", "SO20 1HA")

sameIdentitiy(whinging, whinging)
sameIdentitiy(whinging, wallop)
```

What about a class that does not implement identifiable?
```scala mdoc
case class Person(givenName:String, familyName:String)
val stan = Person("Stanley","Laurel")
val ollie = Person("Oliver","Hardy")
```

```scala mdoc:fail

sameIdentitiy(stan, stan)

```
It does not compile!

Implicit conversion to the rescue

```scala mdoc

implicit def identifiablePerson(person:Person):Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
}


sameIdentitiy(stan, stan)
sameIdentitiy(stan, ollie)
sameIdentitiy(stan, wallop)
```

### Implicit conversion with implicit class
An alternative to a conversion method, is to create a new implicit class.

```scala mdoc
implicit class RichPerson(person:Person) {
    def asIdentifiable:Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
    } 
}
```

```scala mdoc

sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
sameIdentitiy(stan.asIdentifiable, wallop)

```

