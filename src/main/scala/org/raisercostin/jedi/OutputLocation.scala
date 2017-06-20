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
import org.apache.commons.io.IOUtils
import scala.util.Success
import org.raisercostin.jedi.impl.ProcessUtils

//TODO add DeletableLocation?
trait OutputLocation extends AbsoluteBaseLocation { self =>
  override type Repr = self.type
  def unsafeToOutputStream: OutputStream
  def unsafeToOutputStream2: OutputStream = {
    if(!canBeFile)
      throw new RuntimeException("Cannot create an OutputStream since ["+this+"] is not a file!")
    unsafeToOutputStream
  }
  protected def unsafeToWriter: Writer = new BufferedWriter(new OutputStreamWriter(unsafeToOutputStream2, "UTF-8"))
  protected def unsafeToPrintWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(unsafeToOutputStream2, StandardCharsets.UTF_8), true)

  def usingOutputStream[T](op: OutputStream => T): T = using(unsafeToOutputStream2)(op)
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

  def writeContent(content: String): Repr = { usingPrintWriter(_.print(content)); this }
  def appendContent(content: String) = withAppend.writeContent(content)
  def withAppend: self.type
  def copyFrom(src: InputLocation): Repr = {
    (src, this) match {
      case (from, to) if from.isFile && to.isFile => copyFromInputLocation(from)
      case (from, to: NavigableOutputLocation) if from.isFile && to.isFolder => to.copyFromFileToFileOrFolder(from).asInstanceOf[Repr]
      case (from: NavigableInputLocation, to: NavigableOutputLocation) if from.isFolder && to.canBeFolder => to.copyFromFolder(from).asInstanceOf[Repr]
      case (from,to) => copyFromInputLocation(from)
    }
  }
  def copyFromInputLocation(from: InputLocation): this.type = {
    from.usingInputStream { source =>
      usingOutputStream { output =>
        IOUtils.copyLarge(source, output)
      }
    }
    this
  }
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
  def copyFromAsSymLinkAndGet(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Repr = copyFromAsSymLink(src,overwriteIfAlreadyExists).get
  def copyFromAsSymLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Try[Repr] = {
    import org.raisercostin.jedi.impl.LogTry._
    SlfLogger.logger.info("symLink {} -> {}", src, this, "")
    if (!overwriteIfAlreadyExists && exists) {
      Failure(new RuntimeException("Destination file " + this + " already exists."))
    } else {
      val first = Try {
        Files.createSymbolicLink(toPath, src.toPath)
      }
      first.recoverWith {
        case error =>
          val symlinkType = if (src.isFile) "" else "/D"
          val second = ProcessUtils.executeWindows(Seq("mklink", symlinkType, this.absoluteWindows, src.absoluteWindows))
          second.recoverWith { case _ => first.log }
          second
      }.map(x=> this)//log.get
    }
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
