package cosa.zio2

import java.io.File

import zio._

package object transfer extends FileTransfer.Service[FileTransfer] {
  def copy(origin: File, destination: File): TaskR[FileTransfer, Long] =
    ZIO.accessM(_.fileTransfer.copy(origin, destination))
}
