package example

import cats.effect.{ContextShift, IO}

import scala.concurrent.duration._

class UnsafeRunDeadLockTest extends PoolUsageSuite {
  import UnsafeRunDeadLock._

  val shortTimeout = 1.seconds
  val longTimeout  = 4 * shortTimeout

  ioTest("safe to run n tasks in parallel") {
    Foo.computeEC(4).use { ec =>
      implicit val cs: ContextShift[IO] = IO.contextShift(ec)
      terminates(shortTimeout)(runTasks(20)(blocking))
    }
  }

  ioTest("unsafe to run n tasks in parallel") {
    Foo.computeEC(4).use { ec =>
      implicit val cs: ContextShift[IO] = IO.contextShift(ec)
      blocked(longTimeout)(runTasks(20)(unsafeBlocking))
    }
  }

}
