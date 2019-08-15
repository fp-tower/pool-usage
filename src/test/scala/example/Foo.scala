package example

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{TimeUnit, _}

import cats.effect.{IO, Resource, SyncIO}
import cats.~>

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

// copied and adapted from cats-effect IOApp
object Foo {

  def computeEC(threads: Int, name: Int => String = i => s"compute-$i"): Resource[IO, ExecutionContext] =
    fromExecutorService(
      Executors.newFixedThreadPool(
        threads,
        new ThreadFactory {
          val ctr = new AtomicInteger(0)
          def newThread(r: Runnable): Thread = {
            val back = new Thread(r)
            back.setName(name(ctr.getAndIncrement()))
            back.setDaemon(true)
            back
          }
        }
      )
    )

  def fromExecutorService(es: => ExecutorService): Resource[IO, ExecutionContext] =
    Resource
      .make(SyncIO(es))(
        pool =>
          SyncIO {
            println("Starting ExecutionContext shutdown")
            pool.shutdown()
            pool.awaitTermination(10, TimeUnit.SECONDS)
            println("Shutdown complete")
        }
      )
      .map(ExecutionContext.fromExecutorService)
      .map(exitOnFatal)
      .mapK(SyncIOToIO)

  val SyncIOToIO: SyncIO ~> IO = new (SyncIO ~> IO) {
    def apply[A](fa: SyncIO[A]): IO[A] = fa.toIO
  }

  def exitOnFatal(ec: ExecutionContext): ExecutionContext = new ExecutionContext {
    def execute(r: Runnable): Unit =
      ec.execute(new Runnable {
        def run(): Unit =
          try {
            r.run()
          } catch {
            case NonFatal(t) =>
              reportFailure(t)

            case t: Throwable =>
              // under most circumstances, this will work even with fatal errors
              t.printStackTrace()
              System.exit(1)
          }
      })

    def reportFailure(t: Throwable): Unit =
      ec.reportFailure(t)
  }

}
