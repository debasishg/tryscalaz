package net.debasishg.domain.trade.dsl

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class IterateesSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._
  import IterV._

  implicit val StreamEnumerator = new Enumerator[Stream] {
    def apply[E, A](e: Stream[E], i: IterV[E, A]): IterV[E, A] = e match {
      case Stream() => i
      case x #:: xs => i.fold(done = (_, _) => i, cont = k => apply(xs, k(El(x))))
    }
  }

  describe("heads") {

    import Iteratees._
    it("should report number of matches with input stream") {
      (heads("abcd")(Stream('a', 'b', 'c', 'e', 'f'))).run should equal(3)
      (heads("ab")(Stream('a', 'b', 'c', 'e', 'f'))).run should equal(2)
      (heads("abcd")(Stream('a', 'b', 'c', 'd', 'd'))).run should equal(4)
      (heads("abcd")(Stream('b', 'b', 'c', 'd', 'd'))).run should equal(0)
    }

    it("should compose as a monad and report the sum of matches") {
      // as a monad
      val m = heads("abc") >>= ((b: Int) => heads("xyz") map (b1 => (b + b1)))
      m(Stream('a', 'b', 'c', 'x', 'y', 'p', 'q')).run should equal(5)
      m(Stream('a', 'b', 'c', 't', 'y', 'p', 'q')).run should equal(3)
      m(Stream('o', 'b', 'c', 't', 'y', 'p', 'q')).run should equal(0)
      m(Stream('o', 'b', 'c', 'x', 'y', 'p', 'q')).run should equal(0)
    }
  }

  describe("break") {
    it("should break at the failure of specified predicate") {
      import Iteratees._
      val isBreak: Char => Boolean = (c => c == '\n' || c == '\r')

      val b = break[Char, List](isBreak)
      b(Stream('a', 'b', 'c', 'd', '\r', 'e', 'f', '\n')).run should equal(List('a', 'b', 'c', 'd'))
      b(Stream('a', 'b', 'c', 'd', '\n')).run should equal(List('a', 'b', 'c', 'd'))
      b(Stream('\r', 'a', 'b', 'c', 'd', '\r', 'e', 'f', '\n')).run should equal(List())

      val stripLineFeed = b >>= ((s: List[Char]) => head)

      val c = stripLineFeed >>= ((h: Option[Char]) => head)
      c(Stream('a', 'b', 'c', 'd', '\r', 'e', 'f', '\n')).run should equal(Some('e'))
      c(Stream('\r', 'a', 'b', 'c', 'd', '\r', 'e', 'f', '\n')).run should equal(Some('a'))
      c(Stream('a', 'b', 'c', 'd', '\n')).run should equal(None)

      val d = stripLineFeed >>= ((h: Option[Char]) => heads("efgh"))
      d(Stream('a', 'b', 'c', 'd', '\r', 'e', 'f', '\n')).run should equal(2)
    }
  }

  describe("read_lines") {
    it("should read lines breaking at newlines") {
      import Iteratees._
      read_lines(Stream('a', 'b', 'c', 'd', '\n', 'e', 'f', '\n')).run should equal(Left(List("abcd", "ef")))
      read_lines(Stream('a', 'b', 'c', 'd', '\n', 'e', 'f')).run should equal(Left(List("abcd")))
      read_lines(Stream('a', 'b', 'c', 'd', '\n', 'e', 'f', '\r', '\n')).run should equal(Left(List("abcd", "ef")))
      read_lines(Stream('a', 'b', 'c', 'd', '\n', '\n', 'e', 'f', '\r', '\n')).run should equal(Right(List("abcd")))
      read_lines(Stream('a', 'b', 'c', 'd', '\r', '\n', 'e', 'f', '\r', '\n')).run should equal(Left(List("abcd", "ef")))
    }
  }

  describe("nub") {
    import Iteratees._
    it("should remove duplicates") {
      deduplicate(List(1,2,2,3,4,5,1,4,6,8,3)) should equal(List(1,2,3,4,5,6,8))
    }
  }
}
