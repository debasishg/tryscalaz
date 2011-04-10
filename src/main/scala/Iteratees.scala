package net.debasishg.domain.trade.dsl
import scalaz._, Scalaz._


import IterV._
object Iteratees {
  def heads(str: String): IterV[Char, Int] = {
    def step(str: String, acc: Int)(s: Input[Char]): IterV[Char, Int] = 
      if (str.isEmpty) Done(acc, s) // return the input for composability
      else
        s(el = e => if (e == str(0)) Cont(step(str.substring(1, str.length), acc + 1)) else Done(acc, s),
          empty = Cont(step(str, acc)),
          eof = Done(acc, EOF[Char]))

    Cont(step(str, 0))
  }

  def break[A, F[_]](pred: A => Boolean)(implicit mon: Monoid[F[A]], pr: Pure[F]): IterV[A, F[A]] = {
    def step(acc: F[A])(s: Input[A]): IterV[A, F[A]] =
      s(el = e => if (!pred(e)) Cont(step(acc |+| e.η[F])) else Done(acc, s),
        empty = Cont(step(acc)),
        eof = Done(acc, EOF[A]))

    Cont(step(∅[F[A]]))
  }

  def read_lines: IterV[Char, Either[List[String], List[String]]] = {
    def terminators =
      heads("\r\n") >>= 
        ((n: Int) => if (n == 0) heads("\n") else Done(n, IterV.Empty[Char]))

    def isBreak(c: Char) = c == '\n' || c == '\r'

    def lines_acc(acc: List[String]): IterV[Char, Either[List[String], List[String]]] =
      break[Char, List]((c: Char) => c == '\n' || c == '\r') >>= 
        ((s: List[Char]) =>
            terminators >>= ((n: Int) => check(acc, s.mkString, n) ))

    def check(acc: List[String], str: String, n: Int): IterV[Char, Either[List[String], List[String]]] =
      if (n == 0) Done(Left(acc), IterV.Empty[Char])
      else str match {
        case "" => Done(Right(acc), IterV.Empty[Char])
        case x => lines_acc(acc :+ x)
      }

    lines_acc(List.empty[String])
  }


  def deduplicate[T](l: List[T]): List[T] = {
    def deduplicate_acc[T](l: List[T], acc: List[T]): List[T] = l match {
      case x :: xs if acc contains x => deduplicate_acc(xs, acc)
      case x :: xs => deduplicate_acc(xs, acc :+ x)
      case Nil => acc
    }
    deduplicate_acc(l, List.empty[T])
  }
}
