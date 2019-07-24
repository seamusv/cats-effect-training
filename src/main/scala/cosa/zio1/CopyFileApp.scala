package cosa.zio1

import java.io._

import zio._
import zio.console.{putStr, putStrLn}

object CopyFileApp extends App {

  def inputStream(f: File, guard: Semaphore): Managed[Throwable, InputStream] =
    Managed.make {
      Task.effect(new FileInputStream(f))
    } { inStream =>
      guard.withPermit {
        Task.effect(inStream.close()).catchAll(_ => ZIO.succeed(()))
      }
    }

  def outputStream(f: File, guard: Semaphore): Managed[Throwable, OutputStream] =
    Managed.make {
      Task.effect(new FileOutputStream(f))
    } { outStream =>
      guard.withPermit {
        Task.effect(outStream.close()).catchAll(_ => ZIO.succeed(()))
      }
    }

  def transmit(origin: InputStream, destination: OutputStream, buffer: Array[Byte], acc: Long): Task[Long] =
    for {
      amount <- Task.effect(origin.read(buffer))
      count <- if (amount > -1) Task.effect(destination.write(buffer, 0, amount)) *> transmit(origin, destination, buffer, acc + amount)
      else UIO.succeed(acc)
    } yield count

  def transfer(origin: InputStream, destination: OutputStream): Task[Long] =
    for {
      buffer <- Task.effect(new Array[Byte](1024 * 10))
      total <- transmit(origin, destination, buffer, 0L)
    } yield total

  def copy(origin: File, destination: File): Task[Long] =
    for {
      guard <- Semaphore.make(1)
      count <- inputOutputStreams(origin, destination, guard).use { case (in, out) =>
        guard.withPermit(transfer(in, out))
      }
    } yield count

  def inputOutputStreams(in: File, out: File, guard: Semaphore): Managed[Throwable, (InputStream, OutputStream)] =
    for {
      inStream <- inputStream(in, guard)
      outStream <- outputStream(out, guard)
    } yield (inStream, outStream)

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      _ <- if (args.length < 2) Task.fail(new IllegalArgumentException("Need origin and destination files")) else UIO.succeed(())
      orig = new File(args(0))
      dest = new File(args(1))
      count <- copy(orig, dest)
      _ <- putStrLn(s"$count bytes copied from ${orig.getPath} to ${dest.getPath}")
    } yield ())
      .foldM(
        throwable => putStr(s"Error: ${throwable.getMessage}") *> UIO.succeed(1),
        _ => UIO.succeed(0)
      )
}
