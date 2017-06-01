package org.raisercostin.jedi

import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import Locations.logger
import scala.annotation.tailrec

//TODO add DeletableLocation?
trait OutputLocation extends AbsoluteBaseLocation{self=>
  type Repr = self.type
  protected def unsafeToOutputStream: OutputStream
  protected def unsafeToWriter: Writer = new BufferedWriter(new OutputStreamWriter(unsafeToOutputStream, "UTF-8"))
  protected def unsafeToPrintWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(unsafeToOutputStream, StandardCharsets.UTF_8), true)

  def usingOutputStream[T](op: OutputStream => T): T = using(unsafeToOutputStream)(op)
  def usingWriter[T](op: Writer => T): T = using(unsafeToWriter)(op)
  def usingPrintWriter[T](op: PrintWriter => T): T = using(unsafeToPrintWriter)(op)

  def append: Boolean
  def moveTo(dest: OutputLocation): this.type = ???
  def deleteIfExists: Repr = ???
  def delete: Repr = {
    if (exists)
      deleteIfExists
    else
      throw new RuntimeException("File " + this + " doesn't exists!")
    this
  }

  def writeContent(content: String): this.type = { usingPrintWriter(_.print(content)); this }
  def appendContent(content: String) = withAppend.writeContent(content)
  def withAppend: self.type
  def copyFrom(src: InputLocation): this.type = { src.copyTo(this); this }
}
trait FileOutputLocation extends OutputLocation with FileAbsoluteBaseLocation{self=>
  override type Repr = self.type
  protected def unsafeToOutputStream: OutputStream = new FileOutputStream(absolute, append)
   override def moveTo(dest: OutputLocation): this.type = dest match {
    case d:FileOutputLocation=>
      FileUtils.moveFile(toFile, d.toFile)
      this
    case _ =>
      ???
  }
  override def deleteIfExists: Repr = {
    if (exists) {
      logger.info(s"delete existing $absolute")
      impl.ApacheFileUtils.forceDelete(toPath)
    }
    this
  }
 def copyFromAsSymLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): this.type = {
    if (overwriteIfAlreadyExists) {
      Files.createSymbolicLink(toPath, src.toPath)
    } else {
      if (exists) {
        throw new RuntimeException("Destination file " + this + " already exists.")
      } else {
        Files.createSymbolicLink(toPath, src.toPath)
      }
    }
    this
  }
  def copyFromAsHardLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Repr = {
    if (overwriteIfAlreadyExists) {
      Files.createLink(toPath, src.toPath)
    } else {
      if (exists) {
        throw new RuntimeException("Destination file " + this + " already exists.")
      } else {
        if(!src.isFile)
          throw new RuntimeException("Cannot create a hardLink. Source " + src + " is not a file.")
        Files.createLink(toPath, src.toPath)
      }
    }
    this
  }
}
