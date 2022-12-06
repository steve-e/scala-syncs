# Scala implicits

## Implicit parameters

### Without implicits
First a function without implicit arguments

```scala
case class Greeter(greeting:String)

def functionWith2ArgumentLists(name:String)(greeter:Greeter):String = s"${greeter.greeting} $name"
```

We can call this like so

```scala
val casual = Greeter("Hello")
// casual: Greeter = Greeter("Hello")
functionWith2ArgumentLists("Steve")(casual)
// res0: String = "Hello Steve"
functionWith2ArgumentLists("Fred")(casual)
// res1: String = "Hello Fred"
functionWith2ArgumentLists("Mary")(casual)
// res2: String = "Hello Mary"
```

We have to pass the greeting each time explicitly. 
This is repetitive, and worse, it also means if we want to change the greeting we need change it everywhere it is referenced.

### Reimplemented with implicits
```scala
def functionWithImplicits(name:String)(implicit greeter:Greeter):String = s"${greeter.greeting} $name"
```

We can use this as follows

```scala
implicit val informal = Greeter("Hi")
// informal: Greeter = Greeter("Hi")

functionWithImplicits("Leo")
// res3: String = "Hi Leo"
functionWithImplicits("Carlos")
// res4: String = "Hi Carlos"
functionWithImplicits("Eduard")
// res5: String = "Hi Eduard"
```

## Implicit conversions

### Implicit conversion methods
Define a new type `Identifiable` and a function to use it `sameIdentitiy` 
```scala
trait Identifiable { def id:String }

def sameIdentitiy(idA:Identifiable, idB:Identifiable):Boolean = idA.id == idB.id
```

We can use them like so

```scala
case class Address(number:String, street:String, town:String, postcode:String)  extends Identifiable {
    override def id = s"postcode number"
}


val whinging = Address("1","High St", "Little Whinging", "LW1 0RB")
// whinging: Address = Address("1", "High St", "Little Whinging", "LW1 0RB")
val wallop = Address("1","High St", "Little Wallop", "SO20 1HA")
// wallop: Address = Address("1", "High St", "Little Wallop", "SO20 1HA")

sameIdentitiy(whinging, whinging)
// res6: Boolean = true
sameIdentitiy(whinging, wallop)
// res7: Boolean = true
```

What about a class that does not implement identifiable?
```scala
case class Person(givenName:String, familyName:String)
val stan = Person("Stanley","Laurel")
// stan: Person = Person("Stanley", "Laurel")
val ollie = Person("Oliver","Hardy")
// ollie: Person = Person("Oliver", "Hardy")
```

```scala
sameIdentitiy(stan, stan)

// error: type mismatch;
//  found   : repl.MdocSession.MdocApp.Person
//  required: repl.MdocSession.MdocApp.Identifiable
// sameIdentitiy(stan, stan)
//               ^^^^
// error: type mismatch;
//  found   : repl.MdocSession.MdocApp.Person
//  required: repl.MdocSession.MdocApp.Identifiable
// sameIdentitiy(stan, stan)
//                     ^^^^
```

Implicit conversion to the rescue

```scala
implicit def identifiablePerson(person:Person):Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
}


sameIdentitiy(stan, stan)
// res9: Boolean = true
sameIdentitiy(stan, ollie)
// res10: Boolean = false
sameIdentitiy(stan, wallop)
// res11: Boolean = false
```

### Implicit conversion with implicit class
An alternative to a conversion method, is to create a new implicit class.

```scala
implicit class RichPerson(person:Person) {
    def asIdentifiable:Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
    } 
}
```

```scala
sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
// res12: Boolean = false
sameIdentitiy(stan.asIdentifiable, wallop)
// res13: Boolean = false
```

