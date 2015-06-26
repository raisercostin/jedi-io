package org.raisercostin.util.io

import java.nio.file.Paths
import java.io.File
import java.nio.file.Path
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
import java.io.OutputStream
import org.apache.commons.io.FilenameUtils
import java.io.InputStream
import scala.collection.generic.CanBuildFrom
import scala.io.BufferedSource
import java.io.FileInputStream
import java.io.ByteArrayInputStream
import java.io.OutputStreamWriter
import java.io.Writer
import scala.io.Source
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.PrintWriter
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.util.regex.Pattern
import scala.util.Failure
import org.apache.commons.io.IOUtils
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.security.AccessController
import Locations._
import java.io.IOException
import scala.annotation.tailrec
import java.io.Closeable

/**
 * Should take into consideration several composable/ortogonal aspects:
 * - end of line: win,linux,osx - internal standard: \n
 * - file separator: win,linux,osx - internal standard: / (like linux, fewer colisions with string escaping in java)
 * - file name case sensitivity - internal standard: case sensible (in windows there will not be any problem)
 * - win 8.3 file names vs. full file names - internal standard: utf-8
 *
 * In principle should be agnostic to these aspects and only at runtime will depend on the local environment.
 */
//
//trait LocationFactory[-From, +To] {
//  def apply(child:String) : To
//}
trait NavigableLocation[Self <: NavigableLocation[Self]] { self: Self =>
  //trait NavigableLocation[Self]{self:Self=>
  //
  //  implicit val newNavigableLocation = new LocationFactory[NavigableLocation,NavigableLocation](){
  //    def apply(child:String):NavigableLocation = ???
  //  }
  //  type Self = NavigableLocation

  def parent: Self

  //  def child(child:String):Self //f.apply(child)

  def child(child: String): Self

  def child(childText: Option[String]): Self = childText match {
    case None => self
    case Some(s) if s.trim.isEmpty => self
    case Some(s) => child(s)
  }
  def child(childLocation: RelativeLocation): Self = {
    if (childLocation.isEmpty) {
      self
    } else {
      child(childLocation.relativePath)
    }
  }
  def descendant(childs: Seq[String]): Self = if (childs.isEmpty) self else child(childs.head).descendant(childs.tail)
  def withParent(process: (Self) => Any): Self = {
    process(parent)
    self
  }
  def withSelf(process: (Self) => Any): Self = {
    process(self)
    self
  }
  def nameAndBefore: String
  def extension: String = FilenameUtils.getExtension(nameAndBefore)
  def name: String = FilenameUtils.getName(nameAndBefore)
  def baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  def parentName: String = //toFile.getParentFile.getAbsolutePath
    Option(FilenameUtils.getFullPathNoEndSeparator(nameAndBefore)).getOrElse("")
  def extractPrefix(ancestor: NavigableLocation[Self]): Try[RelativeLocation] =
    extractAncestor(ancestor).map(x => relative(FileSystem.constructPath(x))(FileSystem.identityFormatter))
  def extractAncestor(ancestor: NavigableLocation[Self]): Try[Seq[String]] =
    diff(nameAndBefore, ancestor.nameAndBefore).map { FileSystem.splitPartialPath }
  def diff(text: String, prefix: String) =
    if (text.startsWith(prefix))
      Success(text.substring(prefix.length))
    else
      Failure(new RuntimeException(s"Text [$text] doesn't start with [$prefix]."))
  def withBaseName(baseNameSupplier: String => String): Self = parent.child(withExtension2(baseNameSupplier(baseName), extension))
  def withBaseName2(baseNameSupplier: String => Option[String]): Self =
    baseNameSupplier(baseName).map { x => parent.child(withExtension2(x, extension)) }.getOrElse(self)
  def withName(nameSupplier: String => String): Self = parent.child(nameSupplier(name))
  def withExtension(extensionSupplier: String => String): Self = parent.child(withExtension2(baseName, extensionSupplier(extension)))
  protected def withExtension2(name: String, ext: String) =
    if (ext.length > 0)
      name + "." + ext
    else name
}
//class StandardFileSystem extends FileSystem
trait NavigableBaseLocation[Self <: NavigableBaseLocation[Self]] extends NavigableLocation[Self] with BaseLocation { self: Self =>
  def list: Iterable[Self] = Option(existing).map { x =>
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(Locations.file(_).asInstanceOf[Self])
  def descendants: Iterable[Self] = {
    val all: Iterable[File] = Option(existing).map { x =>
      traverse.map(_._1.toFile).toIterable
    }.getOrElse(Iterable[File]())
    all.map(buildNew)
  }

  def buildNew(x: File): Self = Locations.file(x).asInstanceOf[Self]
  def renamedIfExists: Self = {
    @tailrec
    def findUniqueName(destFile: Self, counter: Int): Self =
      if (destFile.exists)
        findUniqueName(destFile.withBaseName { baseName: String => (baseName + "-" + counter) }, counter + 1)
      else
        destFile
    findUniqueName(self, 1)
  }
}
trait BaseLocation {
  //trait BaseLocation[+Self] extends NavigableLocation[Self]{self:Self=>
  //  override type Self = BaseLocation
  def raw: String
  /**To read data you should read the inputstream*/
  def toUrl: java.net.URL = toFile.toURI.toURL
  def toFile: File
  def toPath: Path = toFile.toPath
  def toPath(subFile: String): Path = toPath.resolve(subFile)
  def mimeType = mimeTypeFromName.orElse(mimeTypeFromContent)
  def mimeTypeFromName = MimeTypeDetectors.mimeTypeFromName(nameAndBefore)
  def mimeTypeFromContent = MimeTypeDetectors.mimeTypeFromContent(toPath)
  def size = toFile.length()

  def decoder = {
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    decoder
  }
  //import org.apache.commons.io.input.BOMInputStream
  //import org.apache.commons.io.IOUtils
  //def toBomInputStream: InputStream = new BOMInputStream(unsafeToInputStream,false)
  //def toSource: BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream, "UTF-8")

  def absolutePlatformDependent: String = toPath("").toAbsolutePath.toString
  def standard(selector: this.type => String): String = FileSystem.standard(selector(this))
  def mkdirIfNecessary: this.type = {
    FileUtils.forceMkdir(toFile)
    this
  }
  def pathInRaw: String = raw.replaceAll("""^([^*]*)[*].*$""", "$1")
  //def list: Seq[FileLocation] = Option(existing.toFile.listFiles).getOrElse(Array[File]()).map(Locations.file(_))

  def traverse: Traversable[(Path, BasicFileAttributes)] = if (raw contains "*")
    Locations.file(pathInRaw).parent.traverse
  else
    new FileVisitor.TraversePath(toPath)
  def traverseFiles: Traversable[Path] = if (exists) traverse.map { case (file, attr) => file } else Traversable()

  def traverseWithDir = new FileVisitor.TraversePath(toPath, true)
  protected def using[A <% AutoCloseable, B](resource: A)(f: A => B): B = {
    import scala.language.reflectiveCalls
    try f(resource) finally resource.close()
  }
  import scala.language.implicitConversions
  implicit def toAutoCloseable(source: scala.io.BufferedSource): AutoCloseable = new AutoCloseable {
    override def close() = source.close()
  }

  def hasDirs = RichPath.wrapPath(toPath).list.find(_.toFile.isDirectory).nonEmpty
  def isFile = toFile.isFile
  def exists = toFile.exists
  def nonExisting(process: (this.type) => Any): this.type = {
    if (!exists) process(this)
    this
  }

  def existing: this.type =
    if (toFile.exists)
      this
    else
      throw new RuntimeException("[" + this + "] doesn't exist!")
  def existingOption: Option[this.type] =
    if (exists)
      Some(this)
    else
      None
  def existing(source: BufferedSource) = {
    //if (source.nonEmpty)
    val hasNext = Try { source.hasNext }
    //println(s"$absolute hasNext=$hasNext")
    val hasNext2 = hasNext.recover {
      case ex: Throwable =>
        throw new RuntimeException("[" + this + "] doesn't exist!")
    }
    //hasNext might be false if is empty
    //    if (!hasNext2.get)
    //      throw new RuntimeException("[" + self + "] doesn't have next!")
    source
  }
  def inspect(message: (this.type) => Any): this.type = {
    message(this)
    this
  }
  def length: Long = toFile.length()

  def nameAndBefore: String = absolute
  def absolute: String = standard(_.absolutePlatformDependent)
  def isAbsolute = toFile.isAbsolute()
  /**Gets only the path part (without drive name on windows for example), and without the name of file*/
  def path: String = FilenameUtils.getPath(absolute)
}
trait AbsoluteBaseLocation extends BaseLocation{
}
trait NavigableInputLocation[Self <: NavigableInputLocation[Self]] extends InputLocation with NavigableBaseLocation[Self] { self: Self =>
  def unzip: ZipInputLocation[NavigableInputLocation[_]] = new ZipInputLocation[NavigableInputLocation[_]](self, None)
}
trait InputLocation extends AbsoluteBaseLocation{
  protected def unsafeToInputStream: InputStream = new FileInputStream(absolute)
  protected def unsafeToReader: java.io.Reader = new java.io.InputStreamReader(unsafeToInputStream, decoder)
  protected def unsafeToSource: scala.io.BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream)(decoder)
  def usingInputStream[T](op: InputStream => T): T = using(unsafeToInputStream)(op)
  def usingReader[T](reader: java.io.Reader => T): T = using(unsafeToReader)(reader)
  def usingSource[T](processor: scala.io.BufferedSource => T): T = using(unsafeToSource)(processor)

  def readLines: Iterable[String] = traverseLines.toIterable
  def traverseLines: Traversable[String] = new Traversable[String] {
    def foreach[U](f: String => U): Unit = {
      usingSource { x => x.getLines().foreach(f) }
    }
  }

  //def child(child: String): InputLocation
  //def parent: InputLocation.Self
  def bytes: Array[Byte] = org.apache.commons.io.FileUtils.readFileToByteArray(toFile)
  def copyToIfNotExists(dest: OutputLocation): this.type = { dest.existingOption.map(_.copyFrom(this)); this }
  def copyTo(dest: OutputLocation):this.type = {
    usingInputStream { source =>
      dest.usingOutputStream { output =>
        IOUtils.copyLarge(source, output)
      }
    }
    this
    //overwrite
    //    FileUtils.copyInputStreamToFile(unsafeToInputStream, dest.toOutputStream)
    //    IOUtils.copyLarge(unsafeToInputStream, dest.toOutputStream)
  }
  def copyTo[T<:NavigableOutputLocation[T]](dest: NavigableOutputLocation[T]):this.type = {
    dest.mkdirOnParentIfNecessary
    copyTo(dest.asInstanceOf[OutputLocation])
  }
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
  def copyAsHardLink(dest: OutputLocation, overwriteIfAlreadyExists: Boolean = false): this.type = {
    dest.copyFromAsHardLink(this, overwriteIfAlreadyExists);
    this
  }
}
trait NavigableOutputLocation[Self <: NavigableOutputLocation[Self]] extends OutputLocation with NavigableBaseLocation[Self] { self: Self =>
  def mkdirOnParentIfNecessary: this.type = {
    parent.mkdirIfNecessary
    self
  }
  def rename(renamer: String => String) = {
    val newName = renamer(baseName)
    if (newName == baseName) {
      //println(s"ignore [${absolute}] to [${absolute}]")
    } else {
      val dest: Self = parent.child(withExtension2(newName, extension))
      //println(s"move [${absolute}] to [${dest.absolute}]")
      FileUtils.moveFile(toFile, dest.toFile)
    }
  }
  def deleteOrRenameIfExists: Self = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
}
trait OutputLocation extends AbsoluteBaseLocation{self=>
  //trait OutputLocation extends BaseLocation {
  //  override type Self=OutputLocation
  protected def unsafeToOutputStream: OutputStream = new FileOutputStream(absolute, append)
  protected def unsafeToWriter: Writer = new BufferedWriter(new OutputStreamWriter(unsafeToOutputStream, "UTF-8"))
  protected def unsafeToPrintWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(unsafeToOutputStream, StandardCharsets.UTF_8), true)

  def usingOutputStream[T](op: OutputStream => T): T = using(unsafeToOutputStream)(op)
  def usingWriter[T](op: Writer => T): T = using(unsafeToWriter)(op)
  def usingPrintWriter[T](op: PrintWriter => T): T = using(unsafeToPrintWriter)(op)

  //  def asInput: InputLocation
  def append: Boolean
  def moveTo(dest: OutputLocation): this.type = {
    FileUtils.moveFile(toFile, dest.toFile)
    this
  }
  def deleteIfExists: this.type = {
    if (exists) {
      logger.info(s"delete existing $absolute")
      ApacheFileUtils.forceDelete(toPath)
    }
    this
  }
  def writeContent(content: String): this.type = { usingPrintWriter(_.print(content)); this }
  def appendContent(content: String) = withAppend.writeContent(content)
  def withAppend: self.type
  def copyFrom(src: InputLocation): this.type = { src.copyTo(this); this }
  def copyFromAsSymlink(src: InputLocation) = Files.createSymbolicLink(toPath, src.toPath)
  def copyFromAsHardLink(src: InputLocation, overwriteIfAlreadyExists: Boolean = false): this.type = {
    if (overwriteIfAlreadyExists) {
      Files.createLink(toPath, src.toPath)
    } else {
      if (exists) {
        throw new RuntimeException("Destination file " + this + " already exists.")
      } else {
        Files.createLink(toPath, src.toPath)
      }
    }
    this
  }
}

object ApacheFileUtils {
  def forceDelete(path: Path) = try {
    FileUtils.forceDelete(path.toFile)
  } catch {
    case e: IOException =>
      val msg = "Unable to delete file: "
      if (e.getMessage.startsWith(msg)) {
        Files.deleteIfExists(Paths.get(e.getMessage.stripPrefix(msg)))
      } else
        throw e
  }
}
trait InOutLocation[Self <: InOutLocation[Self]] extends NavigableInputLocation[Self] with NavigableOutputLocation[Self] { self: Self =>
}
trait RelativeLocationLike[Self <: RelativeLocationLike[Self]] extends NavigableLocation[Self] { self: Self =>
  def relativePath: String
  override def nameAndBefore: String = relativePath
  def raw: String = relativePath
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
}
case class RelativeLocation(relativePath: String) extends RelativeLocationLike[RelativeLocation] {
  FileSystem.requireRelativePath(relativePath)
  override def parent: RelativeLocation = new RelativeLocation(parentName)
  override def child(child: String): RelativeLocation = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    new RelativeLocation(if (relativePath.isEmpty) child else FileSystem.addChild(relativePath, child))
  }
}
trait FileLocationLike[Self <: FileLocationLike[Self]] extends InOutLocation[Self] { self: Self =>
  //  override type ChildLocation = FileLocationLike
  def fileFullPath: String
  def append: Boolean

  //  val a=Map(1 -> "a", 2 -> "b").values

  override def parentName: String = toFile.getParentFile.getAbsolutePath
  def raw = fileFullPath
  def asInput: NavigableInputLocation[Self] = self
  lazy val toFile: File = new File(fileFullPath)
  override def toPath: Path = Paths.get(fileFullPath)
  protected override def unsafeToInputStream: InputStream = new FileInputStream(toFile)
  //should not throw exception but return Try?
  def checkedChild(child: String): String = { require(!child.endsWith(" "), "Child [" + child + "] has trailing spaces"); child }
  //import org.raisercostin.util.MimeTypesUtils2
  def asFile: Self = self
}
case class FileLocation(fileFullPath: String, append: Boolean = false) extends FileLocationLike[FileLocation] {self=>
  //  override type ChildLocation=FileLocation
  override def parent: FileLocation = new FileLocation(parentName)
  override def child(child: String): FileLocation = new FileLocation(toPath.resolve(checkedChild(child)).toFile.getAbsolutePath)
  override def withAppend: self.type = self.copy(append = true).asInstanceOf[self.type]
}
case class MemoryLocation(val memoryName: String) extends RelativeLocationLike[MemoryLocation] with InOutLocation[MemoryLocation] {
  //  type ChildLocation = MemoryLocation
  override def nameAndBefore: String = absolute
  def relativePath: String = memoryName
  override def raw = memoryName
  def asInput: NavigableInputLocation[MemoryLocation] = this
  def append: Boolean = false
  //val buffer: Array[Byte] = Array()
  lazy val outStream = new ByteArrayOutputStream()
  override def toFile: File = ???
  protected override def unsafeToOutputStream: OutputStream = outStream
  protected override def unsafeToInputStream: InputStream = new ByteArrayInputStream(outStream.toByteArray())
  def child(child: String): MemoryLocation = ???
  def parent: MemoryLocation = ???
  override def withAppend: this.type = ???
  override def length: Long = outStream.size()
  override def mkdirOnParentIfNecessary: this.type = this
  override def exists = true
  override def descendants: Iterable[MemoryLocation] = Iterable(this)
  override def size = outStream.size()
}
object ClassPathInputLocationLike {
  private def getDefaultClassLoader(): ClassLoader = {
    Try { Thread.currentThread().getContextClassLoader }.toOption.getOrElse(classOf[System].getClassLoader)
    //    var cl: ClassLoader = null
    //    try {
    //      cl = Thread.currentThread().getContextClassLoader
    //    } catch {
    //      case ex: Throwable =>
    //    }
    //    if (cl == null) {
    //      cl = classOf[System].getClassLoader
    //    }
    //    cl
  }
  private def getSpecialClassLoader(): ClassLoader =
    //Option(Thread.currentThread().getContextClassLoader).orElse
    (Option(classOf[ClassPathInputLocation].getClassLoader)).orElse(Option(classOf[ClassLoader].getClassLoader)).get
}
/**
 * @see http://www.thinkplexx.com/learn/howto/java/system/java-resource-loading-explained-absolute-and-relative-names-difference-between-classloader-and-class-resource-loading
 */
trait ClassPathInputLocationLike[Self <: ClassPathInputLocationLike[Self]] extends NavigableInputLocation[Self] { self: Self =>
  def initialResourcePath: String
  def raw = initialResourcePath
  import ClassPathInputLocationLike._
  val resourcePath = initialResourcePath.stripPrefix("/")
  val resource = {
    val res = getSpecialClassLoader.getResource(resourcePath);
    require(res != null, s"Couldn't get a stream from $self");
    res
  }
  override def toUrl: java.net.URL = resource
  override def exists = resource != null
  override def absolute: String = toUrl.toURI().getPath()
  //Try{toFile.getAbsolutePath()}.recover{case e:Throwable => Option(toUrl).map(_.toExternalForm).getOrElse("unfound classpath://" + resourcePath) }.get
  def toFile: File = Try { new File(toUrl.toURI()) }.recoverWith { case e: Throwable => Failure(new RuntimeException("Couldn't get file from " + self, e)) }.get
  protected override def unsafeToInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  ///def toWrite = Locations.file(toFile.getAbsolutePath)
  override def unzip: ZipInputLocation[NavigableInputLocation[_]] = new ZipInputLocation[NavigableInputLocation[_]](self, None)
  override def parentName = {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  def asFile: FileLocation = Locations.file(toFile)
}
case class ClassPathInputLocation(initialResourcePath: String) extends ClassPathInputLocationLike[ClassPathInputLocation] {
  require(initialResourcePath != null)
  def child(child: String): ClassPathInputLocation = new ClassPathInputLocation(FileSystem.addChild(resourcePath, child))
  def parent: ClassPathInputLocation = new ClassPathInputLocation(parentName)
}
case class ZipInputLocation[T <: NavigableInputLocation[_]](zip: T, entry: Option[java.util.zip.ZipEntry]) extends ZipInputLocationLike[T, ZipInputLocation[T]] {
  def parent: ZipInputLocation[T] = ZipInputLocation(zip, Some(rootzip.getEntry(parentName)))
  def child(child: String): ZipInputLocation[T] = entry match {
    case None =>
      ZipInputLocation(zip, Some(rootzip.getEntry(child)))
    case Some(entry) =>
      ZipInputLocation(zip, Some(rootzip.getEntry(entry.getName() + "/" + child)))
  }
  override def list: Iterable[ZipInputLocation[T]] = Option(existing).map(_ => entries).getOrElse(Iterable()).map(entry => ZipInputLocation(zip, Some(entry)))
}
//TODO fix name&path&unique identifier stuff
trait ZipInputLocationLike[T <: NavigableInputLocation[_], Self <: ZipInputLocationLike[T, Self]] extends NavigableInputLocation[ZipInputLocationLike[T, Self]] { self: Self =>
  //  type ChildLocation = ZipInputLocation
  def zip: T
  def entry: Option[java.util.zip.ZipEntry]
  def raw = "ZipInputLocation[" + zip + "," + entry + "]"

  def toFile: File = zip.toFile
  protected override def unsafeToInputStream: InputStream = entry match {
    case None =>
      throw new RuntimeException("Can't read stream from zip folder " + self)
    case Some(entry) =>
      rootzip.getInputStream(entry)
  }

  protected lazy val rootzip = new java.util.zip.ZipFile(Try { toFile }.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
  //private lazy val rootzip = new java.util.zip.ZipInputStream(zip.unsafeToInputStream)
  import collection.JavaConverters._
  import java.util.zip._
  protected lazy val entries: Iterable[ZipEntry] = new Iterable[ZipEntry] {
    def iterator = rootzip.entries.asScala
  }
  override def name = entry.map(_.getName).getOrElse(zip.name + "-unzipped")
  override def unzip: ZipInputLocation[NavigableInputLocation[_]] = usingInputStream { input =>
    new ZipInputLocation[NavigableInputLocation[_]](Locations.temp.randomChild(name).copyFrom(Locations.stream(input)), None)
  }
}

case class StreamLocation(val inputStream: InputStream) extends NavigableInputLocation[StreamLocation] {
  def raw = "inputStream[" + inputStream + "]"
  def child(child: String): StreamLocation = ???
  def parent: StreamLocation = ???
  def toFile: File = ???
  protected override def unsafeToInputStream: InputStream = inputStream
}
case class UrlLocation(url: java.net.URL) extends NavigableInputLocation[UrlLocation] {
  def raw = url.toExternalForm()
  def child(child: String): UrlLocation = ???
  def parent: UrlLocation = ???
  def toFile: File = ???
  import java.net._
  override def length: Long = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("HEAD")
      conn.getInputStream
      val len = conn.getContentLengthLong()
      if (len < 0) throw new RuntimeException("Invalid length received!")
      len
    } catch {
      case e: java.io.IOException => -1
    } finally {
      conn.disconnect()
    }
  }
  protected override def unsafeToInputStream: InputStream = url.openStream()
}
case class TempLocation(temp: File, append: Boolean = false) extends FileLocationLike[TempLocation] {self=>
  override def withAppend: self.type = this.copy(append = true).asInstanceOf[self.type]
  def fileFullPath: String = temp.getAbsolutePath()
  def randomChild(prefix: String = "random", suffix: String = "") = new TempLocation(File.createTempFile(prefix, suffix, toFile))
  override def parent: TempLocation = new TempLocation(new File(parentName))
  override def child(child: String): TempLocation = new TempLocation(toPath.resolve(checkedChild(child)).toFile)
}
/**
 * file(*) - will refer to the absolute path passed as parameter or to a file relative to current directory new File(".") which should be the same as System.getProperty("user.dir") .
 * TODO: file separator agnosticisim: use an internal standard convention indifferent of the "outside" OS convention: Win,Linux,OsX
 * The output will be the same irrespective of the machine that the code is running on. @see org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator
 */
object Locations {
  val logger = org.slf4j.LoggerFactory.getLogger("locations")
  def classpath(resourcePath: String): ClassPathInputLocation =
    new ClassPathInputLocation(resourcePath)
  def file(path: Path): FileLocation =
    file(path.toFile)
  def file(fileFullPath: String): FileLocation =
    createAbsoluteFile(fileFullPath)
  def file(file: File): FileLocation =
    createAbsoluteFile(file.getAbsolutePath())
  def file(file: File, subFile: String): FileLocation =
    createAbsoluteFile(file.getAbsolutePath()).child(subFile)
  def file(fileFullPath: String, optionalParent: NavigableBaseLocation[_]): FileLocation =
    file(if (isAbsolute(fileFullPath)) fileFullPath else optionalParent.absolute + fileFullPath)

  private def isAbsolute(path: String) = new File(path).isAbsolute()
  private def createAbsoluteFile(path: String) = {
    require(path != null, "Path should not be null")
    if (isAbsolute(path))
      new FileLocation(path)
    else
      new FileLocation(new File(path).getAbsolutePath())
  }
  def memory(memoryName: String): MemoryLocation =
    new MemoryLocation(memoryName)
  def stream(stream: InputStream): StreamLocation = new StreamLocation(stream)
  def url(url: java.net.URL): UrlLocation = new UrlLocation(url)
  def temp: TempLocation = TempLocation(tmpdir)
  private val tmpdir = new File(System.getProperty("java.io.tmpdir"))
  def relative(path: String = "")(implicit fsf: FileSystemFormatter): RelativeLocation = RelativeLocation(fsf.standard(path))
  def current(relative: String): FileLocation = file(new File(new File("."), relative).getCanonicalPath())

  implicit val unixAndWindowsToStandard = FileSystem.unixAndWindowsToStandard
}