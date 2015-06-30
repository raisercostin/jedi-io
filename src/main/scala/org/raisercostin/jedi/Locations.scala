package org.raisercostin.jedi

import java.nio.file.Paths
import java.io.File
import java.nio.file.Path
import java.io.FileOutputStream
import java.io.OutputStream
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
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.security.AccessController
import Locations._
import java.io.IOException
import scala.annotation.tailrec
import java.io.Closeable
import org.apache.commons.io._

/**
 * Should take into consideration several composable/ortogonal aspects:
 * - end of line: win,linux,osx - internal standard: \n
 * - file separator: win,linux,osx - internal standard: / (like linux, fewer colisions with string escaping in java)
 * - file name case sensitivity - internal standard: case sensible (in windows there will not be any problem)
 * - win 8.3 file names vs. full file names - internal standard: utf-8
 *
 * In principle should be agnostic to these aspects and only at runtime will depend on the local environment.
 */
trait BaseLocation {
  def raw: String
  def nameAndBefore: String
  def name: String = FilenameUtils.getName(nameAndBefore)
  def extension: String = FilenameUtils.getExtension(nameAndBefore)
  def baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  def mimeType = mimeTypeFromName
  def mimeTypeFromName = MimeTypeDetectors.mimeTypeFromName(nameAndBefore)

  def decoder = {
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    decoder
  }
  def standard(selector: this.type => String): String = FileSystem.standard(selector(this))
  def pathInRaw: String = raw.replaceAll("""^([^*]*)[*].*$""", "$1")
  //def list: Seq[FileLocation] = Option(existing.toFile.listFiles).getOrElse(Array[File]()).map(Locations.file(_))

  def inspect(message: (this.type) => Any): this.type = {
    message(this)
    this
  }
}
trait AbsoluteBaseLocation extends BaseLocation{
  def toUrl: java.net.URL = toFile.toURI.toURL
  //import org.apache.commons.io.input.BOMInputStream
  //import org.apache.commons.io.IOUtils
  //def toBomInputStream: InputStream = new BOMInputStream(unsafeToInputStream,false)
  //def toSource: BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream, "UTF-8")

  override def mimeType = mimeTypeFromName.orElse(mimeTypeFromContent)
  /**To read data you should read the inputstream*/
  def mimeTypeFromContent = MimeTypeDetectors.mimeTypeFromContent(toPath)

  def toFile: File
  def toPath: Path = toFile.toPath
  def toPath(subFile: String): Path = toPath.resolve(subFile)
  def size = toFile.length()
  def absolutePlatformDependent: String = toPath("").toAbsolutePath.toString
  def mkdirIfNecessary: this.type = {
    FileUtils.forceMkdir(toFile)
    this
  }
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
  private def listPath(glob: String): List[Path] = {
    val stream = Files.newDirectoryStream(toPath, glob)
    import scala.collection.JavaConversions.iterableAsScalaIterable
    try stream.toList finally stream.close
  }

  def hasDirs = listPath("*").find(_.toFile.isDirectory).nonEmpty
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
    val hasNext2 = hasNext.recover {
      case ex: Throwable =>
        throw new RuntimeException("[" + this + "] doesn't exist!")
    }
    //hasNext might be false if is empty
    //    if (!hasNext2.get)
    //      throw new RuntimeException("[" + self + "] doesn't have next!")
    source
  }
  def nameAndBefore: String = absolute
  def length: Long = toFile.length()
  def absolute: String = standard(_.absolutePlatformDependent)
  def isAbsolute = toFile.isAbsolute()
  /**Gets only the path part (without drive name on windows for example), and without the name of file*/
  def path: String = FilenameUtils.getPath(absolute)
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

  def bytes: Array[Byte] = org.apache.commons.io.FileUtils.readFileToByteArray(toFile)
  def copyToIfNotExists(dest: OutputLocation): this.type = { dest.existingOption.map(_.copyFrom(this)); this }
  def copyTo(dest: OutputLocation):this.type = copyToOutputLocation(dest)
  def copyTo(dest: NavigableOutputLocation):this.type = {
    dest.mkdirOnParentIfNecessary
    copyToOutputLocation(dest)
  }
  private def copyToOutputLocation(dest: OutputLocation):this.type = {
    usingInputStream { source =>
      dest.usingOutputStream { output =>
        IOUtils.copyLarge(source, output)
      }
    }
    this
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
  def unzip: ZipInputLocation = new ZipInputLocation(this, None)
}
trait OutputLocation extends AbsoluteBaseLocation{self=>
  type Repr = self.type
  //trait OutputLocation extends BaseLocation {
  //  override type Self=OutputLocation
  protected def unsafeToOutputStream: OutputStream = new FileOutputStream(absolute, append)
  protected def unsafeToWriter: Writer = new BufferedWriter(new OutputStreamWriter(unsafeToOutputStream, "UTF-8"))
  protected def unsafeToPrintWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(unsafeToOutputStream, StandardCharsets.UTF_8), true)

  def usingOutputStream[T](op: OutputStream => T): T = using(unsafeToOutputStream)(op)
  def usingWriter[T](op: Writer => T): T = using(unsafeToWriter)(op)
  def usingPrintWriter[T](op: PrintWriter => T): T = using(unsafeToPrintWriter)(op)

  def append: Boolean
  def moveTo(dest: OutputLocation): this.type = {
    FileUtils.moveFile(toFile, dest.toFile)
    this
  }
  def deleteIfExists: Repr = {
    if (exists) {
      logger.info(s"delete existing $absolute")
      impl.ApacheFileUtils.forceDelete(toPath)
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

trait NavigableLocation extends AbsoluteBaseLocation { self =>
  type Repr = self.type
  protected def repr: Repr = toRepr(self)
  implicit protected def toRepr[T<:NavigableLocation](location:T):Repr = location.asInstanceOf[Repr]

  def parent: Repr
  def child(child: String): Repr
  def child(childText: Option[String]): Repr = childText match {
    case None => repr
    case Some(s) if s.trim.isEmpty => repr
    case Some(s) => child(s)
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
  def parentName: String = //toFile.getParentFile.getAbsolutePath
    Option(FilenameUtils.getFullPathNoEndSeparator(nameAndBefore)).getOrElse("")
  def extractPrefix(ancestor: NavigableLocation): Try[RelativeLocation] =
    extractAncestor(ancestor).map(x => relative(FileSystem.constructPath(x))(FileSystem.identityFormatter))
  def extractAncestor(ancestor: NavigableLocation): Try[Seq[String]] =
    diff(nameAndBefore, ancestor.nameAndBefore).map { FileSystem.splitPartialPath }
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
  def list: Iterable[Repr] = Option(existing).map { x =>
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(buildNew)
  def descendants: Iterable[Repr] = {
    val all: Iterable[File] = Option(existing).map { x =>
      traverse.map(_._1.toFile).toIterable
    }.getOrElse(Iterable[File]())
    all.map(buildNew)
  }

  def buildNew(x: File): Repr = Locations.file(x)
  def renamedIfExists: Repr = {
    @tailrec
    def findUniqueName(destFile: Repr, counter: Int): Repr =
      if (destFile.exists)
        findUniqueName(destFile.withBaseName { baseName: String => (baseName + "-" + counter) }, counter + 1)
      else
        destFile
    findUniqueName(repr, 1)
  }
}
trait NavigableInputLocation extends InputLocation with NavigableLocation

trait NavigableOutputLocation extends OutputLocation with NavigableLocation { self =>
  override type Repr = self.type

  def mkdirOnParentIfNecessary: this.type = {
    parent.mkdirIfNecessary
    this
  }
  private def rename(renamer: String => String) = {
    val newName = renamer(baseName)
    if (newName == baseName) {
      //p rintln(s"ignore [${absolute}] to [${absolute}]")
    } else {
      val dest = parent.child(withExtension2(newName, extension))
      //p rintln(s"move [${absolute}] to [${dest.absolute}]")
      FileUtils.moveFile(toFile, dest.toFile)
    }
  }
  def deleteOrRenameIfExists: Repr = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
  def asInput: NavigableInputLocation
}
trait NavigableInOutLocation extends NavigableInputLocation with NavigableOutputLocation

trait RelativeLocationLike extends NavigableLocation { self =>
  override type Repr = self.type
  def relativePath: String
  override def nameAndBefore: String = relativePath
  def raw: String = relativePath
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
}
case class RelativeLocation(relativePath: String) extends RelativeLocationLike {self=>
  override type Repr = self.type
  FileSystem.requireRelativePath(relativePath)
  //TODO to remove
  def toFile = ???
  override def parent: Repr = new RelativeLocation(parentName)
  override def child(child: String): Repr = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    new RelativeLocation(if (relativePath.isEmpty) child else FileSystem.addChild(relativePath, child))
  }
}
trait FileLocationLike extends NavigableInOutLocation { self =>
  override type Repr = self.type
  def fileFullPath: String
  def append: Boolean
  override def parentName: String = toFile.getParentFile.getAbsolutePath
  def raw = fileFullPath
  def asInput: NavigableInputLocation = self
  lazy val toFile: File = new File(fileFullPath)
  override def toPath: Path = Paths.get(fileFullPath)
  protected override def unsafeToInputStream: InputStream = new FileInputStream(toFile)
  //should not throw exception but return Try?
  def checkedChild(child: String): String = { require(!child.endsWith(" "), "Child [" + child + "] has trailing spaces"); child }
  //import org.raisercostin.util.MimeTypesUtils2
  def asFile: Repr = self
}
case class FileLocation(fileFullPath: String, append: Boolean = false) extends FileLocationLike {self=>
  override type Repr = self.type
  override def parent: Repr = new FileLocation(parentName)
  override def child(child: String): Repr = new FileLocation(toPath.resolve(checkedChild(child)).toFile.getAbsolutePath)
  override def withAppend: Repr = self.copy(append = true)
}
case class MemoryLocation(val memoryName: String) extends RelativeLocationLike with NavigableInOutLocation {self=>
  override type Repr = self.type
  override def nameAndBefore: String = absolute
  def relativePath: String = memoryName
  override def raw = memoryName
  def asInput: MemoryLocation = this
  def append: Boolean = false
  //val buffer: Array[Byte] = Array()
  lazy val outStream = new ByteArrayOutputStream()
  override def toFile: File = ???
  protected override def unsafeToOutputStream: OutputStream = outStream
  protected override def unsafeToInputStream: InputStream = new ByteArrayInputStream(outStream.toByteArray())
  override def child(child: String): Repr = ???
  override def parent: Repr = ???
  override def withAppend: this.type = ???
  override def length: Long = outStream.size()
  override def mkdirOnParentIfNecessary: this.type = this
  override def exists = true
  override def descendants: Iterable[Repr] = Iterable(this)
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
trait ClassPathInputLocationLike extends NavigableInputLocation { self =>
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
  override def exists = true//resource != null from constructor
  override def absolute: String = toUrl.toURI().getPath()
  //Try{toFile.getAbsolutePath()}.recover{case e:Throwable => Option(toUrl).map(_.toExternalForm).getOrElse("unfound classpath://" + resourcePath) }.get
  def toFile: File = Try { new File(toUrl.toURI()) }.recoverWith { case e: Throwable => Failure(new RuntimeException("Couldn't get file from " + self, e)) }.get
  protected override def unsafeToInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  ///def toWrite = Locations.file(toFile.getAbsolutePath)
  override def parentName = {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  def asFile: FileLocation = Locations.file(toFile)
}
case class ClassPathInputLocation(initialResourcePath: String) extends ClassPathInputLocationLike {self=>
  override type Repr = self.type
  require(initialResourcePath != null)
  def child(child: String): Repr = new ClassPathInputLocation(FileSystem.addChild(resourcePath, child))
  def parent: Repr = new ClassPathInputLocation(parentName)
}
case class ZipInputLocation(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) extends ZipInputLocationLike {self=>
  override type Repr = self.type
  def parent: Repr = ZipInputLocation(zip, Some(rootzip.getEntry(parentName)))
  def child(child: String): Repr = (entry match {
    case None =>
      ZipInputLocation(zip, Some(rootzip.getEntry(child)))
    case Some(entry) =>
      ZipInputLocation(zip, Some(rootzip.getEntry(entry.getName() + "/" + child)))
  })
  override def list: Iterable[Repr] = Option(existing).map(_ => entries).getOrElse(Iterable()).map(entry => toRepr(ZipInputLocation(zip, Some(entry))))
}
//TODO fix name&path&unique identifier stuff
trait ZipInputLocationLike extends NavigableInputLocation { self =>
  override type Repr = self.type
  def zip: InputLocation
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
  override def unzip: ZipInputLocation = usingInputStream { input =>
    new ZipInputLocation(Locations.temp.randomChild(name).copyFrom(Locations.stream(input)), None)
  }
}

case class StreamLocation(val inputStream: InputStream) extends InputLocation {
  def raw = "inputStream[" + inputStream + "]"
  def toFile: File = ???
  protected override def unsafeToInputStream: InputStream = inputStream
}
case class UrlLocation(url: java.net.URL) extends InputLocation {self=>
  def raw = url.toExternalForm()
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
case class TempLocation(temp: File, append: Boolean = false) extends FileLocationLike {self=>
  override type Repr = self.type
  override def withAppend: self.type = this.copy(append = true)
  def fileFullPath: String = temp.getAbsolutePath()
  def randomChild(prefix: String = "random", suffix: String = "") = new TempLocation(File.createTempFile(prefix, suffix, toFile))
  override def parent: Repr = new TempLocation(new File(parentName))
  override def child(child: String): Repr = new TempLocation(toPath.resolve(checkedChild(child)).toFile)
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
  def file(fileFullPath: String, optionalParent: NavigableLocation): FileLocation =
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