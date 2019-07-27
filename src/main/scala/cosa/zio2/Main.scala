package cosa.zio2

import java.io.File

import zio._
import zio.blocking.Blocking
import zio.console._

object Main extends App {

  import transfer._

  type AppEnvironment = FileTransfer with Console

  def checkContinue(prompt: String): TaskR[Console, Boolean] =
    for {
      _ <- putStr(prompt)
      choice <- getStrLn
      cont <- if (choice.toLowerCase.startsWith("y")) UIO.succeed(true)
      else if (choice.toLowerCase.startsWith("n")) UIO.succeed(false)
      else checkContinue(prompt)
    } yield cont

  def copyAndPrintResult(origin: File, destination: File): TaskR[AppEnvironment, Unit] =
    for {
      count <- copy(origin, destination)
      _ <- putStrLn(s"$count bytes copied from ${origin.getPath} to ${destination.getPath} ")
    } yield ()

  def copyProgram(args: List[String]): TaskR[AppEnvironment, Unit] =
    for {
      _ <- if (args.length < 2) Task.fail(new IllegalArgumentException("Need origin and destination files")) else UIO.succeed(())
      orig = new File(args(0))
      dest = new File(args(1))

      _ <- if (orig.getCanonicalPath == dest.getCanonicalPath) Task.fail(new IllegalArgumentException("Origin and destination cannot be the same")) else UIO.succeed(())

      doCopy <- if (dest.exists()) checkContinue(s"The file ${dest.getCanonicalPath} already exists.  Overwrite? (y/n): ") else UIO.succeed(true)
      _ <- if (doCopy) copyAndPrintResult(orig, dest) else UIO.succeed(())
    } yield ()

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      program <- copyProgram(args)
        .provideSome[Environment] { base =>
          new FileTransfer with Console {
            override def fileTransfer: FileTransfer.Service[Any] = new FileTransfer.Live {
              override val blocking: Blocking.Service[Any] = base.blocking
            }.fileTransfer

            override val console: Console.Service[Any] = base.console
          }
        }
    } yield program)
      .foldM(
        throwable => putStrLn(s"Error: ${throwable.getMessage}") *> UIO.succeed(1),
        _ => UIO.succeed(0)
      )
}
