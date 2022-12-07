# Scala implicits

## Implicit or contextual parameters

### Without implicits
First a function without implicit arguments

```scala
case class Greeter(greeting:String)

def functionWith2ArgumentLists(name:String)(greeter:Greeter):String 
    = s"${greeter.greeting} $name"
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
By the way we could partially apply the function if we wanted to greet the same person multiple ways
```scala
val steveGreeter: Greeter => String = functionWith2ArgumentLists("Steve")
// steveGreeter: Greeter => String = <function1>
steveGreeter(casual)
// res3: String = "Hello Steve"
steveGreeter(Greeter("Yo"))
// res4: String = "Yo Steve"
```

We have to pass the greeting each time explicitly. 
This is repetitive, and worse, it also means if we want to change the greeting we need change it everywhere it is referenced.

### Reimplemented with implicits
```scala
def functionWithImplicit(name:String)(implicit greeter:Greeter):String 
    = s"${greeter.greeting} $name"
```

We can use this as follows

```scala
implicit val informal = Greeter("Hi")
// informal: Greeter = Greeter("Hi")

functionWithImplicit("Leo")
// res5: String = "Hi Leo"
functionWithImplicit("Carlos")
// res6: String = "Hi Carlos"
functionWithImplicit("Eduard")
// res7: String = "Hi Eduard"
```
The compiler has to search for a suitable implicit in various scopes.
If more than one implicit of the required type is found in the same scope of an implicit search then a compilation error occurs.

```scala
implicit val yo = Greeter("Yo")
implicit val greertings = Greeter("Greetings")

functionWithImplicit("Leo")
// error: ambiguous implicit values:
//  both value yo in object MdocApp of type => repl.MdocSession.MdocApp.Greeter
//  and value greertings in object MdocApp of type => repl.MdocSession.MdocApp.Greeter
//  match expected type repl.MdocSession.MdocApp.Greeter
// functionWithImplicit("Leo")(greertings)
// ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```
If there are too many implicits of the same type to use implicitly, 
then the function can be called passing the parameter explicitly.
```scala
implicit val yo = Greeter("Yo")
// yo: Greeter = Greeter("Yo")
implicit val greertings = Greeter("Greetings")
// greertings: Greeter = Greeter("Greetings")
functionWithImplicit("Leo")(greertings)
// res9: String = "Greetings Leo"
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
    override def id = s"$postcode $number"
}


val whinging = Address("1","High St", "Little Whinging", "LW1 0RB")
// whinging: Address = Address("1", "High St", "Little Whinging", "LW1 0RB")
val wallop = Address("1","High St", "Little Wallop", "SO20 1HA")
// wallop: Address = Address("1", "High St", "Little Wallop", "SO20 1HA")

sameIdentitiy(whinging, whinging)
// res10: Boolean = true
sameIdentitiy(whinging, wallop)
// res11: Boolean = false
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
It does not compile!

We could call a method to do the conversion for us...
```scala
def convertPersonToIdentifiable(person:Person):Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
}

sameIdentitiy(convertPersonToIdentifiable(stan), convertPersonToIdentifiable(ollie))
// res13: Boolean = false
```
... but it is verbose, repetitive and ugly.

Implicit conversion to the rescue!

```scala
implicit def identifiablePerson(person:Person):Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
}


sameIdentitiy(stan, stan)
// res14: Boolean = true
sameIdentitiy(stan, ollie)
// res15: Boolean = false
sameIdentitiy(stan, wallop)
// res16: Boolean = false
```
The compiler finds that the method requires Identifiable but is passed a Person.
The compiler then searches for `implicit Person => Identifiable` and finds `identifiablePerson`. 
It then wraps the person parameter in the conversion call, and that allows compilation to continue.
The program works as expected.

### Implicit conversion with implicit class
An alternative to a conversion method, is to create a new implicit class.
This can be used to apparently add methods to class. 

```scala
// For reference, this doesn't compile, yet ...

sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
sameIdentitiy(stan.asIdentifiable, wallop)

// error: value asIdentifiable is not a member of repl.MdocSession.MdocApp.Person
// sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
//               ^^^^^^^^^^^^^^^^^^^
// error: value asIdentifiable is not a member of repl.MdocSession.MdocApp.Person
// sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
//                                    ^^^^^^^^^^^^^^^^^^^^
// error: value asIdentifiable is not a member of repl.MdocSession.MdocApp.Person
// sameIdentitiy(stan.asIdentifiable, wallop)
//               ^^^^^^^^^^^^^^^^^^^
```
Define an implicit class. The name is not usually used in code after it is defined
```scala
implicit class PersonSyntax(person:Person) {
    def asIdentifiable:Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
    } 
}
```

Now the new `asIdentifiable` method can be called
```scala
sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
// res18: Boolean = false
sameIdentitiy(stan.asIdentifiable, wallop)
// res19: Boolean = false
```

