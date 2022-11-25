# Functor

(extracts from https://ncatlab.org/nlab/show/functor and https://bartoszmilewski.com/2015/01/20/functors/)

- A functor is a mapping between categories. Given two categories, C and D, a functor F maps objects in C to objects in D

![](https://bartoszmilewski.files.wordpress.com/2015/01/functor.jpg)

- Given morphisms `f:X→Y, g:Y→Z`, and `h:X→Z`, declaring the triangle commutes amounts to declaring
```h=g∘f```.
In this case, for `F:C→D` to preserve the commutative triangle means

```F(h)=F(g)∘F(f)```
as depicted below

![](http://ncatlab.org/nlab/files/functor.jpg)

- In scala, Functors are usually implemented for a 1st-order-kinded type, F[_], eg List, Option, IO
