package cosa.zio2.transfer

import java.io._

import zio._
import zio.blocking.Blocking

trait FileTransfer {

  def fileTransfer: FileTransfer.Service[Any]

}

object FileTransfer {

  trait Service[R] {
    def copy(origin: File, destination: File): TaskR[R, Long]
  }

  trait Live extends FileTransfer with Blocking {

    override def fileTransfer: Service[Any] = new Service[Any] {
      override def copy(origin: File, destination: File): Task[Long] =
        for {
          guard <- Semaphore.make(1)
          count <- inputOutputStreams(origin, destination, guard).use { case (in, out) =>
            guard.withPermit(transfer(in, out))
          }
        } yield count

      private def inputStream(f: File, guard: Semaphore): Managed[Throwable, InputStream] =
        Managed.make {
          Task.effect(new FileInputStream(f))
        } { inStream =>
          guard.withPermit {
            Task.effect(inStream.close()).catchAll(_ => ZIO.succeed(()))
          }
        }

      private def outputStream(f: File, guard: Semaphore): Managed[Throwable, OutputStream] =
        Managed.make {
          Task.effect(new FileOutputStream(f))
        } { outStream =>
          guard.withPermit {
            Task.effect(outStream.close()).catchAll(_ => ZIO.succeed(()))
          }
        }

      private def transmit(origin: InputStream, destination: OutputStream, buffer: Array[Byte]): Task[Long] = {
        def inner(acc: Long): Long = {
          val amount = origin.read(buffer)
          if (amount > -1) {
            destination.write(buffer, 0, amount)
            inner(acc + amount)
          } else {
            acc
          }
        }

        blocking.effectBlocking {
          inner(0L)
        }
      }

      private def transfer(origin: InputStream, destination: OutputStream): Task[Long] =
        for {
          buffer <- Task.effect(new Array[Byte](1024 * 8))
          total <- transmit(origin, destination, buffer)
        } yield total

      private def inputOutputStreams(in: File, out: File, guard: Semaphore): Managed[Throwable, (InputStream, OutputStream)] =
        for {
          inStream <- inputStream(in, guard)
          outStream <- outputStream(out, guard)
        } yield (inStream, outStream)

    }
  }

}