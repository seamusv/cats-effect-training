package cosa.zio2

import java.io.File

import zio._
import zio.console._

object Main extends App {

  import transfer._

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      _ <- if (args.length < 2) Task.fail(new IllegalArgumentException("Need origin and destination files")) else UIO.succeed(())
      orig = new File(args(0))
      dest = new File(args(1))

      app = for {
        count <- copy(orig, dest)
        _ <- putStrLn(s"$count bytes copied from ${orig.getPath} to ${dest.getPath}")
      } yield count

      program <- app
        .provideSome[Environment] { base =>
        new FileTransfer with Console {
          override def fileTransfer: FileTransfer.Service[Any] = FileTransfer.Live.fileTransfer

          override val console: Console.Service[Any] = base.console
        }
      }
    } yield program)
      .foldM(
        throwable => putStrLn(s"Error: ${throwable.getMessage}") *> UIO.succeed(1),
        _ => UIO.succeed(0)
      )
}
