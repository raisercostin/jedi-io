package org.raisercostin.jedi

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
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
}