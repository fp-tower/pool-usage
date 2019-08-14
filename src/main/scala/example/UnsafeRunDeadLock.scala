package example

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.implicits._

import scala.concurrent.duration._

/**
  * Running n computations in parallel using unsafeRunSync will create a dead lock
  * when n >= number of CPU.
  * See https://gitter.im/typelevel/cats-effect?at=5d5417b7d03a7c63e6275c59
*/
object UnsafeRunDeadLock extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    1.to(20)
      .toList
      .parTraverse_(unsafeBlocking) // dead lock with unsafeBlocking but not with blocking
      .as(ExitCode.Success)

  def unsafeBlocking(i: Int): IO[Unit] =
    IO { blocking(i).unsafeRunSync() }

  def blocking(i: Int): IO[Unit] =
    ContextShift[IO].shift *> IO {
      println(s"Started $i")
      Thread.sleep(2.seconds.toMillis)
      println(s"Completed $i")
    }
}
