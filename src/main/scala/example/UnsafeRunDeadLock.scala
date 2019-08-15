package example

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.implicits._

import scala.concurrent.duration._

object UnsafeRunDeadLockApp extends IOApp {
  import UnsafeRunDeadLock._

  def run(args: List[String]): IO[ExitCode] =
    runTasks(20)(unsafeBlocking).as(ExitCode.Success) // deadlock
//  runTasks(20)(blocking).as(ExitCode.Success) // completes
}

object UnsafeRunDeadLock {

  /**
    * unsafeRunSync can cause deadlocks when it runs IO with asynchronous boundary (shift)
    * This deadlock is due to unsafeRunSync using a latch to wait for termination
    * So if you have n threads blocked by unsafeRunSync where n is thread pool size
    * you would have no thread available to make progress (complete the IO)
    *
    * See discussion on gitter: https://gitter.im/typelevel/cats-effect?at=5d5417b7d03a7c63e6275c59
    */
  def runTasks(n: Int)(task: Int => IO[Unit])(implicit cs: ContextShift[IO]): IO[Unit] =
    1.to(n)
      .toList
      .parTraverse_(task)

  def unsafeBlocking(i: Int)(implicit cs: ContextShift[IO]): IO[Unit] =
    IO { blocking(i).unsafeRunSync() }

  def blocking(i: Int)(implicit cs: ContextShift[IO]): IO[Unit] =
    cs.shift *>
      IO {
        println(s"Started $i")
        Thread.sleep(100.milliseconds.toMillis)
        println(s"Completed $i")
      }
}
