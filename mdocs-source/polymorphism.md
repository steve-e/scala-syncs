# Polymorphism in scala

Polymorphism allows an instance of a type to take on multiple behaviours.
Polymorphism comes from the Greek meaning many shapes.
There are different ways that this can be implemented in different languages.
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

One way to fix that is to overload the function `concatenate`

```scala mdoc
def concatenate(a:(Int, Int, Int), b:(Int, Int, Int)):(Int, Int, Int, Int, Int, Int) =
    (a._1, a._2, a._3, b._1, b._2, b._3)
    
concatenate(a, b)
```
Method overloading is used a lot in the math library. 
For example `max` is implemented for various numeric types. 
Here are simple implementations for Int and Float
```scala mdoc
def max(a: Int, b: Int): Int = if (a >= b) a else b
def max(a: Float, b: Float): Float = if (a >= b) a else b

max(2, 4)
max(22/7.0F, 3.14F)

```
But we have not implemented max for Double, so that will fail
```scala mdoc:fail
max(2.1D, 199/99.0D)
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

## Putting it together to make a polymorphic data structure

A polymorphic Tree. This can hold different types. 
Tree is a type constructor for types such as Tree[Int] or Tree[String]. 
The instances of the Tree[T] are of the subtypes, Empty and Node[T]

```scala mdoc
sealed trait Tree[+T]

case object Empty extends Tree[Nothing]
case class Node[T](value:T,left:Tree[T] , right: Tree[T]) extends Tree[T] {
  override def productPrefix = "Tree"
}

```
A companion object with a couple ways to get a tree instance, and map and fold methods. 
These methods have type parameters, and are parametrically polymorphic

```scala mdoc
object Tree {
  def empty[T]:Tree[T] = Empty
  
  def apply[T](t:T, left:Tree[T] = empty, right: Tree[T] = empty):Tree[T] = Node(t, left, right)
  
  def map[A,B](tree:Tree[A])( f:A => B):Tree[B] = tree match {
    case Empty => Empty
    case Node(a,left,right) => Node(f(a), map(left)(f), map(right)(f))
  }
  
  def fold[A, B](fa: Tree[A], b: B)(f: (B, A) => B):B = fa match {
    case Empty => b
    case Node(a,left,right) => {
      val newB = f(b,a)
      val lb = fold(left,newB)(f)
      fold(right,lb)(f)      
    }
  }
}
```

Create some trees
```scala mdoc
val nums:Tree[Int] = Tree(5,
  left = Tree(3),
  right = Tree(4,
    left = Tree(2)
  )
)

val nonums = Tree.empty[Int]
```
And make some function calls.

Transform the tree values with map.
```scala mdoc
Tree.map(nums)(_ + 1)
Tree.map(nums)(_ * 2)
Tree.map(nonums)(_ + 1)
```
Convert the Tree[A] to a new type B with fold.
```scala mdoc
Tree.fold(nums, 0)((b , x) => b + x)
Tree.fold(nums, 0)(_ - _)
Tree.fold(nums, 1)(_ * _)
Tree.fold(nums, "")(_ + _)
Tree.fold(nums, List.empty[Int])((l,v) => v :: l)
```
