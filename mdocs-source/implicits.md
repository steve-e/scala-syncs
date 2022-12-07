# Scala implicits

## Implicit or contextual parameters

### Without implicits
First a function without implicit arguments

```scala mdoc
case class Greeter(greeting:String)

def functionWith2ArgumentLists(name:String)(greeter:Greeter):String 
    = s"${greeter.greeting} $name"
```

We can call this like so

```scala mdoc
val casual = Greeter("Hello")
functionWith2ArgumentLists("Steve")(casual)
functionWith2ArgumentLists("Fred")(casual)
functionWith2ArgumentLists("Mary")(casual)
```
By the way we could partially apply the function if we wanted to greet the same person multiple ways
```scala mdoc
val steveGreeter: Greeter => String = functionWith2ArgumentLists("Steve")
steveGreeter(casual)
steveGreeter(Greeter("Yo"))
```

We have to pass the greeting each time explicitly. 
This is repetitive, and worse, it also means if we want to change the greeting we need change it everywhere it is referenced.

### Reimplemented with implicits
```scala mdoc
def functionWithImplicit(name:String)(implicit greeter:Greeter):String 
    = s"${greeter.greeting} $name"
```

We can use this as follows

```scala mdoc
implicit val informal = Greeter("Hi")

functionWithImplicit("Leo")
functionWithImplicit("Carlos")
functionWithImplicit("Eduard")
```
The compiler has to search for a suitable implicit in various scopes.
If more than one implicit of the required type is found in the same scope of an implicit search then a compilation error occurs.

```scala mdoc:fail
implicit val yo = Greeter("Yo")
implicit val greetings = Greeter("Greetings")

functionWithImplicit("Leo")
```
If there are too many implicits of the same type to use implicitly, 
then the function can be called passing the parameter explicitly.
```scala mdoc
implicit val yo = Greeter("Yo")
implicit val greetings = Greeter("Greetings")
functionWithImplicit("Leo")(greetings)
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
    override def id = s"$postcode $number"
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

We could call a method to do the conversion for us...
```scala mdoc
def convertPersonToIdentifiable(person:Person):Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
}

sameIdentitiy(convertPersonToIdentifiable(stan), convertPersonToIdentifiable(ollie))
```
... but it is verbose, repetitive and ugly.

Implicit conversion to the rescue!

```scala mdoc

implicit def identifiablePerson(person:Person):Identifiable = new  Identifiable {
    override def id = s"${person.givenName} ${person.familyName}"
}


sameIdentitiy(stan, stan)
sameIdentitiy(stan, ollie)
sameIdentitiy(stan, wallop)
```
The compiler finds that the method requires Identifiable but is passed a Person.
The compiler then searches for `implicit Person => Identifiable` and finds `identifiablePerson`. 
It then wraps the person parameter in the conversion call, and that allows compilation to continue.
The program works as expected.

### Implicit conversion with implicit class
An alternative to a conversion method, is to create a new implicit class.
This can be used to apparently add methods to class. 

```scala mdoc:fail
// For reference, this doesn't compile, yet ...

sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
sameIdentitiy(stan.asIdentifiable, wallop)

```
Define an implicit class. The name is not usually used in code after it is defined
```scala mdoc
implicit class PersonSyntax(person:Person) {
    def asIdentifiable:Identifiable = new  Identifiable {
        override def id = s"${person.givenName} ${person.familyName}"
    } 
}
```

Now the new `asIdentifiable` method can be called
```scala mdoc

sameIdentitiy(stan.asIdentifiable, ollie.asIdentifiable)
sameIdentitiy(stan.asIdentifiable, wallop)

```

