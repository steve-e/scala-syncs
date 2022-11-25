# What is a Category?


- A category is an embarrassingly simple concept. A category consists of objects and arrows that go between them, such that these relationships can be combined and include the “identity” relationship “is the same as.”
- More precisely, a category consists of a collection of objects and a collection of morphisms. Every morphism has a source object and a target object. If f is a morphism with x as its source and y as its target, we write
  ```
  f:x→y
  ```
  and we say that f is a morphism from x to y. 
  In a category, we can compose a morphism `g:x→y` and a morphism `f:y→z` to get a morphism `f∘g:x→z`. 
  Composition is associative and satisfies the left and right unit laws.


  ![](https://ncatlab.org/nlab/files/AssociativityDiagram.png)

  - A good example to keep in mind is the category *Set*, in which the objects are sets and a morphism `f:x→y` is a function from the set x to the set y. Here composition is the usual composition of functions.
  - Other categories in mathematics are Monoids, Groups, Graphs
  - There is nearly a category of Scala, in which objects are types, and morphisms are functions.

(bits taken from https://ncatlab.org/nlab/show/category https://bartoszmilewski.com/2014/11/04/category-the-essence-of-composition/)
