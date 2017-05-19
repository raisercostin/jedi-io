package org.raisercostin.jedi

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import scala.annotation.tailrec
trait NavigableOutputLocation extends OutputLocation with NavigableFileLocation { self =>
  override type Repr = self.type

  def mkdirOnParentIfNecessary: this.type = {
    parent.mkdirIfNecessary
    this
  }
  def deleteOrRenameIfExists: Repr = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
  def asInput: NavigableFileInputLocation
  @tailrec final def moveToRenamedIfExists(dest: NavigableOutputLocation): this.type = {
    try{
      FileUtils.moveFile(toFile, dest.toFile)
      this
    }catch{
      case _ : Exception =>
        moveToRenamedIfExists(dest.renamedIfExists)
    }
  }
}