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
import scala.util.Try
import org.raisercostin.jedi.impl.SlfLogger
import scala.util.Failure

//TODO add DeletableLocation?
trait OutputLocation extends AbsoluteBaseLocation { self =>
  override type Repr = self.type
  def unsafeToOutputStream: OutputStream
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
  def copyFrom(src: InputLocation): this.type = { src.copyTo(self); this }
}
trait FileOutputLocation extends OutputLocation with FileAbsoluteBaseLocation { self =>
  override type Repr = self.type
  def unsafeToOutputStream: OutputStream = if (isFolder)
    throw new RuntimeException(s"Cannot open an OutputStream to the folder ${this}")
  else
    new FileOutputStream(absolute, append)
  override def moveTo(dest: OutputLocation): this.type = dest match {
    case d: FileOutputLocation =>
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
  def copyFromAsSymLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Repr = {
    import org.raisercostin.jedi.impl.LogTry._
    SlfLogger.logger.info("symLink {} -> {}", src, this, "")
    if (!overwriteIfAlreadyExists && exists) {
      throw new RuntimeException("Destination file " + this + " already exists.")
    } else {
      val first = Try {
        Files.createSymbolicLink(toPath, src.toPath)
      }
      first.recoverWith {
        case error =>
          val symlinkType = if (src.isFile) "" else "/D"
          val second = Try { executeWindows(Seq("mklink", symlinkType, this.absoluteWindows, src.absoluteWindows)) }
          second.recoverWith { case _ => first.log }
          second
      }.log
    }
    this
  }
  /**Inspired from here: http://winaero.com/blog/symbolic-link-in-windows-10 */
  def executeWindows(command: Seq[String]) = {
    SlfLogger.logger.info("Execute on windows shell: [{}]", command.mkString("\"", "\" \"", "\""))
    import sys.process._
    val processLogger = ProcessLogger(out => logger.info(out), err => logger.warn(err))
    Seq("cmd", "/C") ++ command ! (processLogger)
  }
  def copyFromAsHardLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Repr = {
    if (overwriteIfAlreadyExists) {
      Files.createLink(toPath, src.toPath)
    } else {
      if (exists) {
        throw new RuntimeException("Destination file " + this + " already exists.")
      } else {
        if (!src.isFile)
          throw new RuntimeException("Cannot create a hardLink. Source " + src + " is not a file.")
        Files.createLink(toPath, src.toPath)
      }
    }
    this
  }
}
