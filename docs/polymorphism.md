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
//   repl.MdocSession$MdocApp$Bird@47f65167,
//   repl.MdocSession$MdocApp$Owl@27252395,
//   repl.MdocSession$MdocApp$Crow@2f8038bf,
//   repl.MdocSession$MdocApp$Penguin@3dcac793
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
Tree.map(nonums)(_ + 1)
// res9: Tree[Int] = Empty
```

Let's provide Functor and Foldable type classes
```scala
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
// treeFunctorFoldable: Functor[Tree] with Foldable[Tree] = repl.MdocSession$MdocApp$$anon$1@652392fb
```

Now we can use syntax to access various methods

From Functor

```scala
import cats.syntax.functor._
nums.map(_ * 2)
// res10: Tree[Int] = Tree(
//   10,
//   Tree(6, Empty, Empty),
//   Tree(8, Tree(4, Empty, Empty), Empty)
// )
nums.void
// res11: Tree[Unit] = Tree(
//   (),
//   Tree((), Empty, Empty),
//   Tree((), Tree((), Empty, Empty), Empty)
// )
nonums.map(_ * 42)
// res12: Tree[Int] = Empty
```

From Foldable
```scala
import cats.syntax.foldable._

nums.foldLeft(0)((b , x) => b + x)
// res13: Int = 14
nums.foldLeft(0)(_ - _)
// res14: Int = -14
nums.foldLeft(1)(_ * _)
// res15: Int = 120
nums.foldLeft("")(_ + _)
// res16: String = "5342"

nums.foldLeft(List.empty[Int])((l,v) => v :: l)
// res17: List[Int] = List(2, 4, 3, 5)
nums.foldRight(Eval.now(List.empty[Int]))((v,l) => l.map(v :: _)).value
// res18: List[Int] = List(3, 2, 4, 5)
nums.toList
// res19: List[Int] = List(5, 3, 4, 2)

nums.isEmpty
// res20: Boolean = false
nonums.isEmpty
// res21: Boolean = true

nums.get(0)
// res22: Option[Int] = Some(3)
nums.get(5)
// res23: Option[Int] = None
nonums.get(0)
// res24: Option[Int] = None
```
