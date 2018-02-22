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
import javafx.scene.Parent

object CopyOptions {
  def copyWithoutMetadata: CopyOptions = CopyOptions(false, false)
  def copyWithMetadata: CopyOptions = CopyOptions(true, false)
  def copyWithOptionalMetadata: CopyOptions = CopyOptions(true, true)
  def simpleCopy: CopyOptions = copyWithOptionalMetadata
}
trait OperationMonitor {
  def warn(message: => String)
}
object LoggingOperationMonitor extends LoggingOperationMonitor()
case class LoggingOperationMonitor() extends OperationMonitor with SlfLogger {
  override def warn(message: => String) = logger.warn("JediOperation: {}", message)
}
case class CopyOptions(copyMeta: Boolean, optionalMeta: Boolean, monitor: OperationMonitor = LoggingOperationMonitor) {
  def checkCopyToSame(from: AbsoluteBaseLocation, to: AbsoluteBaseLocation): Boolean = {
    if (to.exists && from.uniqueId == to.uniqueId)
      throw new RuntimeException(s"You tried to copy ${from} to itself ${to}. Both have same uniqueId=${from.uniqueId}")
    else
      (from, to) match {
        case (f:BaseNavigableLocation, t:BaseNavigableLocation) =>
          if(t.childOf(f))
            throw new RuntimeException(s"You tried to copy ${from} to child ${to}.")
        case _ =>
      }
    true
  }
}

//TODO add DeletableLocation?
trait OutputLocation extends AbsoluteBaseLocation { self =>
  override type MetaRepr <: OutputLocation with InputLocation
  def unsafeToOutputStream: OutputStream
  def unsafeToOutputStream2: OutputStream = {
    if (!canBeFile)
      throw new RuntimeException("Cannot create an OutputStream since [" + this + "] is not a file!")
    unsafeToOutputStream
  }
  protected def unsafeToWriter: Writer = new BufferedWriter(new OutputStreamWriter(unsafeToOutputStream2, "UTF-8"))
  protected def unsafeToPrintWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(unsafeToOutputStream2, StandardCharsets.UTF_8), true)

  def usingOutputStream[T](op: OutputStream => T): T = using(unsafeToOutputStream2)(op)
  def usingWriter[T](op: Writer => T): T = using(unsafeToWriter)(op)
  def usingPrintWriter[T](op: PrintWriter => T): T = using(unsafeToPrintWriter)(op)

  /** Produce lateral effects in op.*/
  def usingOutputStreamAndContinue(op: OutputStream => Any): self.type = {using(unsafeToOutputStream2)(op);this}
  /** Produce lateral effects in op.*/
  def usingWriterAndContinue(op: Writer => Any): self.type = {using(unsafeToWriter)(op);this}
  /** Produce lateral effects in op.*/
  def usingPrintWriterAndContinue(op: PrintWriter => Any): self.type = {using(unsafeToPrintWriter)(op);this}

  def append: Boolean
  def moveTo(dest: OutputLocation): this.type = ???
  def deleteIfExists: self.type = ???
  def delete: self.type = {
    if (exists)
      deleteIfExists
    else
      throw new RuntimeException("File " + this + " doesn't exists!")
    this
  }

  def writeContent(content: String): self.type = { usingPrintWriter(_.print(content)); this }
  def appendContent(content: String) = withAppend.writeContent(content)
  def withAppend: self.type
  def copyFromWithoutMetadata(src: InputLocation): self.type = copyFrom(src)(CopyOptions.copyWithoutMetadata)
  def copyFromWithMetadata(src: InputLocation): self.type = copyFrom(src)(CopyOptions.copyWithMetadata)

  def copyFrom(src: InputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): self.type = {
    (src, this) match {
      case (from, to: NavigableOutputLocation) if from.isFile && to.isFolder =>
        to.copyFromFileToFileOrFolder(from).asInstanceOf[self.type]
      case (from: NavigableInputLocation, to: NavigableOutputLocation) if from.isFolder && to.canBeFolder =>
        to.copyFromFolder(from).asInstanceOf[self.type]
      case (from, to) if from.isFile && to.isFile => copyFromInputLocation(from)
      case (from, to)                             => copyFromInputLocation(from)
    }
  }
  private def copyFromIncludingMetadata(src: InputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): self.type =
    (for {
      x1 <- Try(copyFromWithoutMetadata(src));
      x2 <- metaLocation;
      x3 <- src.metaLocation;
      x4 <- Try(x2.copyFromWithoutMetadata(x3))
    } yield x1).get.asInstanceOf[self.type]
  def copyFromInputLocation(from: InputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): this.type = {
    if (option.checkCopyToSame(from, this))
      from.usingInputStream { source =>
        usingOutputStream { output =>
          IOUtils.copyLarge(source, output)
        }
      }
    this
  }
}
trait FileOutputLocation extends NavigableOutputLocation with FileAbsoluteBaseLocation { self =>
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
  def moveInto(dest: OutputLocation): this.type = dest match {
    case d: FileOutputLocation =>
      FileUtils.moveFile(toFile, d.child(name).toFile)
      this
    case _ =>
      ???
  }
  override def deleteIfExists: self.type = {
    if (exists) {
      logger.info(s"delete existing $absolute")
      impl.ApacheFileUtils.forceDelete(toPath)
    }
    this
  }
  def copyFromAsHardLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): self.type = {
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
