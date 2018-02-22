package eclipseScalaBug
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.CodingErrorAction
import java.nio.file.FileStore
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.io.BufferedSource
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Failure
import scala.util.Properties
import scala.util.Success
import scala.util.Try

import org.apache.commons.io.{ FileUtils => CommonsFileUtils }
import org.apache.commons.io.FilenameUtils
import org.raisercostin.jedi.impl.Escape

import sun.net.www.protocol.file.FileURLConnection

object Locations {
  val logger = org.slf4j.LoggerFactory.getLogger("locations")
  private val tmpdir = new File(System.getProperty("java.io.tmpdir"))
  //def temp: TempLocation = TempLocation(tmpdir)
}

trait NavigableInOutLocation extends InOutLocation with NavigableInputLocation with NavigableOutputLocation {
}

trait NavigableFileInputLocation extends InputLocation with NavigableFileLocation with NavigableInputLocation
trait NavigableFileInOutLocation extends InOutLocation with NavigableFileInputLocation with NavigableFileOutputLocation with NavigableInOutLocation

/**Location orthogonal dimension: Resolved/Unresolved: Can reach content/cannot.*/
trait LocationState
/**
 * Trait to mark if a location is not resolved to a file system. For example Relative locations or offline urls that
 * are not available in offline mode.
 */
trait UnresolvedLocationState extends LocationState
/**If a location has access to its content and metadata is said to be resolved.*/
trait ResolvedLocationState extends LocationState with IsFileOrFolder {
  type MetaRepr <: InputLocation

  /**The meta seen as another location.*/
  def metaLocation: Try[MetaRepr]
  def meta: Try[HierarchicalMultimap] = metaLocation.flatMap(_.existingOption.map(_.readContentAsText.map(x => HierarchicalMultimap(x))).getOrElse(Success(HierarchicalMultimap())))
}

trait IsFileOrFolder {
  /**Returns true if is file and file exists.*/
  def isFile: Boolean
  /**Returns true if is folder and folder exists.*/
  def isFolder: Boolean
  /**Returns true if is not an existing folder => so could be a file if created.*/
  def canBeFile: Boolean = !isFolder
  /**Returns true if is not an existing file => so could be a folder if created.*/
  def canBeFolder: Boolean = !isFile
}
trait IsFile extends IsFileOrFolder {
  override def isFile = true
  override def isFolder = false
}
trait IsFolder extends IsFileOrFolder {
  override def isFile = false
  override def isFolder = true
}
trait UnknownFileOrFolder extends IsFileOrFolder {
  override def isFile = throw new RuntimeException("Unknown if file or folder.")
  override def isFolder = throw new RuntimeException("Unknown if file or folder.")
}

trait BaseLocation extends IsFileOrFolder {
  def uri: String = raw
  def raw: String
  /**A part of the location that will be used to retrieve name, baseName, extension.*/
  def nameAndBefore: String
  def name: String = FilenameUtils.getName(nameAndBefore)
  def extension: String = FilenameUtils.getExtension(nameAndBefore)
  def baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  def mimeType = mimeTypeFromName
  def mimeTypeFromName = ???
  //TODO improve slug
  def slug = Escape.toSlug(uri)

  def decoder = {
    import java.nio.charset.Charset
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    decoder
  }
  def standard(text: => String): String = ???
  def standard(selector: this.type => String): String = ???
  def standardWindows(selector: this.type => String): String = ???
  def pathInRaw: String = raw.replaceAll("""^([^*]*)[*].*$""", "$1")
  //def list: Seq[FileLocation] = Option(existing.toFile.listFiles).getOrElse(Array[File]()).map(Locations.file(_))

  def inspect(message: (this.type) => Any): this.type = {
    message(this)
    this
  }
}


abstract case class CacheEntry(cache: NavigableFileInOutLocation) {
  def cacheIt: Unit
}
trait CacheConfig {
  def cacheFor(src: InputLocation): CacheEntry
}
object DefaultCacheConfig extends TimeSensitiveEtagCachedEntry(???)

case class EtagCacheConfig(cacheFolder: NavigableFileInOutLocation) extends CacheConfig {
  def cacheFor(origin: InputLocation): CacheEntry = new CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).withBaseName(x => x + "--etag-" + origin.etag)) {
    def cacheIt: Unit = ???
  }
}
case class TimeSensitiveCachedEntry(cacheFolder: NavigableFileInOutLocation) extends CacheConfig {
  def cacheFor(origin: InputLocation): CacheEntry = new CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).withBaseName(x => x + "--date-" + LocalDate.now())) {
    def cacheIt: Unit = ???
  }
}
/**Use etag if UrlLocation returns one non empty otherwise use date.*/
case class TimeSensitiveEtagCachedEntry(cacheFolder: NavigableFileInOutLocation) extends CacheConfig {
  def cacheFor(origin: InputLocation): CacheEntry = new CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).withBaseName { x =>
    x +
      (if (origin.etag.isEmpty())
        "--date-" + LocalDate.now()
      else
        "--etag-" + origin.etag)
  }) {
    def cacheIt: Unit = ???
  }
}

//TODO CachedLocation when printed should show the temporary file
case class CachedLocation[O <: InputLocation](cacheConfig: CacheConfig, origin: O) extends FileLocation { self =>
  override type Repr = self.type

  private lazy val cacheEntry: CacheEntry = cacheConfig.cacheFor(origin)
  def cache = cacheEntry.cache
  //def cache: InOutLocation = cacheConfig.cacheFor(origin)
  override def build(path: String): Repr = origin match {
    case n: NavigableLocation =>
      CachedLocation(cacheConfig, n.build(path))
    case _ =>
      //TODO bug since origin is not used?
      FileLocation(path)
  }
  override def childName(child: String): String = toPath.resolve(checkedChild(child)).toFile.getAbsolutePath
  //override def withAppend: Repr = self.copy(append = true)
  override def unsafeToInputStream: InputStream = {
    flush
    super.unsafeToInputStream
  }
  /**Force caching.*/
  //TODO as async
  def flush: this.type = {
    cacheEntry.cacheIt
    this
  }

  override def append: Boolean = cache.append
  def fileFullPath: String = cache.nameAndBefore
  def withAppend = ??? //this.copy(cache = cache.withAppend)
}

/**
 * @see http://www.thinkplexx.com/learn/howto/java/system/java-resource-loading-explained-absolute-and-relative-names-difference-between-classloader-and-class-resource-loading
 */
trait ClassPathInputLocation extends NavigableFileInputLocation { self =>
  def initialResourcePath: String
  def raw = initialResourcePath
  import ClassPathInputLocation._
  val resourcePath = initialResourcePath.stripPrefix("/")
  val resource: java.net.URL = ???
  override def exists = true //resource != null - resource is always resolved

  override def toUrl: java.net.URL = resource
  override def absolute: String =
    {
      val x = toUrl.toURI().getPath()
      if (x == null)
        toUrl.toURI().toString()
      else if (Properties.isWin)
        x.stripPrefix("/")
      else
        x
    }
  //Cannot detect if is file or folder. See https://stackoverflow.com/questions/20105554/is-there-a-way-to-tell-if-a-classpath-resource-is-a-file-or-a-directory
  override def isFolder: Boolean = exists && hasMultipleChildrenDifferentThanThis
  override def isFile: Boolean = exists && !hasMultipleChildrenDifferentThanThis
  private def hasMultipleChildrenDifferentThanThis: Boolean =
    if (resource.toURI.isOpaque)
      false
    else
      list match {
        case x :: nil =>
          list.head.absolute != absolute.stripSuffix("/").stripSuffix("\\")
        case _ =>
          true
      }

  def toFile: File = Try { new File(resource.toURI()) }.recoverWith {
    case e: Throwable => Failure(new RuntimeException("Couldn't get file from " + self + " with url [" + resource.toURI() + "]. " + e.getMessage, e))
  }.get
  override def unsafeToInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  override def parentName = {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  def asFile: FileLocation = ???
  def asUrl: UrlLocation = ???
  override def build(path: String): Repr = ClassPathInputLocation(standard(path).stripPrefix(outerPrefix))
  def outerPrefix: String = absolute.stripSuffix(initialResourcePath)
  def innerPath: String = absolute.stripPrefix(outerPrefix)
}
object ClassPathInputLocation {
  def apply(initialResourcePath: String) = ClassPathInputLocationImpl(initialResourcePath)
  private def getDefaultClassLoader(): ClassLoader = {
    Try { Thread.currentThread().getContextClassLoader }.toOption.getOrElse(classOf[System].getClassLoader)
  }
  private def getSpecialClassLoader(): ClassLoader =
    (Option(classOf[ClassPathInputLocation].getClassLoader)).orElse(Option(classOf[ClassLoader].getClassLoader)).get
}
case class ClassPathInputLocationImpl(initialResourcePath: String) extends ClassPathInputLocation {

  override def toString = s"ClassPathInputLocation($initialResourcePath)"
}
case class EmailLocation(host: String, port: Int, username: String, password: String) extends NavigableFileInOutLocation { self =>
  override type Repr = self.type

  /**
   * As seen from class EmailLocation, the missing signatures are as follows.
   *  For convenience, these are usable as stub implementations.
   */
  def raw: String = ???

  override def build(path: String): Repr = ???

  def toFile: java.io.File = ???

  def unsafeToInputStream: java.io.InputStream = ???

  def asInput: NavigableFileInputLocation = ???

  override def append: Boolean = ???
  def unsafeToOutputStream: java.io.OutputStream = ???
  def withAppend: Repr = ???
}

trait AbsoluteBaseLocation extends BaseLocation with ResolvedLocationState { self =>
  type Repr = self.type
  def uniqueId: String = raw
  def toUrl: java.net.URL = ???
  def size: Long
  final def length: Long = size
  def exists: Boolean
  protected def using[A <% AutoCloseable, B](resource: A)(f: A => B): B = {
    try f(resource) finally resource.close()
  }
  implicit def toAutoCloseable(source: scala.io.BufferedSource): AutoCloseable = new AutoCloseable {
    override def close() = source.close()
  }
  def nonExisting(process: (this.type) => Any): Repr = {
    if (!exists) process(this)
    this
  }
  def existing(source: BufferedSource) = {
    //if (source.nonEmpty)
    val hasNext = Try { source.hasNext }
    val hasNext2 = hasNext.recover {
      case ex: Throwable =>
        throw new RuntimeException("[" + this + "] doesn't exist!")
    }
    //hasNext might be false if is empty
    //    if (!hasNext2.get)
    //      throw new RuntimeException("[" + self + "] doesn't have next!")
    source
  }
  def existing: Repr =
    if (exists)
      this
    else
      throw new RuntimeException("[" + this + "] doesn't exist!")
  def existingOption: Option[Repr] =
    if (exists)
      Some(this)
    else
      None
  def nonExistingOption: Option[Repr] =
    if (exists)
      None
    else
      Some(this)
}
///**Trait to mark if a location is resolved to a file system.*/
//trait FileResolvedLocationState extends ResolvedLocationState with FileVersionedLocation{self:FileAbsoluteBaseLocation =>
//  def toFile: File
//}

trait FileAbsoluteBaseLocation extends AbsoluteBaseLocation with ResolvedLocationState with FileVersionedLocation { self =>
  override type Repr = self.type
  def toFile: File
  override def toUrl: java.net.URL = toFile.toURI.toURL

  override def mimeType = ???
  /**To read data you should read the inputstream*/
  def mimeTypeFromContent = ???

  def toPath: Path = toFile.toPath
  def toPath(subFile: String): Path = toPath.resolve(subFile)
  def mkdirIfNecessary: Repr = {
    CommonsFileUtils.forceMkdir(toFile)
    this
  }
  private def listPath(glob: String): List[Path] = {
    val stream = Files.newDirectoryStream(toPath, glob)
    try stream.asScala.toList finally stream.close
  }

  def hasDirs = listPath("*").find(_.toFile.isDirectory).nonEmpty
  def isFile = toFile.isFile
  def isFolder = toFile.isDirectory
  def isSymlink = Files.isSymbolicLink(toPath)
  def symlink: Try[FileLocation] = Try { FileLocation(Files.readSymbolicLink(toPath)) }
  //TODO this one is not ok attributes.basic.isSymbolicLink
  def exists: Boolean = {
    toFile.exists()
    //    Files.exists(toPath)
    //    val a = toFile
    //    a.exists
  }
  def existsWithoutResolving = if (isSymlink)
    Files.exists(toPath, LinkOption.NOFOLLOW_LINKS)
  else
    exists
  def nameAndBefore: String = absolute
  override def size: Long = toFile.length()
  def absolute: String = standard(_.absolutePlatformDependent)
  def absoluteWindows: String = standardWindows(_.absolutePlatformDependent)
  def absolutePlatformDependent: String = toPath("").toAbsolutePath.toString
  def isAbsolute = toFile.isAbsolute()
  /**Gets only the path part (without drive name on windows for example), and without the name of file*/
  def path: String = FilenameUtils.getPath(absolute)
  def attributes: FileAttributes = FileAttributes(this)
}
/**
 * See
 * - http://javapapers.com/java/file-attributes-using-java-nio/
 * - https://jakubstas.com/links-nio-2
 * - http://www.java2s.com/Tutorials/Java/Java_io/1000__Java_nio_File_Attributes.htm
 * - https://docs.oracle.com/javase/tutorial/essential/io/fileAttr.html
 * - http://cr.openjdk.java.net/~alanb/7017446/webrev/test/java/nio/file/Files/FileAttributes.java-.html
 */
case class FileAttributes(location: FileAbsoluteBaseLocation) {
  import java.nio.file.attribute.FileOwnerAttributeView
  //  import java.nio.file.attribute.PosixFileAttributes
  //  import sun.nio.fs.WindowsFileAttributes
  //  import java.nio.file.attribute.DosFileAttributes
  //  import com.sun.nio.zipfs.ZipFileAttributes

  def basic: BasicFileAttributes = Files.readAttributes(location.toPath, classOf[BasicFileAttributes])
  //  private def zip:ZipFileAttributes = Files.readAttributes(location.toPath,classOf[ZipFileAttributes])
  //  private def dos:DosFileAttributes = Files.readAttributes(location.toPath,classOf[DosFileAttributes])
  //  def windows:WindowsFileAttributes = Files.readAttributes(location.toPath,classOf[WindowsFileAttributes])
  //  private def posix:PosixFileAttributes = Files.readAttributes(location.toPath,classOf[PosixFileAttributes])

  //  def aclView:AclFileAttributeView = Files.getFileAttributeView(location.toPath,classOf[AclFileAttributeView])
  //  def basicView:BasicFileAttributeView = Files.getFileAttributeView(location.toPath,classOf[BasicFileAttributeView])
  //  def dosView:DosFileAttributeView = Files.getFileAttributeView(location.toPath,classOf[DosFileAttributeView])
  //  def fileView:FileAttributeView = Files.getFileAttributeView(location.toPath,classOf[FileAttributeView])
  //  def fileStoreView:FileStoreAttributeView = Files.getFileAttributeView(location.toPath,classOf[FileStoreAttributeView])
  //  def posixFileView:PosixFileAttributeView = Files.getFileAttributeView(location.toPath,classOf[PosixFileAttributeView])
  //  def userDefinedView:UserDefinedFileAttributeView = Files.getFileAttributeView(location.toPath,classOf[UserDefinedFileAttributeView])

  def inode: Option[String] = {
    //code from http://www.javacodex.com/More-Examples/1/8
    Option(basic.fileKey()).map(_.toString()).map(all =>
      all.substring(all.indexOf("ino=") + 4, all.indexOf(")")))
  }
  def owner: FileOwnerAttributeView = Files.getFileAttributeView(location.toPath, classOf[FileOwnerAttributeView])
  def toMap: Map[String, Object] = {
    Files.readAttributes(location.toPath, "*").asScala.toMap
  }
  def fileStore: FileStore = Files.getFileStore(location.toPath);
}

import java.io.FileInputStream
import java.nio.file.Paths

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver

import rx.lang.scala.Observable
import rx.lang.scala.Subscription

trait FileLocation extends NavigableFileInOutLocation with FileInputLocation with FileOutputLocation { self =>
  override type Repr = self.type
  def fileFullPath: String
  override def parentName: String = toFile.getParentFile.getAbsolutePath
  def raw = fileFullPath
  def asInput: NavigableFileInputLocation = self
  lazy val toFile: File = new File(fileFullPath)
  override def toPath: Path = Paths.get(fileFullPath)
  override def unsafeToInputStream: InputStream = new FileInputStream(toFile)
  //should not throw exception but return Try?
  def checkedChild(child: String): String = {
    require(!child.endsWith(" "), "Child [" + child + "] has trailing spaces");
    require(!child.startsWith("/"), "Child [" + child + "] starts with path separator suggesting starting from root. That is not a child.");
    child
  }
  def childFile(child: String) = toPath.resolve(checkedChild(child)).toFile
  //import org.raisercostin.util.MimeTypesUtils2
  def asFile: Repr = self
  def renamed(renamer: String => String): Try[Repr] = Try {
    val newName = renamer(baseName)
    if (newName == baseName) {
      //p rintln(s"ignore [${absolute}] to [${absolute}]")
      this
    } else {
      val dest = parent.child(withExtension2(newName, extension))
      //p rintln(s"move [${absolute}] to [${dest.absolute}]")
      FileUtils.moveFile(toFile, dest.toFile)
      dest
    }
  }

  def watch(pollingIntervalInMillis: Long = 1000): Observable[FileAltered] = {
    Observable.apply { obs =>
      val observer = new FileAlterationObserver(toFile);
      val monitor = new FileAlterationMonitor(pollingIntervalInMillis);
      val fileListener = new FileAlterationListenerAdaptor() {
        //        override def onFileCreate(file: File) = {
        //          val location = Locations.file(file)
        //          try {
        //            obs.onNext(FileCreated(file))
        //          } catch {
        //            case NonFatal(e) =>
        //              obs.onError(new RuntimeException(s"Processing of [${Locations.file(file)}] failed.", e))
        //          }
        //        }
        /**File system observer started checking event.*/
        //override def onStart(file:FileAlterationObserver) = obs.onNext(FileChanged(file))
        override def onDirectoryCreate(file: File) = obs.onNext(DirectoryCreated(file))
        override def onDirectoryChange(file: File) = obs.onNext(DirectoryChanged(file))
        override def onDirectoryDelete(file: File) = obs.onNext(DirectoryDeleted(file))
        override def onFileCreate(file: File) = obs.onNext(FileCreated(file))
        override def onFileChange(file: File) = obs.onNext(FileChanged(file))
        override def onFileDelete(file: File) = obs.onNext(FileDeleted(file))
        /**File system observer finished checking event.*/
        //override def onStop(file:FileAlterationObserver) = obs.onNext(FileChanged(file))
      }
      observer.addListener(fileListener)
      monitor.addObserver(observer)
      monitor.start()
      Subscription { monitor.stop() }
    }
  }
  //  @deprecated("Use watch with observable", "0.31")
  //  def watch(pollingIntervalInMillis: Long = 1000, listener: FileLocation => Unit): FileMonitor = {
  //    FileMonitor(watchFileCreated(pollingIntervalInMillis).subscribe(file => listener.apply(file.location), error => LoggerFactory.getLogger(classOf[FileLocation]).error("Watch failed.", error)))
  //  }

  //  def copyFromFolder(src:FileLocation):Repr={
  //    src.descendants.map { x =>
  //      val rel = x.extractPrefix(src).get
  //      val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
  //      println(f"""copy ${rel.raw}%-40s $x -> $y""")
  //    }
  //    this
  //  }
  override def childName(child: String): String = childFile(child).getAbsolutePath

  override def build(path: String): Repr = FileLocation(path)
  def copyFromAsSymLinkAndGet(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Repr = copyFromAsSymLink(src, overwriteIfAlreadyExists).get
  def copyFromAsSymLink(src: FileInputLocation, overwriteIfAlreadyExists: Boolean = false): Try[Repr] = {
    ???
    if (!parent.exists) {
      Failure(new RuntimeException("Destination parent folder " + parent + " doesn't exists."))
    } else if (!overwriteIfAlreadyExists && exists) {
      Failure(new RuntimeException("Destination file " + this + " already exists."))
    } else {
      val first: Try[Repr] = Try {
        Files.createSymbolicLink(toPath, src.toPath)
        self
      }
      first.recoverWith {
        case error =>
          val symlinkType = if (src.isFile) "" else "/D"
          val second: Try[Repr] = {
            ???
          }
          second.recoverWith { case x => Failure { x.addSuppressed(first.failed.get); x } }
      }
    }
  }
}

//@deprecated("Use watch with observable", "0.31")
//case class FileMonitor(private val subscription: Subscription) {
//  def stop() = subscription.unsubscribe()
//}
sealed abstract class FileAltered {
  lazy val location: FileLocation = ???
  protected def file: File
}
case class FileCreated(file: File) extends FileAltered
case class FileChanged(file: File) extends FileAltered
case class FileDeleted(file: File) extends FileAltered
case class DirectoryCreated(file: File) extends FileAltered
/**
 * The Changed event is raised when changes are made to the size, system attributes, last write time, last access time, or security permissions of a file or directory in the directory being monitored.
 * @see https://msdn.microsoft.com/en-us/library/system.io.filesystemwatcher.changed(v=vs.110).aspx
 */
case class DirectoryChanged(file: File) extends FileAltered
case class DirectoryDeleted(file: File) extends FileAltered
object FileLocation {
  def apply(fileFullPath: String, append: Boolean = false): FileLocation = if (append) FileLocationAppendable(fileFullPath) else FileLocationImpl(fileFullPath)
  def apply(path: Path): FileLocation = apply(path, false)
  def apply(path: Path, append: Boolean): FileLocation = apply(path.toFile.getAbsolutePath, append)
}
case class FileLocationImpl(fileFullPath: String) extends FileLocation { self =>
  override type Repr = self.type
  override def withAppend: Repr = FileLocationAppendable(fileFullPath)
  override def append: Boolean = false
}
case class FileLocationAppendable(fileFullPath: String) extends FileLocation { self =>
  override type Repr = self.type
  override def withAppend: Repr = self
  override def append: Boolean = true
}

object FileUtils {
  def moveFile(srcFile: File, destFile: File) = ???
}

trait HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap
  def get(key: String): Option[String]
  def list(key: String): Option[String]
  def asMap: Map[String, Seq[String]]
}

object HierarchicalMultimap {
  def apply(): HierarchicalMultimap = EmptyHMap()
  def apply(data: String): HierarchicalMultimap = ???
  def apply(data: InputLocation): HierarchicalMultimap = load(data)
  def load(data: InputLocation): HierarchicalMultimap = {
    val prop = new java.util.Properties()
    data.usingInputStream { s => prop.load(s) }
    import scala.collection.JavaConverters._
    MapHMap2(prop.asScala.toMap.mapValues(Seq(_)))
  }
  def save(map: HierarchicalMultimap, data: OutputLocation): Try[Unit] = Try {
    println(map)
    val prop = new java.util.Properties()
    import scala.collection.JavaConverters._
    prop.putAll(map.asMap.map {
      case (x, y) if x == null => ("", toLine(y))
      case (x, y)              => (x, toLine(y))
    }.asJava)
    data.usingWriter(s => prop.store(s, "saved HMap"))
  }
  def toLine(line: Seq[String]): String = line.mkString(",")
}

case class EmptyHMap() extends HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap = this
  def get(key: String): Option[String] = None
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] = Map()
}
case class MapHMap2(map: Map[String, Seq[String]]) extends HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap = subsetString(key + ".")
  private def subsetString(key: String): HierarchicalMultimap = MapHMap2(map.filterKeys(_.startsWith(key)).map { case (k, v) => k.stripPrefix(key) -> v })
  def get(key: String): Option[String] = map.getOrElse(key, Seq()).headOption
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] = map
}
/**HMap with good performance on subset,key,list.*/
object FastHMap {
  def apply(map: Map[String, Seq[String]]): HierarchicalMultimap = FastHMap("", map)
}
case class FastHMap(prefix: String, map: Map[String, Seq[String]]) extends HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap = FastHMap(prefix + "." + key, map)
  def get(key: String): Option[String] = map.getOrElse(prefix + "." + key, Seq()).headOption
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] =
    if (prefix.isEmpty)
      map
    else
      map.filterKeys(_.startsWith(prefix + ".")).map { case (k, v) => k.stripPrefix(prefix + ".") -> v }
}

case class HttpHMap(request: Map[String, Seq[String]], response: Map[String, Seq[String]]) extends HierarchicalMultimap {
  val all = request.map { case (key, value) => "request." + key -> value } ++ response.map { case (key, value) => "response." + key -> value }
  def subset(key: String): HierarchicalMultimap = key match {
    case "request" =>
      FastHMap(request)
    case "response" =>
      FastHMap(response)
    case _ =>
      EmptyHMap()
  }
  def get(key: String): Option[String] = all.getOrElse(key, Seq()).headOption
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] = all
}

import scala.io.Codec.decoder2codec

import org.apache.commons.io.IOUtils

trait InputLocation extends AbsoluteBaseLocation with ResolvedLocationState with VersionedLocation { self =>
  override type Repr = self.type
  def unsafeToInputStream: InputStream
  def unsafeToInputStreamIfFile: InputStream = {
    //Return the InputStream only if this is a file. Classpath folder is returning an InputStream with the list of the files.
    if (!isFile)
      throw new RuntimeException("Cannot create inputStream since [" + this + "] is not a file!")
    unsafeToInputStream
  }
  def unsafeToReader: java.io.Reader = new java.io.InputStreamReader(unsafeToInputStreamIfFile, decoder)
  def unsafeToSource: scala.io.BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStreamIfFile)(decoder)
  def bytes: Array[Byte] = {
    //TODO implement
    ??? //IOUtils.readFully(x$1, x$2)
  }

  def usingInputStream[T](op: InputStream => T): T = using(unsafeToInputStreamIfFile)(op)
  def usingReader[T](reader: java.io.Reader => T): T = using(unsafeToReader)(reader)
  def usingSource[T](processor: scala.io.BufferedSource => T): T = using(unsafeToSource)(processor)

  def usingInputStreamAndContinue(op: InputStream => Any): Repr = { using(unsafeToInputStreamIfFile)(op); this }
  def usingReaderAndContinue(reader: java.io.Reader => Any): Repr = { using(unsafeToReader)(reader); this }
  def usingSourceAndContinue(processor: scala.io.BufferedSource => Any): Repr = { using(unsafeToSource)(processor); this }

  def readLines: Iterable[String] = traverseLines.toIterable
  def traverseLines: Traversable[String] = new Traversable[String] {
    def foreach[U](f: String => U): Unit = {
      usingSource { x => x.getLines().foreach(f) }
    }
  }

  def copyToIfNotExists(dest: OutputLocation): Repr = { dest.nonExistingOption.map(_.copyFrom(this)); this }
  def copyTo(dest: OutputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): Repr = { dest.copyFrom(self); this }

  def readContent = {
    // Read a file into a string
    //    import rapture._
    //    import core._, io._, net._, uri._, json._, codec._
    //    import encodings.`UTF-8`
    //    val src = uri"http://rapture.io/sample.json".slurp[Char]
    //existing(toSource).getLines mkString ("\n")
    usingReader { reader =>
      try { IOUtils.toString(reader) } catch { case x: Throwable => throw new RuntimeException("While reading " + this, x) }
    }
  }
  def readContentAsText: Try[String] =
    Try(readContent)
  //Try(existing(toSource).getLines mkString ("\n"))
  //def unzip: ZipInputLocation = ???
  def unzip: ZipInputLocation = ZipInputLocation(this, None)
  def cached(implicit cacheConfig: CacheConfig = DefaultCacheConfig): CachedLocation[this.type] = CachedLocation(cacheConfig, this)
  /**Sometimes we want the content to be available locally in the filesystem.*/
  def asFileInputLocation: FileInputLocation = cached.flush
}
trait FileInputLocation extends InputLocation with FileAbsoluteBaseLocation with VersionedLocation {
  //import org.apache.commons.io.input.BOMInputStream
  //import org.apache.commons.io.IOUtils
  //def toBomInputStream: InputStream = new BOMInputStream(unsafeToInputStream,false)
  //def toSource: BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream, "UTF-8")

  def unsafeToInputStream: InputStream = new FileInputStream(absolute)
  override def bytes: Array[Byte] = org.apache.commons.io.FileUtils.readFileToByteArray(toFile)
  def copyAsHardLink(dest: FileOutputLocation, overwriteIfAlreadyExists: Boolean = false): this.type = {
    dest.copyFromAsHardLink(this, overwriteIfAlreadyExists);
    this
  }
  def copyAsSymLink(dest: FileLocation, overwriteIfAlreadyExists: Boolean = false): this.type = {
    dest.copyFromAsSymLink(this, overwriteIfAlreadyExists);
    this
  }
  /**Optimize by using the current FileInputLocation.*/
  override def asFileInputLocation: FileInputLocation = this
}

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

import scala.Iterable

case class MemoryLocation(val memoryName: String) extends RelativeLocation with InOutLocation with NavigableInputLocation { self =>
  override type Repr = self.type
  override def nameAndBefore: String = absolute
  def absolute: String = memoryName
  def relativePath: String = memoryName
  override def raw = memoryName
  def asInput: MemoryLocation = this
  override def append: Boolean = false
  //val buffer: Array[Byte] = Array()
  lazy val outStream = new ByteArrayOutputStream()
  override def unsafeToOutputStream: OutputStream = outStream
  override def unsafeToInputStream: InputStream = new ByteArrayInputStream(outStream.toByteArray())
  override def child(child: String): Repr = ???
  override def build(path: String): Repr = new MemoryLocation(path)
  override def parent: Repr = ???
  override def withAppend: this.type = ???
  override def size: Long = outStream.size()
  override def exists = true
  override def descendantsWithOptions(traverseDir: Boolean): Iterable[Repr] = Iterable(this)
  override def list: Iterable[Repr] = Iterable(this)
  override def childName(child: String): String = ???
  override def isFolder = false
  override def isFile: Boolean = true
  override def metaLocation: Try[MetaRepr] = ???
}

import scala.annotation.tailrec

object BaseNavigableLocation {
  val stateSep = "--state#"
}
trait BaseNavigableLocation extends BaseLocation with LocationState { self =>
  type Repr = self.type
  protected def repr: Repr = toRepr(self)
  implicit protected def toRepr[T <: BaseNavigableLocation](location: T): Repr = location.asInstanceOf[Repr]
  def build(path: String): Repr

  def parent: Repr = build(parentName)
  def child(child: String): Repr = build(childName(child))
  def childName(child: String): String = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    ???
  }

  def child(childText: Option[String]): Repr = childText match {
    case None                      => repr
    case Some(s) if s.trim.isEmpty => repr
    case Some(s)                   => child(s)
  }
  def child(childLocation: RelativeLocation): Repr = {
    if (childLocation.isEmpty) {
      repr
    } else {
      child(childLocation.relativePath)
    }
  }
  def descendant(childs: Seq[String]): Repr = if (childs.isEmpty) repr else child(childs.head).descendant(childs.tail)
  def withParent(process: (Repr) => Any): Repr = {
    process(parent)
    repr
  }
  def withSelf(process: (Repr) => Any): Repr = {
    process(repr)
    repr
  }
  /**This one if folder otherwise the parent*/
  def folder: Repr = {
    if (isFile)
      parent
    else
      this
  }
  def ancestorOf[T <: BaseLocation](folder: T): Boolean = ancestor(folder) == this
  @deprecated("use childOf")
  def hasAncestor[T <: BaseLocation](folder: T): Boolean = childOf(folder)
  def childOf[T <: BaseLocation](folder: T): Boolean = ancestor(folder) == folder
  /** Finds the common ancestor of current Location and the src location. A folder should end in `/`. */
  def ancestor[T <: BaseLocation](src: T*): Repr = build(src.foldLeft(this.nameAndBefore)((x, file) => folderCommonPrefix(x, file.nameAndBefore)))
  //  private def ancestor2[T<:BaseLocation](a:String,b:String):Repr = {
  //    build(getFolderCommonPrefix(a,b))
  //  }
  private def ancestor3(a: String, b: String): String = {
    folderCommonPrefix(a, b)
  }
  private def folderCommonPrefix(a: String, b: String): String = {
    ???
  }
  //see org.apache.commons.lang3.StringUtils
  private def commonPrefix(a: String, b: String): String = {
    var i = 0
    val maxi = Math.min(a.length, b.length)
    while (i < maxi && a(i) == b(i)) i += 1
    a.substring(0, i)
  }
  def parentName: String = //toFile.getParentFile.getAbsolutePath
    Option(FilenameUtils.getFullPathNoEndSeparator(nameAndBefore)).getOrElse("")
  def extractPrefix(ancestor: BaseNavigableLocation): Try[RelativeLocation] = ???
  def extractAncestor(ancestor: BaseNavigableLocation): Try[Seq[String]] =
    ???
  def diff(text: String, prefix: String) =
    if (text.startsWith(prefix))
      Success(text.substring(prefix.length))
    else
      Failure(new RuntimeException(s"Text [$text] doesn't start with [$prefix]."))
  def withBaseName(baseNameSupplier: String => String): Repr = parent.child(withExtension2(baseNameSupplier(baseName), extension))
  def withBaseName2(baseNameSupplier: String => Option[String]): Repr =
    baseNameSupplier(baseName).map { x => parent.child(withExtension2(x, extension)) }.getOrElse(repr)
  def withName(nameSupplier: String => String): Repr = parent.child(nameSupplier(name))
  def withExtension(extensionSupplier: String => String): Repr = parent.child(withExtension2(baseName, extensionSupplier(extension)))
  protected def withExtension2(name: String, ext: String) =
    if (ext.length > 0)
      name + "." + ext
    else name

  /** State is a part before extension that can be used to add minimal metadata to your file.*/
  def withState(state: String): Repr = {
    import BaseNavigableLocation._
    val index = baseName.lastIndexOf(stateSep)
    if (index == -1)
      if (state.isEmpty())
        //unchanged - now existing state, no new state
        this
      else
        //add new state
        withBaseName(_ + stateSep + state)
    else if (state.isEmpty())
      //remove old state - no new state
      withBaseName(_ => baseName.substring(0, index))
    else
      //replace old state
      withBaseName(_ => baseName.substring(0, index + stateSep.length) + state)
  }
  def withoutState = withState("")
}
trait NavigableLocation extends BaseNavigableLocation with AbsoluteBaseLocation { self =>
  override type Repr = self.type
  def list: Iterable[Repr]
  final def descendants: Iterable[Repr] = descendantsWithOptions(true)
  def descendantsWithOptions(traverseDir: Boolean): Iterable[Repr] = ???
  def absolute: String
  //  def loop(h: Int, n: Int): Stream[Int] = h #:: loop(n, h + n)
  //  loop(1, 1)
  //  def descendantsStream: Stream[Repr] =
  //  def descendants: Iterable[Repr] = {
  //    val all: Iterable[File] = Option(existing).map {  x =>
  //      list.map()
  //      traverse.map(_._1.toFile).toIterable
  //    }.getOrElse(Iterable[File]())
  //    all.map(buildNewFile)
  //  }
}
trait NavigableFileLocation extends FileAbsoluteBaseLocation with BaseNavigableLocation with NavigableLocation { self =>
  override type Repr = self.type
  //TODO review these
  override protected def repr: Repr = toRepr2(self)
  implicit protected def toRepr2[T <: NavigableFileLocation](location: T): Repr = location.asInstanceOf[Repr]
  def isEmptyFolder = list.isEmpty

  def list: Iterable[Repr] = Option(existing).map { x =>
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(buildFromFile)
  override def descendantsWithOptions(includeDirs: Boolean): Iterable[Repr] = {
    val all: Iterable[File] = Option(existing).map { x =>
      traverse(includeDirs).map(_._1.toFile).toIterable
    }.getOrElse(Iterable[File]())
    all.map(buildFromFile)
  }
  def traverse(includeDirs: Boolean): Traversable[(Path, BasicFileAttributes)] = ???
  //def traverseFiles: Traversable[Path] = if (exists) traverse.map { case (file, attr) => file } else Traversable()
  //def traverseWithDir = new TraversePath(toPath, true)

  override def build(path: String): Repr = ???
  protected def buildFromFile(x: File): Repr = build(x.getAbsolutePath)
  def renamedIfExists: Repr = renamedIfExists(true)
  def renamedIfExists(renameIfEmptyToo: Boolean = false): Repr = {
    @tailrec
    def findUniqueName(destFile: Repr, counter: Int): Repr = {
      val renamed = destFile.withBaseName { baseName: String => (baseName + "-" + counter) }
      if (renamed.existsWithoutResolving)
        if (renameIfEmptyToo && exists && list.isEmpty)
          renamed
        else
          findUniqueName(destFile, counter + 1)
      else
        renamed
    }
    if (repr.existsWithoutResolving)
      findUniqueName(repr, 1)
    else
      repr
  }
}

trait NavigableFileOutputLocation extends OutputLocation with NavigableFileLocation with NavigableOutputLocation { self =>
  override type Repr = self.type

  def mkdirOnParentIfNecessary: Repr = {
    parent.mkdirIfNecessary
    this
  }
  def deleteOrRenameIfExists: Repr = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
  def asInput: NavigableFileInputLocation
  @tailrec final def moveToRenamedIfExists(dest: NavigableFileOutputLocation): this.type = {
    try {
      FileUtils.moveFile(toFile, dest.toFile)
      this
    } catch {
      case _: Exception =>
        moveToRenamedIfExists(dest.renamedIfExists)
    }
  }
  private def backupExistingOneAndReturnBackup(backupEmptyFolderToo: Boolean = true): Repr = {
    //new name should never exists
    val newName: NavigableFileLocation = renamedIfExists(false)
    if (!newName.equals(this)) {
      if (!backupEmptyFolderToo && newName.exists && newName.isEmptyFolder)
        newName.delete
      renameTo(newName)
    } else
      this
  }
  def backupExistingOne(onBackup: Repr => Unit, backupIfEmpty: Boolean = true): Repr = {
    onBackup(backupExistingOneAndReturnBackup(backupIfEmpty))
    this
  }
  def backupExistingOne(backupEmptyFolderToo: Boolean): Repr = {
    backupExistingOneAndReturnBackup(backupEmptyFolderToo)
    this
  }
  def backupExistingOne: Repr = backupExistingOne(true)

  def renameTo[T <: FileAbsoluteBaseLocation](newName: T): T = {
    if (isSymlink)
      Files.move(toPath, newName.toPath)
    else
      FileUtils.moveFile(toFile, newName.toFile)
    newName
  }
}

import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.StandardCharsets

import Locations.logger

object CopyOptions {
  def copyWithoutMetadata: CopyOptions = CopyOptions(false, false)
  def copyWithMetadata: CopyOptions = CopyOptions(true, false)
  def copyWithOptionalMetadata: CopyOptions = CopyOptions(true, true)
  def simpleCopy: CopyOptions = copyWithOptionalMetadata
}
trait OperationMonitor {
  def warn(message: => String)
}
trait SlfLogger { self =>
  lazy val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(self.getClass);
}

object SlfLogger extends SlfLogger
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
        case (f: BaseNavigableLocation, t: BaseNavigableLocation) =>
          if (t.childOf(f))
            throw new RuntimeException(s"You tried to copy ${from} to child ${to}.")
        case _ =>
      }
    true
  }
}

//TODO add DeletableLocation?
trait OutputLocation extends AbsoluteBaseLocation { self =>
  override type MetaRepr <: OutputLocation with InputLocation
  override type Repr = self.type
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
  def usingOutputStreamAndContinue(op: OutputStream => Any): Repr = { using(unsafeToOutputStream2)(op); this }
  /** Produce lateral effects in op.*/
  def usingWriterAndContinue(op: Writer => Any): Repr = { using(unsafeToWriter)(op); this }
  /** Produce lateral effects in op.*/
  def usingPrintWriterAndContinue(op: PrintWriter => Any): Repr = { using(unsafeToPrintWriter)(op); this }

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
  def copyFromWithoutMetadata(src: InputLocation): Repr = copyFrom(src)(CopyOptions.copyWithoutMetadata)
  def copyFromWithMetadata(src: InputLocation): Repr = copyFrom(src)(CopyOptions.copyWithMetadata)

  def copyFrom(src: InputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): Repr = ???
  private def copyFromIncludingMetadata(src: InputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): Repr =
    ???
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
  def moveInto(dest: OutputLocation): this.type = dest match {
    case d: FileOutputLocation =>
      FileUtils.moveFile(toFile, d.child(name).toFile)
      this
    case _ =>
      ???
  }
  override def deleteIfExists: Repr = {
    if (exists) {
      logger.info(s"delete existing $absolute")
      ???
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
        if (!src.isFile)
          throw new RuntimeException("Cannot create a hardLink. Source " + src + " is not a file.")
        Files.createLink(toPath, src.toPath)
      }
    }
    this
  }
}

object RelativeLocation {
  def apply(relativePath: String) = RelativeLocationImpl(relativePath)
}
trait RelativeLocation extends BaseNavigableLocation with UnresolvedLocationState { self =>
  override type Repr = self.type
  override def nameAndBefore: String = relativePath
  override def raw: String = relativePath

  def relativePath: String
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
  override def childName(child: String): String = ???
  def relativePath(separator: String): String = ???
  override def build(path: String): Repr = RelativeLocation(path)
}
case class RelativeLocationImpl(relativePath: String) extends RelativeLocation with UnknownFileOrFolder {
  ???
}

case class StreamLocation(val inputStream: InputStream) extends InputLocation with IsFile {
  def exists: Boolean = true
  def nameAndBefore: String = inputStream.toString()
  def raw = "inputStream[" + inputStream + "]"
  override def unsafeToInputStream: InputStream = inputStream
  override def size: Long = ???
  def metaLocation: Try[MetaRepr] = ???
}

case class StreamProviderLocation(inputStream: () => InputStream) extends InputLocation with IsFile {
  def exists: Boolean = true
  def nameAndBefore: String = inputStream.toString()
  def raw = "inputStream[" + inputStream + "]"
  override def unsafeToInputStream: InputStream = inputStream.apply()
  override def size: Long = ???
  def metaLocation: Try[MetaRepr] = ???
}

case class TempLocation(temp: File, append: Boolean = false) extends FileLocation { self =>
  override type Repr = self.type
  override def withAppend: self.type = this.copy(append = true)
  def fileFullPath: String = temp.getAbsolutePath()
  def randomChild(prefix: String = "random", suffix: String = "") = new TempLocation(File.createTempFile(prefix, suffix, toFile))
  def randomFolderChild(prefix: String = "random") = new TempLocation(Files.createTempDirectory(prefix).toFile)
  override def build(path: String): Repr = new TempLocation(new File(path), append)
  //optimized not to convert back and forth to the external format
  override def child(child: String): Repr = new TempLocation(childFile(child))
}

object HttpConfig {
  val defaultConfig: HttpConfig = HttpConfig(header = Map(
    "User-Agent" -> "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36",
    "Accept" -> "*/*"))
}
case class HttpConfig(header: Map[String, String] = Map(), allowedRedirects: Int = 5, connectionTimeout: Int = 10000, readTimeout: Int = 15000, useScalaJHttp: Boolean = true) {
  def followRedirects = allowedRedirects > 0
  def configureConnection(conn: HttpURLConnection): Unit = {
    header.foreach(element => conn.setRequestProperty(element._1, element._2))
    conn.setInstanceFollowRedirects(followRedirects)
    conn.setConnectTimeout(connectionTimeout)
    conn.setReadTimeout(readTimeout)
  }
  /**Usually needed if a 403 is returned.*/
  def withBrowserHeader: HttpConfig = HttpConfig(header = this.header + (
    "User-Agent" -> "curl/7.51.0",
    "Accept" -> "*/*"))
  //Other useful settings:
  //"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
  //"Connection" -> "keep-alive"
  //User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36
  //val cookies = "osCsid=d9bbb1602e315dadfe4a5b6e07832053; MIsid=18d1653b970413564d19469c06c8ebad"
  //Set-Cookie: osCsid=d9bbb1602e315dadfe4a5b6e07832053; path=/;
  //val userAgent = "User-Agent" -> "Wget/1.9"
  //MIsid 20b3258abfd25dfda1d9a2a04088f577
  //Http.configure(_ setFollowRedirects true)(q OK as.String)
  //"Connection" -> "Keep-Alive", "Cookie" -> cookies)
  def withJavaImpl: HttpConfig = this.copy(useScalaJHttp = false)
  def withAgent(newAgent: String) = this.copy(header = header + ("User-Agent" -> newAgent))
  def withoutAgent = this.copy(header = header - "User-Agent")
}
object UrlLocation extends SlfLogger
/**
 * See here for good behaviour: https://www.scrapehero.com/how-to-prevent-getting-blacklisted-while-scraping/
 */
case class UrlLocation(url: java.net.URL, redirects: Seq[UrlLocation] = Seq(), config: HttpConfig = HttpConfig.defaultConfig) extends InputLocation with IsFile { self =>
  override type MetaRepr = MemoryLocation
  def exists = ???
  def raw = url.toExternalForm()
  //TODO dump intermediate requests/responses
  override def toUrl: java.net.URL = url
  override def nameAndBefore: String = url.getPath
  def toFile: File = ???
  import java.net._
  override def size: Long = lengthTry.get

  //TODO sending the current etag as well and wait for 302 not modified? This will save one more connection. Maybe this should be managed in a CachedUrlLocation?
  def etagFromHttpRequestHeader: Option[String] = headConnection { conn => conn.getHeaderField("ETag").stripPrefix("\"").stripSuffix("\"") }.toOption
  def headConnection[T](openedHeadConnection: URLConnection => T): Try[T] = ???
  def metaLocation: Try[MetaRepr] = {
    val out = ???
    HierarchicalMultimap.save(meta.get, out).map(_ => out)
  }
  /**
   * InputLocations should have metadata. Worst case scenario in a separate file or other files in the filesystem.
   * See .svn, .csv, .git, dos navigator, .info files, nio meta/user attributes etc.
   */
  override def meta: Try[HttpHMap] = headConnection { conn =>
    conn match {
      case conn: HttpURLConnection =>
        if (conn.getResponseCode != 200)
          throw new RuntimeException("A redirect is needed. Cannot compute size!")
        import scala.collection.JavaConverters._
        HttpHMap(conn.getRequestProperties.asScala.toMap.mapValues(_.asScala), conn.getHeaderFields.asScala.toMap.mapValues(_.asScala))
      case conn: FileURLConnection =>
        HttpHMap(Map(), Map())
    }
  }
  def lengthTry: Try[Long] = headConnection { conn =>
    conn match {
      case conn: HttpURLConnection =>
        if (conn.getResponseCode != 200)
          throw new RuntimeException("A redirect is needed. Cannot compute size!")
        val len = conn.getContentLengthLong()
        if (len < 0) throw new RuntimeException("Invalid length " + len + " received!")
        len
      case conn: FileURLConnection =>
        //conn.getInputStream
        val len = conn.getContentLengthLong()
        if (len < 0) throw new RuntimeException("Invalid length " + len + " received!")
        len
    }
  }
  override def unsafeToInputStream: InputStream =
    if (config.useScalaJHttp)
      unsafeToInputStreamUsingScalaJHttp
    else
      unsafeToInputStreamUsingJava

  private def createRequester(url: String) = {
    ???
  }
  def unsafeToInputStreamUsingScalaJHttp: InputStream = {
    ???
  }

  //protected override
  def unsafeToInputStreamUsingJava: InputStream = {
    url.openConnection() match {
      case conn: HttpURLConnection =>
        config.configureConnection(conn)
        import scala.collection.JavaConverters._
        UrlLocation.logger.info("header:\n" + config.header.mkString("\n    "))
        UrlLocation.logger.info(s"RequestHeaders for $raw:\n    " + conn.getRequestProperties.asScala.mkString("\n    "))
        //if (UrlLocation.log.isDebugEnabled())
        UrlLocation.logger.info(s"ResponseHeaders for $raw:\n    " + Try { conn.getHeaderFields.asScala.mkString("\n    ") })
        handleCode(conn.getResponseCode, conn.getHeaderField("Location"), { conn.getInputStream }, Try { conn.getHeaderFields.asScala.toMap })
      case conn =>
        conn.getInputStream
    }
  }

  def handleCode(code: Int, location: String, stream: => InputStream, map: => Try[Map[String, _]]): InputStream =
    (code, location) match {
      case (200, _) =>
        stream
      case (code, location) if config.allowedRedirects > redirects.size && location != null && location.nonEmpty && location != raw =>
        //This is manual redirection. The connection should already do all the redirects if config.allowedRedirects is true
        closeStream(stream)
        UrlLocation(new java.net.URL(location), this +: redirects, config).unsafeToInputStream
      case (code, _) =>
        closeStream(stream)
        throw new HttpStatusException(s"Got $code response from $this. A 200 code is needed to get an InputStream. The header is\n    " + map.getOrElse(Map()).mkString("\n    ")
          + " After " + redirects.size + " redirects:\n    " + redirects.mkString("\n    "), code, this)
    }
  /**
   * Shouldn't disconnect as it "Indicates that other requests to the server are unlikely in the near future."
   * We should just close() on the input/output/error streams
   * http://stackoverflow.com/questions/15834350/httpurlconnection-closing-io-streams
   */
  def closeStream(stream: => InputStream) = Try {
    if (stream != null)
      stream.close
  }.recover { case e => UrlLocation.logger.debug("Couldn't close input/error stream to " + this, e) }

  def withSensibleAgent = withAgent("User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
  def withAgent(newAgent: String) = this.copy(config = config.withAgent(newAgent))
  def withoutAgent = this.copy(config = config.withoutAgent)
  def withBrowserHeader = this.copy(config = config.withBrowserHeader)
  def withoutRedirect = this.copy(config = config.copy(allowedRedirects = 0))
  def resolved: ResolvedUrlLocation = ResolvedUrlLocation(this)
  def withJavaImpl = this.copy(config = config.withJavaImpl)

  override def etag: String = etagFromHttpRequestHeader.getOrElse("")
}
//TODO add a resolved state where you can interrogate things like All redirects headers, status code and others.
case class ResolvedUrlLocation(location: UrlLocation) {
}
case class HttpStatusException(message: String, code: Int, url: UrlLocation) extends IOException(message)

import java.util.UUID

import org.apache.commons.codec.digest.DigestUtils

/**
 * A versioned location needs to be resolved since the version refers to the content.
 * For a versioned location you can get an etag that can be used later to detect if file was changed.
 * This might be needed for implementing caching mechanisms.
 *
 * Type of etags:
 * - weak - they only indicate two representations are semantically equivalent.
 * - strong
 * -- shallow - see apache implementation http://httpd.apache.org/docs/2.0/mod/core.html#fileetag based on
 * --- inode - they vary from system to system - see inode in java http://www.javacodex.com/More-Examples/1/8
 * --- mtime
 * --- size
 * --- all
 * -- deep
 *
 * Resources:
 * - https://bitworking.org/news/150/REST-Tip-Deep-etags-give-you-more-benefits
 * - https://unix.stackexchange.com/questions/192800/does-the-inode-change-when-renaming-or-moving-a-file
 * - http://bitworking.org/news/ETags__This_stuff_matters
 * - https://www.infoq.com/articles/java7-nio2
 */
trait VersionedLocation extends ResolvedLocationState {
  /**
   * In worst case every location is considered to have a different version indifferent of content
   * Two files with same version should likely be identical.
   * Problems:
   * - files with same content on different servers
   * => Compute a fast md5 on size, start, end?
   * => Make sure they are replicated with the same **controlled** name, timestamp etc.
   * Solution)
   * A file could have a name like: <name>-<changeTimestamp>-<counter>.<extension>
   * - When reading the file with specific name and latest changeTimestamp is returned. Version is the <changeTimestamp>-<counter>.
   * - On replicated they should see the same "change" so a file with identical name.
   * - A separator is needed for versioned files that implement this policy.
   */
  def version: String = UUID.randomUUID().toString()
  def versionOfContent: String = ???
  /**The default etag is based on the strongETag.*/
  def etag: String = strongETag
  /**The efficient strong tag is a shallow one.*/
  def strongETag: String = shallowETag
  /**A not so efficient strong tag that is based on the content.*/
  def strongDeepETag: String = DigestUtils.sha1Hex(versionOfContent)
  /**The shallowETag shouldn't need access to content. The default one is a sha1Hex of the `version`.*/
  def shallowETag: String = DigestUtils.sha1Hex(version)
  /**
   * A weak ETag doesn't change if two representations are semantically equivalent.
   * After removal of a timestamp from content for example.
   * It is hard to compute and is not sure what it means.
   */
  @deprecated("Use strongETag since a weak etag is not clear how to be computed.", "0.33")
  def weakETag: String = throw new RuntimeException("Is not clear how to compute them.")
}

trait FileVersionedLocation extends VersionedLocation { self: FileAbsoluteBaseLocation =>
  override def uniqueId: String = attributes.inode.getOrElse(DigestUtils.sha1Hex(canonicalOverSymLinks))
  def canonical = toFile.getCanonicalPath
  def canonicalOverSymLinks = {
    target(toPath, 10).right.get.toFile().getCanonicalPath
  }
  //TODO transform it into a Stream or better an Observable?
  @tailrec private def target(path: Path, maxDepth: Int): Either[Path, Path] =
    if (maxDepth <= 0)
      Left(path)
    else if (Files.isSymbolicLink(path))
      target(Files.readSymbolicLink(path), maxDepth - 1)
    else
      Right(path)

  def versionFromUniqueId: String = uniqueId
  def versionFromModificationTime: String = attributes.basic.lastModifiedTime().toMillis().toString
  private def versionFromSize: String = toFile.length().toString

  //inspired by http://httpd.apache.org/docs/2.0/mod/core.html#fileetag
  override def version: String = versionFromUniqueId + "-" + versionFromModificationTime + "-" + versionFromSize
}

import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.VFS
import org.raisercostin.jedi.impl.Escape
import org.raisercostin.jedi.impl.Escape
import org.raisercostin.jedi.impl.Escape

object VfsLocation {
  private val fsManager = VFS.getManager()
  def apply(url: String): VfsLocation = VfsLocation(fsManager.resolveFile(url))
}

case class VfsLocation(file: FileObject) extends NavigableInOutLocation { self =>
  override type Repr = self.type
  def raw = file.getName.getPath
  def fileFullPath: String = file.getName.getPath
  override def build(path: String): Repr = ??? //new VfsLocation(file)
  def buildNew(x: FileObject): Repr = new VfsLocation(x)
  override def parent: Repr = buildNew(file.getParent)
  override def child(child: String): Repr = buildNew(file.getChild(child))
  override def childName(child: String): String = file.getChild(child).getName.getPath
  override def exists: Boolean = file.exists
  override def list: Iterable[Repr] = Option(existing).map { x =>
    Option(x.file.getChildren).map(_.toIterable).getOrElse(Iterable(x.file))
  }.getOrElse(Iterable()).map(buildNew)
  def isFile: Boolean = file.isFile
  def isFolder: Boolean = file.isFolder
  def mkdirIfNecessary: Repr = {
    file.createFolder
    this
  }
  def mkdirOnParentIfNecessary: Repr = {
    file.getParent.createFolder
    this
  }
  def asInput: NavigableInOutLocation = self
  override def append: Boolean = ???
  def withAppend: Repr = ???
  override def size: Long = file.getContent.getSize
  override def nameAndBefore: String = raw
  override def name: String = file.getName.getBaseName
  def external: VfsLocation = {
    var all = file.getURL.toExternalForm
    all = all.dropWhile { x => x != ':' }.drop(1)
    all = all.reverse.dropWhile { x => x != '!' }.drop(1).reverse
    VfsLocation(all);
  }
  override def absolute: String = file.getName.getExtension
  override def toString = {
    "VfsLocation[url=" + file.getURL + "]"
  }
  println(file)
  def withProtocol(protocol: String): VfsLocation = {
    val newUrl = protocol + ":" + file.getURL.toString + "!/"
    println("new " + newUrl)
    VfsLocation(newUrl)
  }
  override def unsafeToInputStream = file.getContent.getInputStream
  override def unsafeToOutputStream = file.getContent.getOutputStream
}

object ZipInputLocation {
  def apply(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) = ZipInputLocationImpl(zip, entry)
}
//TODO fix name&path&unique identifier stuff
trait ZipInputLocation extends NavigableFileInputLocation { self =>
  override type Repr = self.type
  def zip: InputLocation
  def entry: Option[java.util.zip.ZipEntry]
  def raw = "ZipInputLocation[" + zip + "," + entry + "]"

  //def toFile: File = zip.toFile
  override def unsafeToInputStream: InputStream = ???

  protected lazy val rootzip = ???
  import java.util.zip._
  protected lazy val entries: Iterable[ZipEntry] = ???
  override def name = entry.map(_.getName).getOrElse(zip.name + "-unzipped")
  override def unzip: ZipInputLocation = ???
  override def child(child: String): Repr = ???
  override def list: Iterable[Repr] = Option(existing).map(_ => entries).getOrElse(Iterable()).map(entry => toRepr(ZipInputLocation(zip, Some(entry))))
  override def toFile = zip match {
    case zip: FileAbsoluteBaseLocation => zip.toFile
    case _                             => println(zip.toString()); ???
  }
  override def build(path: String): Repr = ???
  override def parent: Repr = ???
  override def childName(child: String): String = ???
}
case class ZipInputLocationImpl(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) extends ZipInputLocation {
  override def toString = s"ZipInputLocation($zip,$entry)"
}

/**
 * Should take into consideration several composable/ortogonal aspects:
 * - end of line: win,linux,osx - internal standard: \n
 * - file separator: win,linux,osx - internal standard: / (like linux, fewer colisions with string escaping in java)
 * - file name case sensitivity - internal standard: case sensible (in windows there will not be any problem)
 * - win 8.3 file names vs. full file names - internal standard: utf-8
 *
 * In principle should be agnostic to these aspects and only at runtime will depend on the local environment.
 */
trait InOutLocation extends InputLocation with OutputLocation
trait NavigableInputLocation extends InputLocation with NavigableLocation { self =>
  override type MetaRepr = self.type
  override type Repr = self.type
  def copyToFolder(to: NavigableOutputLocation): Repr = {
    to.copyFromFolder(self)
  }
  def metaLocation: Try[MetaRepr] = Try { withName(_ + ".meta") }
}
trait NavigableOutputLocation extends OutputLocation with NavigableLocation { self =>
  override type Repr = self.type
  type InputPairType = NavigableInputLocation
  def asInput: InputPairType
  def mkdirIfNecessary: Repr
  def mkdirOnParentIfNecessary: Repr
  def copyFromFolder(src: NavigableInputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): Repr = {
    if (!src.isFolder)
      throw new RuntimeException(s"Src $src is not a folder")
    if (option.checkCopyToSame(src, this))
      src.descendants.map { x =>
        val rel = x.extractPrefix(src).get
        val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
        println(f"""copy ${rel.raw}%-40s $x -> $y""")
      }
    this
  }
  def copyFromFileToFileOrFolder(from: InputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): Repr = {
    def copyMeta(meta: Try[MetaRepr]): Unit = {
      if (option.copyMeta) {
        if (!option.optionalMeta || meta.isSuccess && meta.get.exists)
          meta.get.copyFromInputLocation(from.metaLocation.get)
        else
          option.monitor.warn("Optional meta " + meta + " doesn't exists. Ignored.")
      }
    }

    mkdirOnParentIfNecessary
    if (isFolder) {
      copyMeta(child(from.name).metaLocation)
      child(from.name).copyFromInputLocation(from)
    } else {
      copyMeta(metaLocation)
      copyFromInputLocation(from)
    }
  }
}
