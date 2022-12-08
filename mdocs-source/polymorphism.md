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

## Putting it together to make a polymorphic data structure

A polymorphic Tree
```scala mdoc
sealed trait Tree[+T]

case object Empty extends Tree[Nothing]
case class Node[T](value:T,left:Tree[T] , right: Tree[T]) extends Tree[T] {
  override def productPrefix = "Tree"
}

```
A companion object with a couple ways to get a tree instance, and a map method

```scala mdoc
object Tree {
  def empty[T]:Tree[T] = Empty
  def apply[T](t:T, left:Tree[T] = empty, right: Tree[T] = empty):Tree[T] = Node(t, left, right)
  def map[A,B](tree:Tree[A])( f:A=>B):Tree[B] = tree match {
    case Empty => Empty
    case Node(a,left,right) => Node(f(a), map(left)(f), map(right)(f))
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
Tree.map(nums)(_ + 1)
Tree.map(nonums)(_ + 1)
```

Let's provide Functor and Foldable type classes
```scala mdoc
import cats._

implicit val treeFunctorFoldable:Functor[Tree] with Foldable[Tree] = new Functor[Tree] with Foldable[Tree] {
  override def map[A, B](fa: Tree[A])(f: A => B):Tree[B] =
    Tree.map(fa)(f)

  override def foldLeft[A, B](fa: Tree[A], b: B)(f: (B, A) => B):B = fa match {
    case Empty => b
    case Node(a,left,right) => {
      val newB = f(b,a)
      val lb = foldLeft(left,newB)(f)
      val rb = foldLeft(right,lb)(f)
      rb
    }
  }

  override def foldRight[A, B](fa: Tree[A], b: Eval[B])(f: (A, Eval[B]) => Eval[B]):Eval[B]
  =  fa match {
    case Empty => b
  case Node(a,left,right) =>
    val newB = f(a,b)
    val rb = foldRight(right,newB)(f)
    val lb = foldRight(left, rb)(f)
    lb
  }
}

```

Now we can use syntax to access various methods

From Functor

```scala mdoc
import cats.syntax.functor._
nums.map(_ * 2)
nums.void
nonums.map(_ * 42)
```

From Foldable
```scala mdoc
import cats.syntax.foldable._

nums.foldLeft(0)((b , x) => b + x)
nums.foldLeft(0)(_ - _)
nums.foldLeft(1)(_ * _)
nums.foldLeft("")(_ + _)

nums.foldLeft(List.empty[Int])((l,v) => v :: l)
nums.foldRight(Eval.now(List.empty[Int]))((v,l) => l.map(v :: _)).value
nums.toList

nums.isEmpty
nonums.isEmpty

nums.get(0)
nums.get(5)
nonums.get(0)
```
