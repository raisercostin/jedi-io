package org.raisercostin.jedi

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import scala.annotation.tailrec
trait NavigableOutputLocation extends OutputLocation with NavigableLocation { self =>
  override type Repr = self.type

  def mkdirOnParentIfNecessary: this.type = {
    parent.mkdirIfNecessary
    this
  }
  def deleteOrRenameIfExists: Repr = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
  def asInput: NavigableInputLocation
  @tailrec final def moveToRenamedIfExists(dest: NavigableOutputLocation): this.type = {
    try{
      FileUtils.moveFile(toFile, dest.toFile)
      this
    }catch{
      case _ =>
        moveToRenamedIfExists(dest.renamedIfExists)
    }
  }
}