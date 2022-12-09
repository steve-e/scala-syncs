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

```scala
val t1 = (1,2)
// t1: (Int, Int) = (1, 2)
val t2 = (5,10)
// t2: (Int, Int) = (5, 10)

def concatenate(a:(Int, Int), b:(Int, Int)):(Int, Int, Int, Int) =
    (a._1, a._2, b._1, b._2)

concatenate(t1, t2)
// res0: (Int, Int, Int, Int) = (1, 2, 5, 10)
```
Great. What if we wanted to concatenate two 3-tuples?
```scala
val a = (1,2,3)
// a: (Int, Int, Int) = (1, 2, 3)
val b = (5, 10, 15)
// b: (Int, Int, Int) = (5, 10, 15)
```
```scala
concatenate(a, b)
// error: type mismatch;
//  found   : (Int, Int, Int)
//  required: (Int, Int)
// concatenate(a, b)
//             ^
// error: type mismatch;
//  found   : (Int, Int, Int)
//  required: (Int, Int)
// concatenate(a, b)
//                ^
```

One way to fix that is to overload the function concatenate

```scala
def concatenate(a:(Int, Int, Int), b:(Int, Int, Int)):(Int, Int, Int, Int, Int, Int) =
    (a._1, a._2, a._3, b._1, b._2, b._3)
    
concatenate(a, b)
// res2: (Int, Int, Int, Int, Int, Int) = (1, 2, 3, 5, 10, 15)
```
Method overloading is used a lot in the math library. 
For example `max` is implemented for various numeric types. 
Here are implementations equivalent to what the library does for Int and Double
```scala
def max(a: Int, b: Int): Int = if (a >= b) a else b

max(2, 4)
// res3: Int = 4
```
But we have not reimplemented max for Float or Double, so that will fail
```scala
max(2.1, 199/99.0)
// error: type mismatch;
//  found   : Int(199)
//  required: ?{def /(x$1: ? >: Double(99.0)): ?}
// Note that implicit conversions are not applicable because they are ambiguous:
//  both method int2long in object Int of type (x: Int)Long
//  and method int2float in object Int of type (x: Int)Float
//  are possible conversion functions from Int(199) to ?{def /(x$1: ? >: Double(99.0)): ?}
// max(2.1, 199/99.0)
//          ^^^
// error: type mismatch;
//  found   : Double(2.1)
//  required: Int
// max(2.1, 199/99.0)
//     ^^^
// error: overloaded method value / with alternatives:
//   (x: Int)Int <and>
//   (x: Char)Int <and>
//   (x: Short)Int <and>
//   (x: Byte)Int
//  cannot be applied to (Double)
// max(2.1, 199/99.0)
//          ^^^^
```

## Parametric Polymorphism

We can create a function that relies on a type parameter

```scala
def duplicate[T](t:T):(T, T) = (t,t)

duplicate(2)
// res5: (Int, Int) = (2, 2)
duplicate("both")
// res6: (String, String) = ("both", "both")
```

## Subtyping

We can define a type, and then have implementations in subclasses

```scala
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
// birds: List[Bird] = List(
//   repl.MdocSession$MdocApp$Bird@19a968e3,
//   repl.MdocSession$MdocApp$Owl@290bbb8a,
//   repl.MdocSession$MdocApp$Crow@71181254,
//   repl.MdocSession$MdocApp$Penguin@4eafcb5
// )

"\n"+birds.map(birdInfo).mkString("\n")
// res7: String = """
// birds can fly, tweet tweet
// owls can fly, Terwit Terwoo
// crows can fly, caw caw
// penguins cannot fly, """
```

## Putting it together to make a polymorphic data structure

A polymorphic Tree
```scala
sealed trait Tree[+T]

case object Empty extends Tree[Nothing]
case class Node[T](value:T,left:Tree[T] , right: Tree[T]) extends Tree[T] {
  override def productPrefix = "Tree"
}
```
A companion object with a couple ways to get a tree instance, and a map method

```scala
object Tree {
  def empty[T]:Tree[T] = Empty
  def apply[T](t:T, left:Tree[T] = empty, right: Tree[T] = empty):Tree[T] = Node(t, left, right)
  def map[A,B](tree:Tree[A])( f:A=>B):Tree[B] = tree match {
    case Empty => Empty
    case Node(a,left,right) => Node(f(a), map(left)(f), map(right)(f))
  }
  
   def foldLeft[A, B](fa: Tree[A], b: B)(f: (B, A) => B):B = fa match {
    case Empty => b
    case Node(a,left,right) => {
      val newB = f(b,a)
      val lb = foldLeft(left,newB)(f)
      val rb = foldLeft(right,lb)(f)
      rb
    }
  }
}
```

Create some trees
```scala
val nums:Tree[Int] = Tree(5,
  left = Tree(3),
  right = Tree(4,
    left = Tree(2)
  )
)
// nums: Tree[Int] = Tree(
//   5,
//   Tree(3, Empty, Empty),
//   Tree(4, Tree(2, Empty, Empty), Empty)
// )

val nonums = Tree.empty[Int]
// nonums: Tree[Int] = Empty
Tree.map(nums)(_ + 1)
// res8: Tree[Int] = Tree(
//   6,
//   Tree(4, Empty, Empty),
//   Tree(5, Tree(3, Empty, Empty), Empty)
// )
Tree.map(nums)(_ * 2)
// res9: Tree[Int] = Tree(
//   10,
//   Tree(6, Empty, Empty),
//   Tree(8, Tree(4, Empty, Empty), Empty)
// )
Tree.map(nonums)(_ + 1)
// res10: Tree[Int] = Empty


Tree.foldLeft(nums, 0)((b , x) => b + x)
// res11: Int = 14
Tree.foldLeft(nums, 0)(_ - _)
// res12: Int = -14
Tree.foldLeft(nums, 1)(_ * _)
// res13: Int = 120
Tree.foldLeft(nums, "")(_ + _)
// res14: String = "5342"
Tree.foldLeft(nums, List.empty[Int])((l,v) => v :: l)
// res15: List[Int] = List(2, 4, 3, 5)
```
