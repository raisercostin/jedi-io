package org.raisercostin.jedi

import java.io.File
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import java.nio.file.Files


data class TempLocation(temp: File, append: Boolean = false) : FileLocation {self->
  override fun ,Append: self.type = this.copy(append = true)
  fun fileFullPath: String = temp.getAbsolutePath()
  fun randomChild(prefix: String = "random", suffix: String = "") = TempLocation(File.createTempFile(prefix, suffix, toFile))
  fun randomFolderChild(prefix: String = "random") = TempLocation(Files.createTempDirectory(prefix).toFile)
  override fun build(path:String): self.type = TempLocation(new File(path),append)
  //optimized not to convert back and forth to the external format
  override fun child(child: String): self.type = TempLocation(childFile(child))
}