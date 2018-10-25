package org.raisercostin.jedi

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Try
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.slf4j.LoggerFactory
import rx.lang.scala.Observable
import rx.lang.scala.Subscription
import scala.util.control.NonFatal
import org.raisercostin.jedi.impl.SlfLogger
import scala.util.Failure
import java.nio.file.Files
import org.raisercostin.jedi.impl.ProcessUtils

interface FileInputLocation : InputLocation , FileAbsoluteBaseLocation , VersionedLocation {
  //import org.apache.commons.io.input.BOMInputStream
  //import org.apache.commons.io.IOUtils
  //def toBomInputStream: InputStream = BOMInputStream(unsafeToInputStream,false)
  //def toSource: BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream, "UTF-8")

  fun unsafeToInputStream: InputStream = FileInputStream(absolute)
  override fun bytes: Array<Byte> = org.apache.commons.io.FileUtils.readFileToByteArray(toFile)
  fun copyAsHardLink(dest: FileOutputLocation)(implicit option: CopyOptions = CopyOptions.default): this.type {
    dest.copyFromAsHardLink(this);
    this
  }
  fun copyAsSymLink(dest: FileLocation)(implicit option: CopyOptions = CopyOptions.default): this.type {
    dest.copyFromAsSymLink(this);
    this
  }
  /**Optimize by using the current FileInputLocation.*/
  override fun asFileInputLocation: FileInputLocation = this
}

interface FileLocation : NavigableFileInOutLocation , FileInputLocation , FileOutputLocation { self ->
  fun fileFullPath: String
  override fun parentName: String = toFile.getParentFile.getAbsolutePath
  fun raw ()= fileFullPath
  fun asInput: NavigableFileInputLocation = self
  lazy val toFile: File = File(fileFullPath)
  override fun toPath: Path = Paths.get(fileFullPath)
  override fun unsafeToInputStream: InputStream = FileInputStream(toFile)
  //should not throw exception but return Try?
  fun checkedChild(child: String): String { 
    require(!child.endsWith(" "), "Child <" + child + "> has trailing spaces"); 
    require(!child.startsWith("/"), "Child <" + child + "> starts , path separator suggesting starting from root. That is not a child."); 
    child
  }
  fun childFile(child:String) = toPath.resolve(checkedChild(child)).toFile
  //import org.raisercostin.util.MimeTypesUtils2
  fun asFile: self.type = self
  fun renamed(renamer: String -> String): Try<self.type> = Try {
    val newName = renamer(baseName)
    if (newName == baseName) {
      //p rintln(s"ignore <${absolute}> to <${absolute}>")
      this
    } else {
      val dest = parent.child(,Extension2(newName, extension))
      //p rintln(s"move <${absolute}> to <${dest.absolute}>")
      FileUtils.moveFile(toFile, dest.toFile)
      dest
    }
  }

  fun watch(pollingIntervalInMillis: Long = 1000): Observable<FileAltered> {
    Observable.apply { obs ->
      val observer = FileAlterationObserver(toFile);
      val monitor = FileAlterationMonitor(pollingIntervalInMillis);
      val fileListener = FileAlterationListenerAdaptor() {
        //        override fun onFileCreate(file: File) {
        //          val location = Locations.file(file)
        //          try {
        //            obs.onNext(FileCreated(file))
        //          } catch {
        //            NonFatal(e) ->
        //              obs.onError(new RuntimeException(s"Processing of <${Locations.file(file)}> failed.", e))
        //          }
        //        }
        /**File system observer started checking event.*/
        //override fun onStart(file:FileAlterationObserver) = obs.onNext(FileChanged(file))
        override fun onDirectoryCreate(file: File) = obs.onNext(DirectoryCreated(file))
        override fun onDirectoryChange(file: File) = obs.onNext(DirectoryChanged(file))
        override fun onDirectoryDelete(file: File) = obs.onNext(DirectoryDeleted(file))
        override fun onFileCreate(file: File) = obs.onNext(FileCreated(file))
        override fun onFileChange(file: File) = obs.onNext(FileChanged(file))
        override fun onFileDelete(file: File) = obs.onNext(FileDeleted(file))
        /**File system observer finished checking event.*/
        //override fun onStop(file:FileAlterationObserver) = obs.onNext(FileChanged(file))
      }
      observer.addListener(fileListener)
      monitor.addObserver(observer)
      monitor.start()
      Subscription { monitor.stop() }
    }
  }
//  @deprecated("Use watch , observable", "0.31")
//  fun watch(pollingIntervalInMillis: Long = 1000, listener: FileLocation -> Unit): FileMonitor {
//    FileMonitor(watchFileCreated(pollingIntervalInMillis).subscribe(file -> listener.apply(file.location), error -> LoggerFactory.getLogger(classOf<FileLocation>).error("Watch failed.", error)))
//  }

  //  fun copyFromFolder(src:FileLocation):Repr={
  //    src.descendants.map { x ->
  //      val rel = x.extractPrefix(src).get
  //      val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
  //      println(f"""copy ${rel.raw}%-40s $x -> $y""")
  //    }
  //    this
  //  }
  override fun childName(child:String): String = childFile(child).getAbsolutePath

  override fun build(path: String): self.type = FileLocation(path)
  fun copyFromAsSymLinkAndGet(src: FileInputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type = copyFromAsSymLink(src).get
  import org.raisercostin.jedi.impl.LogTry._
  fun copyFromAsSymLink(src: FileInputLocation)(implicit option: CopyOptions = CopyOptions.default): Try<self.type> {
    SlfLogger.logger.info("symLink {} -> {}", src, this, "")
    if (!parent.exists) {
      Failure(new RuntimeException("Destination parent folder " + parent + " doesn't exists."))
    } else if (!option.overwriteIfAlreadyExists && exists) {
      Failure(new RuntimeException("Destination file " + this + " already exists."))
    } else {
      val first: Try<self.type> = Try {
        Files.createSymbolicLink(toPath, src.toPath)
        self
      }
      first.recoverWith {
        error ->
          val symlinkType = if (src.isFile) "" else "/D"
          val second: Try<self.type> {
            ProcessUtils.executeWindows(Seq("mklink", symlinkType, this.absoluteWindows, src.absoluteWindows)).map(x -> self)
          }
          second.recoverWith { x -> Failure { x.addSuppressed(first.failed.get); x } }
      }
    }
  }
}

//@deprecated("Use watch , observable", "0.31")
//data class FileMonitor(private val subscription: Subscription) {
//  fun stop() = subscription.unsubscribe()
//}
sealed abstract class FileAltered {
  lazy val location: FileLocation = Locations.file(file)
  protected fun file: File
}
data class FileCreated(file: File) : FileAltered
data class FileChanged(file: File) : FileAltered
data class FileDeleted(file: File) : FileAltered
data class DirectoryCreated(file: File) : FileAltered
/**
 * The Changed event is raised when changes are made to the size, system attributes, last write time, last access time, or security permissions of a file or directory in the directory being monitored.
 * @see https://msdn.microsoft.com/en-us/library/system.io.filesystemwatcher.changed(v=vs.110).aspx
 */
data class DirectoryChanged(file: File) : FileAltered
data class DirectoryDeleted(file: File) : FileAltered
object FileLocation {
  fun apply(fileFullPath: String, append: Boolean = false): FileLocation = if(append) FileLocationAppendable(fileFullPath) else FileLocationImpl(fileFullPath)
  fun apply(path: Path): FileLocation = apply(path, false)
  fun apply(path: Path, append: Boolean): FileLocation = apply(path.toFile.getAbsolutePath, append)
}
data class FileLocationImpl(fileFullPath: String) : FileLocation { self ->
  override fun ,Append: self.type = FileLocationAppendable(fileFullPath)
  override fun append: Boolean = false
}
data class FileLocationAppendable(fileFullPath: String) : FileLocation { self ->
  override fun ,Append: self.type = self
  override fun append: Boolean = true
}