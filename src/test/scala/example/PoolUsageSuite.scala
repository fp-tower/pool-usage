package example

import cats.effect.{ContextShift, IO, Resource, Timer}
import org.scalatest.{Assertion, FunSuite, Matchers}

import scala.concurrent.{ExecutionContext, TimeoutException}
import scala.concurrent.duration._

class PoolUsageSuite extends FunSuite with Matchers {
  import PoolUsageSuite._

  def ioTest[A](name: String)(fa: IO[Assertion]): Unit =
    test(name) {
      fa.unsafeRunSync()
    }

  def terminates[A](duration: FiniteDuration)(fa: IO[A]): IO[Assertion] =
    fa.timeout(duration)(TestTimer, TestContextShift).attempt.map(_ should be('right))

  def blocked[A](duration: FiniteDuration)(fa: IO[A]): IO[Assertion] =
    fa.timeout(duration)(TestTimer, TestContextShift).attempt.map {
      case Left(_: TimeoutException) => succeed
      case Left(e)                   => fail(e)
      case Right(a)                  => fail(s"Terminates successfully with $a")
    }
}

object PoolUsageSuite {
  // This execution is only use to race tests with timeout such as we can check if they dead lock
  private val TestExecutionContext: ExecutionContext = Foo.computeEC(2).allocated.unsafeRunSync()._1
  private val TestTimer: Timer[IO]                   = IO.timer(TestExecutionContext)
  private val TestContextShift: ContextShift[IO]     = IO.contextShift(TestExecutionContext)
}
