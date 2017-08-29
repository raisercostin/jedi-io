package org.raisercostin.jedi

import java.io.File
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import java.nio.file.Files


case class TempLocation(temp: File, append: Boolean = false) extends FileLocation {self=>
  override type Repr = self.type
  override def withAppend: self.type = this.copy(append = true)
  def fileFullPath: String = temp.getAbsolutePath()
  def randomChild(prefix: String = "random", suffix: String = "") = new TempLocation(File.createTempFile(prefix, suffix, toFile))
  def randomFolderChild(prefix: String = "random") = new TempLocation(Files.createTempDirectory(prefix).toFile)
  override def build(path:String): Repr = new TempLocation(new File(path),append)
  //optimized not to convert back and forth to the external format
  override def child(child: String): Repr = new TempLocation(childFile(child))
}