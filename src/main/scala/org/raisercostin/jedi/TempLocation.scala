package org.raisercostin.jedi

import java.io.File

import scala.language.implicitConversions
import scala.language.reflectiveCalls


case class TempLocation(temp: File, append: Boolean = false) extends FileLocationLike {self=>
  override type Repr = self.type
  override def withAppend: self.type = this.copy(append = true)
  def fileFullPath: String = temp.getAbsolutePath()
  def randomChild(prefix: String = "random", suffix: String = "") = new TempLocation(File.createTempFile(prefix, suffix, toFile))
  override def parent: Repr = new TempLocation(new File(parentName))
  override def child(child: String): Repr = new TempLocation(toPath.resolve(checkedChild(child)).toFile)
}