package syncs.typeclasses
import simulacrum._

@typeclass trait AnnotatedMonoid[A] {
  def empty:A
  @op("|+|", alias = true) def combine(x: A, y: A): A
}

