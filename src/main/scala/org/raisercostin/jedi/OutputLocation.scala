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

/** A copy operation can be influenced by a lot of parameters.
  * Still needs to decide which ones are useful. 
  *
  *
  * # Some software have:
  * totalCommander has on (F5):
  * - overwrite options
  *   - ask user
  *   - overwrite all
  *   - skip all
  *   - overwrite all older
  *   - auto-rename copied
  *   - auto-rename target files
  *   - copy all larger files (overwrite smaller)
  *   - copy all smaller files (overwrite larger)
  * - skip all which cannot be opened for reading
  * - overwrite/delete read only/hidden/system
  * - copy to all selected folders/links in the target panel
  * - copy NTFS permissions (may need administrator rights)
  * - F2 - Queue (non blocking copy in a queue together with other commands)
  * - only files of this type with format as ( *.*|target\ .git\ .target\ .svn\ bin\ share\ .settings\ logs\ .meta\ target2\ docs\ )
  * - verify
  *
  * totalCommander if need to overwrite displays the menu:
  * - overwrite
  * - overwrite all
  * - skip
  * - skip all
  * - overwrite all older
  * - rename - manual rename
  * - more options:
  *   - compare
  *   - rename existing target file
  *   - auto-rename copied
  *   - auto-rename target files
  *   - ovewrite all older and of the same age
  *   - copy all larger files (overwrite smaller)
  *   - copy all smaller files (overwrite larger)
  *
  * DosNavigator copy has the following options:
  * - overwrite/append/resume/skip/refresh/ask the action
  * - check free disk space
  * - verify disk writes
  * - copy descriptions (like copyWithMetadata)
  * - copy access rights
  * - remove source files (a move)
  *
  * Far copy has the following options:
  * - access rights: default, copy, inherit
  * - copy contents of symbolic links
  * - process multiple destinations
  * - use filter
  *
  * MC copy has the following options:
  * - using shell patterns
  * - follow symlinks
  * - dive into subdir if exists
  * - preserve attributes
  * - stable symlinks
  * - background
  *
  *
  * robocopy.exe has other options:
  * Copy options :
  * /S * copy Subdirectories, but not empty ones.
  * /E * copy subdirectories, including Empty ones.
  * /LEV:n * only copy the top n LEVels of the source directory tree.
  * /Z * copy files in restartable mode.
  * /B * copy files in Backup mode.
  * /ZB * use restartable mode; if access denied use Backup mode.
  * /COPY:copyflag[s] * what to COPY (default is /COPY:DAT).
  * (copyflags : D=Data, A=Attributes, T=Timestamps).
  * (S=Security=NTFS ACLs, O=Owner info, U=aUditing info).
  * /SEC * copy files with SECurity (equivalent to /COPY:DATS).
  * /COPYALL * COPY ALL file info (equivalent to /COPY:DATSOU).
  * /NOCOPY * COPY NO file info (useful with /PURGE).
  * /PURGE * delete dest files/dirs that no longer exist in source.
  * /MIR * MIRror a directory tree (equivalent to /E plus /PURGE).
  * /MOV * MOVe files (delete from source after copying).
  * /MOVE * MOVE files AND dirs (delete from source after copying).
  * /A+:[RASHNT] * add the given Attributes to copied files.
  * /A-:[RASHNT] * remove the given Attributes from copied files.
  * /CREATE * CREATE directory tree and zero-length files only.
  * /FAT * create destination files using 8.3 FAT file names only.
  * /FFT * assume FAT File Times (2-second granularity).
  * /256 * turn off very long path (> 256 characters) support.
  * /MON:n * MONitor source; run again when more than n changes seen.
  * /MOT:m * MOnitor source; run again in m minutes Time, if changed.
  * /RH:hhmm-hhmm * Run Hours - times when new copies may be started.
  * /PF * check run hours on a Per File (not per pass) basis.
  * /IPG:n * Inter-Packet Gap (ms), to free bandwidth on slow lines.
  * */
object CopyOptions {
  def copyWithoutMetadata: CopyOptions = CopyOptions(overwriteIfAlreadyExists = false, false, false)

  def copyWithMetadata: CopyOptions = CopyOptions(overwriteIfAlreadyExists = false, true, false)

  def copyWithOptionalMetadata: CopyOptions = CopyOptions(overwriteIfAlreadyExists = false, true, true)

  def default: CopyOptions = copyWithOptionalMetadata
}

trait OperationMonitor {
  def warn(message: => String)
}

object LoggingOperationMonitor extends LoggingOperationMonitor()

case class LoggingOperationMonitor() extends OperationMonitor with SlfLogger {
  override def warn(message: => String) = logger.warn("JediOperation: {}", message)
}

case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta: Boolean, optionalMeta: Boolean, monitor: OperationMonitor = LoggingOperationMonitor) {
  def checkCopyToSame(from: AbsoluteBaseLocation, to: AbsoluteBaseLocation): Boolean = {
    if (to.exists && from.uniqueId == to.uniqueId)
      throw new RuntimeException(s"You tried to copy ${from} to itself ${to}. Both have same uniqueId=${from.uniqueId}")
    else
      (from, to) match {
        case (f: BaseNavigableLocation, t: BaseNavigableLocation) =>
          if (t.childOf(f))
            throw new RuntimeException(s"You tried to copy ${from} to child ${to}.")
        case _ =>
      }
    true
  }

  def withOverwriteIfAlreadyExists = this.copy(overwriteIfAlreadyExists = true)
}

//TODO add DeletableLocation?
trait OutputLocation extends AbsoluteBaseLocation {
  self =>
  //override type MetaRepr <: OutputLocation with InputLocation
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

  /** Produce lateral effects in op. */
  def usingOutputStreamAndContinue(op: OutputStream => Any): self.type = {
    using(unsafeToOutputStream2)(op); this
  }

  /** Produce lateral effects in op. */
  def usingWriterAndContinue(op: Writer => Any): self.type = {
    using(unsafeToWriter)(op); this
  }

  /** Produce lateral effects in op. */
  def usingPrintWriterAndContinue(op: PrintWriter => Any): self.type = {
    using(unsafeToPrintWriter)(op); this
  }

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

  def writeContent(content: String): self.type = {
    usingPrintWriter(_.print(content)); this
  }

  def appendContent(content: String) = withAppend.writeContent(content)

  def withAppend: self.type

  def copyFromWithoutMetadata(src: InputLocation): self.type = copyFrom(src)(CopyOptions.copyWithoutMetadata)

  def copyFromWithMetadata(src: InputLocation): self.type = copyFrom(src)(CopyOptions.copyWithMetadata)

  def copyFrom(src: InputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type = {
    (src, this) match {
      case (from, to: NavigableOutputLocation) if from.isFile && to.isFolder =>
        to.copyFromFileToFileOrFolder(from).asInstanceOf[self.type]
      case (from: NavigableInputLocation, to: NavigableOutputLocation) if from.isFolder && to.canBeFolder =>
        to.copyFromFolder(from).asInstanceOf[self.type]
      case (from, to) if from.isFile && to.isFile => copyFromInputLocation(from)
      case (from, to) => copyFromInputLocation(from)
    }
  }

  private def copyFromIncludingMetadata(src: InputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type =
    (for {
      x1 <- Try(copyFromWithoutMetadata(src));
      x2 <- metaLocation;
      x3 <- src.metaLocation;
      x4 <- Try(x2.copyFromWithoutMetadata(x3))
    } yield x1).get.asInstanceOf[self.type]

  def copyFromInputLocation(from: InputLocation)(implicit option: CopyOptions = CopyOptions.default): this.type = {
    if (option.checkCopyToSame(from, this))
      from.usingInputStream { source =>
        usingOutputStream { output =>
          IOUtils.copyLarge(source, output)
        }
      }
    this
  }
}

trait FileOutputLocation extends NavigableOutputLocation with FileAbsoluteBaseLocation {
  self =>
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

  def copyFromAsHardLink(src: FileInputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type = {
    if (option.overwriteIfAlreadyExists) {
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
