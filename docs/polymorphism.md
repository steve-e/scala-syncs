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

One way to fix that is to overload the function `concatenate`

```scala
def concatenate(a:(Int, Int, Int), b:(Int, Int, Int)):(Int, Int, Int, Int, Int, Int) =
    (a._1, a._2, a._3, b._1, b._2, b._3)
    
concatenate(a, b)
// res2: (Int, Int, Int, Int, Int, Int) = (1, 2, 3, 5, 10, 15)
```
Method overloading is used a lot in the math library. 
For example `max` is implemented for various numeric types. 
Here are simple implementations for Int and Float
```scala
def max(a: Int, b: Int): Int = if (a >= b) a else b
def max(a: Float, b: Float): Float = if (a >= b) a else b

max(2, 4)
// res3: Int = 4
max(22/7.0F, 3.14F)
// res4: Float = 3.142857F
```
But we have not implemented max for Double, so that will fail
```scala
max(2.1D, 199/99.0D)
// error: overloaded method value max with alternatives:
//   (a: Float,b: Float)Float <and>
//   (a: Int,b: Int)Int
//  cannot be applied to (Double, Double)
// max(2.1D, 199/99.0D)
// ^^^
```

## Parametric Polymorphism

We can create a function that relies on a type parameter

```scala
def duplicate[T](t:T):(T, T) = (t,t)

duplicate(2)
// res6: (Int, Int) = (2, 2)
duplicate("both")
// res7: (String, String) = ("both", "both")
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
//   repl.MdocSession$MdocApp$Bird@4d404d46,
//   repl.MdocSession$MdocApp$Owl@48516198,
//   repl.MdocSession$MdocApp$Crow@2c662bc5,
//   repl.MdocSession$MdocApp$Penguin@3f2d4284
// )

"\n"+birds.map(birdInfo).mkString("\n")
// res8: String = """
// birds can fly, tweet tweet
// owls can fly, Terwit Terwoo
// crows can fly, caw caw
// penguins cannot fly, """
```

## Putting it together to make a polymorphic data structure

A polymorphic Tree. This can hold different types. 
Tree is a type constructor for types such as Tree[Int] or Tree[String]. 
The instances of the Tree[T] are of the subtypes, Empty and Node[T]

```scala
sealed trait Tree[+T]

case object Empty extends Tree[Nothing]
case class Node[T](value:T,left:Tree[T] , right: Tree[T]) extends Tree[T] {
  override def productPrefix = "Tree"
}
```
A companion object with a couple ways to get a tree instance, and map and fold methods. 
These methods have type parameters, and are parametrically polymorphic

```scala
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
  
  def unsafeLowest(t:Tree[String]):String = fold(t, List.empty[String])((l,v) => v :: l).min
  def unsafeLowest(t:Tree[Int]):Int = fold(t, List.empty[Int])((l,v) => v :: l).min
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

val doubles: Tree[Double] = Tree(5.0, left = Tree(3.0), right = Empty)
// doubles: Tree[Double] = Tree(5.0, Tree(3.0, Empty, Empty), Empty)
val strings: Tree[String] = Tree("branch", left = Tree("leaf"), right = Empty)
// strings: Tree[String] = Tree("branch", Tree("leaf", Empty, Empty), Empty)
```
And make some function calls.

Transform the tree values with map.
```scala
Tree.map(nums)(_ + 1)
// res9: Tree[Int] = Tree(
//   6,
//   Tree(4, Empty, Empty),
//   Tree(5, Tree(3, Empty, Empty), Empty)
// )
Tree.map(nums)(_ * 2)
// res10: Tree[Int] = Tree(
//   10,
//   Tree(6, Empty, Empty),
//   Tree(8, Tree(4, Empty, Empty), Empty)
// )
Tree.map(nonums)(_ + 1)
// res11: Tree[Int] = Empty
```
Convert the Tree[A] to a new type B with fold.
```scala
Tree.fold(nums, 0)((b , x) => b + x)
// res12: Int = 14
Tree.fold(nums, 0)(_ - _)
// res13: Int = -14
Tree.fold(nums, 1)(_ * _)
// res14: Int = 120
Tree.fold(strings, "")(_ + _)
// res15: String = "branchleaf"
Tree.fold(nums, "")(_ + _)
// res16: String = "5342"
Tree.fold(nums, List.empty[Int])((l,v) => v :: l)
// res17: List[Int] = List(2, 4, 3, 5)
```
Use an overloaded tree method
```scala
Tree.unsafeLowest(nums)
// res18: Int = 2
Tree.unsafeLowest(strings)
// res19: String = "branch"
```
```scala
Tree.unsafeLowest(doubles)
// error: overloaded method value unsafeLowest with alternatives:
//   (t: repl.MdocSession.MdocApp.Tree[Int])Int <and>
//   (t: repl.MdocSession.MdocApp.Tree[String])String
//  cannot be applied to (repl.MdocSession.MdocApp.Tree[Double])
// Tree.unsafeLowest(doubles)
// ^^^^^^^^^^^^^^^^^
```
