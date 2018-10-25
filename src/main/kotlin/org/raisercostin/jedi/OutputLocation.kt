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
  * - F2 - Queue (non blocking copy in a queue together , other commands)
  * - only files of this type , format as ( *.*|target\ .git\ .target\ .svn\ bin\ share\ .settings\ logs\ .meta\ target2\ docs\ )
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
  * /COPY:copyflag<s> * what to COPY (default is /COPY:DAT).
  * (copyflags : D=Data, A=Attributes, T=Timestamps).
  * (S=Security=NTFS ACLs, O=Owner info, U=aUditing info).
  * /SEC * copy files , SECurity (equivalent to /COPY:DATS).
  * /COPYALL * COPY ALL file info (equivalent to /COPY:DATSOU).
  * /NOCOPY * COPY NO file info (useful , /PURGE).
  * /PURGE * delete dest files/dirs that no longer exist in source.
  * /MIR * MIRror a directory tree (equivalent to /E plus /PURGE).
  * /MOV * MOVe files (delete from source after copying).
  * /MOVE * MOVE files AND dirs (delete from source after copying).
  * /A+:<RASHNT> * add the given Attributes to copied files.
  * /A-:<RASHNT> * remove the given Attributes from copied files.
  * /CREATE * CREATE directory tree and zero-length files only.
  * /FAT * create destination files using 8.3 FAT file names only.
  * /FFT * assume FAT File Times (2-second granularity).
  * /256 * turn off very long path (> 256 characters) support.
  * /MON:n * MONitor source; run again when more than n changes seen.
  * /MOT:m * MOnitor source; run again in m minutes Time, if changed.
  * /RH:hhmm-hhmm * Run Hours - times when copies may be started.
  * /PF * check run hours on a Per File (not per pass) basis.
  * /IPG:n * Inter-Packet Gap (ms), to free bandwidth on slow lines.
  * */
object CopyOptions {
  fun copyWithoutMetadata: CopyOptions = CopyOptions(overwriteIfAlreadyExists = false, false, false)

  fun copyWithMetadata: CopyOptions = CopyOptions(overwriteIfAlreadyExists = false, true, false)

  fun copyWithOptionalMetadata: CopyOptions = CopyOptions(overwriteIfAlreadyExists = false, true, true)

  fun default: CopyOptions = copyWithOptionalMetadata
}

interface OperationMonitor {
  fun warn(message: -> String)
}

object LoggingOperationMonitor : LoggingOperationMonitor()

data class LoggingOperationMonitor() : OperationMonitor , SlfLogger {
  override fun warn(message: -> String) = logger.warn("JediOperation: {}", message)
}

data class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta: Boolean, optionalMeta: Boolean, monitor: OperationMonitor = LoggingOperationMonitor) {
  fun checkCopyToSame(from: AbsoluteBaseLocation, to: AbsoluteBaseLocation): Boolean {
    if (to.exists && from.uniqueId == to.uniqueId)
      throw RuntimeException(s"You tried to copy ${from} to itself ${to}. Both have same uniqueId=${from.uniqueId}")
    else
      (from, to) when {
        (f: BaseNavigableLocation, t: BaseNavigableLocation) ->
          if (t.childOf(f))
            throw RuntimeException(s"You tried to copy ${from} to child ${to}.")
        else ->
      }
    true
  }

  fun ,OverwriteIfAlreadyExists = this.copy(overwriteIfAlreadyExists = true)
}

//TODO add DeletableLocation?
interface OutputLocation : AbsoluteBaseLocation {
  self ->
  //override type MetaRepr : OutputLocation , InputLocation
  fun unsafeToOutputStream: OutputStream

  fun unsafeToOutputStream2: OutputStream {
    if (!canBeFile)
      throw RuntimeException("Cannot create an OutputStream since <" + this + "> is not a file!")
    unsafeToOutputStream
  }

  protected fun unsafeToWriter: Writer = BufferedWriter(new OutputStreamWriter(unsafeToOutputStream2, "UTF-8"))

  protected fun unsafeToPrintWriter: PrintWriter = PrintWriter(new OutputStreamWriter(unsafeToOutputStream2, StandardCharsets.UTF_8), true)

  fun usingOutputStream<T>(op: OutputStream -> T): T = using(unsafeToOutputStream2)(op)

  fun usingWriter<T>(op: Writer -> T): T = using(unsafeToWriter)(op)

  fun usingPrintWriter<T>(op: PrintWriter -> T): T = using(unsafeToPrintWriter)(op)

  /** Produce lateral effects in op. */
  fun usingOutputStreamAndContinue(op: OutputStream -> Any): self.type {
    using(unsafeToOutputStream2)(op); this
  }

  /** Produce lateral effects in op. */
  fun usingWriterAndContinue(op: Writer -> Any): self.type {
    using(unsafeToWriter)(op); this
  }

  /** Produce lateral effects in op. */
  fun usingPrintWriterAndContinue(op: PrintWriter -> Any): self.type {
    using(unsafeToPrintWriter)(op); this
  }

  fun append: Boolean

  fun moveTo(dest: OutputLocation): this.type = ???

  fun deleteIfExists: self.type = ???

  fun delete: self.type {
    if (exists)
      deleteIfExists
    else
      throw RuntimeException("File " + this + " doesn't exists!")
    this
  }

  fun writeContent(content: String): self.type {
    usingPrintWriter(_.print(content)); this
  }

  fun appendContent(content: String) = ,Append.writeContent(content)

  fun ,Append: self.type

  fun copyFromWithoutMetadata(src: InputLocation): self.type = copyFrom(src)(CopyOptions.copyWithoutMetadata)

  fun copyFromWithMetadata(src: InputLocation): self.type = copyFrom(src)(CopyOptions.copyWithMetadata)

  fun copyFrom(src: InputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type {
    (src, this) when {
      (from, to: NavigableOutputLocation) if from.isFile && to.isFolder ->
        to.copyFromFileToFileOrFolder(from) as self.type>
      (from: NavigableInputLocation, to: NavigableOutputLocation) if from.isFolder && to.canBeFolder ->
        to.copyFromFolder(from) as self.type>
      (from, to) if from.isFile && to.isFile -> copyFromInputLocation(from)
      (from, to) -> copyFromInputLocation(from)
    }
  }

  private fun copyFromIncludingMetadata(src: InputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type =
    (for {
      x1 <- Try(copyFromWithoutMetadata(src));
      x2 <- metaLocation;
      x3 <- src.metaLocation;
      x4 <- Try(x2.copyFromWithoutMetadata(x3))
    } yield x1).get as self.type>

  fun copyFromInputLocation(from: InputLocation)(implicit option: CopyOptions = CopyOptions.default): this.type {
    if (option.checkCopyToSame(from, this))
      from.usingInputStream { source ->
        usingOutputStream { output ->
          IOUtils.copyLarge(source, output)
        }
      }
    this
  }
}

interface FileOutputLocation : NavigableOutputLocation , FileAbsoluteBaseLocation {
  self ->
  fun unsafeToOutputStream: OutputStream = if (isFolder)
    throw RuntimeException(s"Cannot open an OutputStream to the folder ${this}")
  else
    FileOutputStream(absolute, append)

  override fun moveTo(dest: OutputLocation): this.type = dest when {
    d: FileOutputLocation ->
      FileUtils.moveFile(toFile, d.toFile)
      this
    else ->
      ???
  }

  fun moveInto(dest: OutputLocation): this.type = dest when {
    d: FileOutputLocation ->
      FileUtils.moveFile(toFile, d.child(name).toFile)
      this
    else ->
      ???
  }

  override fun deleteIfExists: self.type {
    if (exists) {
      logger.info(s"delete existing $absolute")
      impl.ApacheFileUtils.forceDelete(toPath)
    }
    this
  }

  fun copyFromAsHardLink(src: FileInputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type {
    if (option.overwriteIfAlreadyExists) {
      Files.createLink(toPath, src.toPath)
    } else {
      if (exists) {
        throw RuntimeException("Destination file " + this + " already exists.")
      } else {
        if (!src.isFile)
          throw RuntimeException("Cannot create a hardLink. Source " + src + " is not a file.")
        Files.createLink(toPath, src.toPath)
      }
    }
    this
  }
}