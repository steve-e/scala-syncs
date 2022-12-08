# Polymorphism in scala

Polymorphism allows an instance of a type to take on multiple behaviours.
Polymorphism comes from the Greek meaning many shapes.
There different ways that this can be implemented in different languages.
We will look at some in scala

## Ad hoc Polymorphism

"Ad hoc" is a Latin phrase used in English to mean something created for a specific task. 
Despite the mixed Latin and Greek in the name, Ad hoc polymorphism is possibly the simplest.
It is essentially function overloading or operator over loading.

Suppose we wanted to concatenate two 2-tuples.
We could write a method to do that.

```scala mdoc
val t1 = (1,2)
val t2 = (5,10)

def concatenate(a:(Int, Int), b:(Int, Int)):(Int, Int, Int, Int) =
    (a._1, a._2, b._1, b._2)

concatenate(t1, t2)
   
```
Great. What if we wanted to concatenate two 3-tuples?
```scala mdoc
val a = (1,2,3)
val b = (5, 10, 15)
```
```scala mdoc:fail
concatenate(a, b)
```

One way to fix that is to overload the function concatenate

```scala mdoc
def concatenate(a:(Int, Int, Int), b:(Int, Int, Int)):(Int, Int, Int, Int, Int, Int) =
    (a._1, a._2, a._3, b._1, b._2, b._3)
    
concatenate(a, b)
```
Method overloading is used a lot in the math library. 
For example `max` is implemented for various numeric types. 
Here are implementations equivalent to what the library does for Int and Double
```scala mdoc
  def max(a: Int, b: Int): Int = if (a >= b) a else b

max(2, 4)

```
But we have not reimplemented max for Float or Double, so that will fail
```scala mdoc:fail
max(2.1, 199/99.0)
```

## Parametric Polymorphism

We can create a function that relies on a type parameter

```scala mdoc

def duplicate[T](t:T):(T, T) = (t,t)

duplicate(2)
duplicate("both")

```

## Subtyping

We can define a type, and then have implementations in subclasses

```scala mdoc

class Bird {
    def name = getClass.getSimpleName.toLowerCase
    def canFly:Boolean = true
    def sing:String = "tweet tweet"
}

class Owl extends Bird {
    override def sing = "Terwit Terwoo"
}

class Crow extends Bird {
    override def sing = "caw caw"
}

class Penguin extends Bird {
    override def canFly = false
    override def sing = ""
}

def birdInfo(b:Bird):String = {
    val fly = if(b.canFly) "can fly" else "cannot fly"
    s"${b.name}s $fly, ${b.sing}" 

}
val birds = List(new Bird, new Owl, new Crow, new Penguin)

"\n"+birds.map(birdInfo).mkString("\n")
```
