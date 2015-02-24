package org.raisercostin.util.io

import java.nio.file.Paths
import java.io.File
import java.nio.file.Path
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
import java.io.OutputStream
import org.apache.commons.io.FilenameUtils
import java.io.InputStream
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
import java.io.FileSystem

/**
 * Should take into consideration several composable/ortogonal aspects:
 * - end of line: win,linux,osx - internal standard: \n
 * - file separator: win,linux,osx - internal standard: / (like linux, fewer colisions with string escaping in java)
 * - file name case sensitivity - internal standard: case sensible (in windows there will not be any problem)
 * - win 8.3 file names vs. full file names - internal standard: utf-8
 *
 * In principle should be agnostic to these aspects and only at runtime will depend on the local environment.
 */
package object io {

}

import Locations._
trait NavigableLocation { self =>
  def nameAndBefore: String
  def parent: this.type
  def child(child: String): this.type

  def child(childText: Option[String]): this.type = childText match {
    case None => this
    case Some(s) if s.trim.isEmpty => this
    case Some(s) => child(s)
  }
  def child(childLocation: RelativeLocation): this.type = {
    if (childLocation.isEmpty) {
      this
    } else {
      child(childLocation.relativePath)
    }
  }
  def withParent(process: (this.type) => Any): this.type = {
    process(parent)
    this
  }
  def withSelf(process: (this.type) => Any): this.type = {
    process(this)
    this
  }
  def descendant(childs: Seq[String]): this.type = if (childs.isEmpty) this else child(childs.head).descendant(childs.tail)
  def extension: String = FilenameUtils.getExtension(nameAndBefore)
  def name: String = FilenameUtils.getName(nameAndBefore)
  def baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  def parentName: String = //toFile.getParentFile.getAbsolutePath
    Option(FilenameUtils.getFullPathNoEndSeparator(nameAndBefore)).getOrElse("")
  def extractPrefix(ancestor: NavigableLocation): RelativeLocation = relative(extractAncestor(ancestor).get.foldLeft("")((x, y) => (if (x.isEmpty) "" else (x + SEP)) + y))
  def extractAncestor(ancestor: NavigableLocation): Try[Seq[String]] = diff(nameAndBefore, ancestor.nameAndBefore).map { _.split(Pattern.quote(SEP)).filterNot(_.trim.isEmpty) }
  def diff(text: String, prefix: String) = if (text.startsWith(prefix)) Success(text.substring(prefix.length)) else Failure(new RuntimeException(s"Text [$text] doesn't start with [$prefix]."))
  def withBaseName(baseNameSupplier: String => String): this.type = parent.child(withExtension2(baseNameSupplier(baseName), extension))
  def withBaseName2(baseNameSupplier: String => Option[String]): this.type = baseNameSupplier(baseName).map { x => parent.child(withExtension2(x, extension)) }.getOrElse(self).asInstanceOf[this.type]
  def withName(nameSupplier: String => String): this.type = parent.child(nameSupplier(name))
  def withExtension(extensionSupplier: String => String): this.type = parent.child(withExtension2(baseName, extensionSupplier(extension)))
  protected def withExtension2(name: String, ext: String) =
    if (ext.length > 0)
      name + "." + ext
    else name
  def standard(selector: this.type => String): String = Locations.standard(selector(this))
}
trait AbsoluteLocation {
  def path: String
}
trait AbsoluteBaseLocation extends BaseLocation with AbsoluteLocation {
  /**Gets only the path part (without drive name on windows for example), and without the name of file*/
  def path: String = FilenameUtils.getPath(absolute)
}
trait BaseLocation extends NavigableLocation {
  def raw: String
  def nameAndBefore: String = absolute
  /**To read data you should read the inputstream*/
  def toUrl: java.net.URL = toFile.toURI.toURL
  def toFile: File
  def toPath: Path = toFile.toPath
  def toInputStream: InputStream
  def toPath(subFile: String): Path = toPath.resolve(subFile)

  def toSource: scala.io.BufferedSource = {
    //import org.apache.commons.io.input.BOMInputStream
    //import org.apache.commons.io.IOUtils
    //def toBomInputStream: InputStream = new BOMInputStream(toInputStream,false)
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    scala.io.Source.fromInputStream(toInputStream)(decoder)
    //def toSource: BufferedSource = scala.io.Source.fromInputStream(toInputStream, "UTF-8")
  }
  def absolute: String = toPath("").toAbsolutePath.toString
  def absoluteStandard: String = standard(_.absolute)
  def isAbsolute = toFile.isAbsolute()
  def mkdirIfNecessary: this.type = {
    FileUtils.forceMkdir(toFile)
    this
  }
  def parent: this.type
  def mkdirOnParentIfNecessary: this.type = {
    parent.mkdirIfNecessary
    this
  }
  def pathInRaw: String = raw.replaceAll("""^([^*]*)[*].*$""", "$1")
  def list: Iterator[InputLocation] = Option(existing).map(_.toFile.listFiles.toIterator).getOrElse(Iterator()).map(Locations.file(_))
  def traverse: Traversable[(Path, BasicFileAttributes)] = if (raw contains "*")
    Locations.file(pathInRaw).parent.traverse
  else
    new FileVisitor.TraversePath(toPath)
  def traverseFiles = if (exists) traverse.map { case (file, attr) => file } else Traversable()

  def traverseWithDir = new FileVisitor.TraversePath(toPath, true)
  protected def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B =
    try f(resource) finally resource.close()

  def hasDirs = RichPath.wrapPath(toPath).list.find(_.toFile.isDirectory).nonEmpty
  def isFile = toFile.isFile
  def exists = toFile.exists
  def renamedIfExists: this.type = {
      def findUniqueName[T <: BaseLocation](destFile: T): T = {
        var newDestFile = destFile
        var counter = 1
        while (newDestFile.exists) {
          newDestFile = destFile.withBaseName { baseName: String => (baseName + "-" + counter) }
          counter += 1
        }
        newDestFile
      }
    findUniqueName(this)
  }
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
    //      throw new RuntimeException("[" + this + "] doesn't have next!")
    source
  }
  def inspect(message: (this.type) => Any): this.type = {
    message(this)
    this
  }
  def length: Long = toFile.length()
}
trait InputLocation extends AbsoluteBaseLocation {
  def toInputStream: InputStream = new FileInputStream(absolute)
  //def child(child: String): InputLocation
  //def parent: InputLocation.this.type
  def bytes: Array[Byte] = org.apache.commons.io.FileUtils.readFileToByteArray(toFile)
  def usingInputStream(op: InputStream => Unit): Unit =
    using(toInputStream)(inputStream => op(inputStream))
  def readLines =
    existing(toSource).getLines
  def copyToIfNotExists(dest: OutputLocation) = { dest.existingOption.map(_.copyFrom(this)); this }
  def copyTo(dest: OutputLocation) = {
    dest.mkdirOnParentIfNecessary
    val source = toInputStream
    try {
      val output = dest.toOutputStream
      try {
        IOUtils.copyLarge(source, output)
        output.close()
      } finally {
        IOUtils.closeQuietly(output)
      }
    } finally {
      IOUtils.closeQuietly(source)
    }
    //overwrite
    //    FileUtils.copyInputStreamToFile(toInputStream, dest.toOutputStream)
    //    IOUtils.copyLarge(toInputStream, dest.toOutputStream)
  }
  def readContent = {
    // Read a file into a string
    //    import rapture._
    //    import core._, io._, net._, uri._, json._, codec._
    //    import encodings.`UTF-8`
    //    val src = uri"http://rapture.io/sample.json".slurp[Char]
    //existing(toSource).getLines mkString ("\n")
    try { IOUtils.toString(toInputStream) } catch { case x: Throwable => throw new RuntimeException("While reading " + this, x) }
  }
  def readContentAsText: Try[String] =
    Try(readContent)
  //Try(existing(toSource).getLines mkString ("\n"))
  //def unzip: ZipInputLocation = ???
  def unzip: ZipInputLocation = new ZipInputLocation(this, None)
  def copyAsHardLink(dest: OutputLocation, overwriteIfAlreadyExists: Boolean = false): this.type = { dest.copyFromAsHardLink(this, overwriteIfAlreadyExists); this }
}
trait OutputLocation extends BaseLocation {
  def asInput: InputLocation
  def append: Boolean
  def toOutputStream: OutputStream = new FileOutputStream(absolute, append)
  def toWriter: Writer = new BufferedWriter(new OutputStreamWriter(toOutputStream, "UTF-8"))
  def toPrintWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(toOutputStream, StandardCharsets.UTF_8), true)
  def rename(renamer: String => String) = {
    val newName = renamer(baseName)
    if (newName == baseName) {
      //println(s"ignore [${absolute}] to [${absolute}]")
    } else {
      val dest = parent.child(withExtension2(newName, extension))
      //println(s"move [${absolute}] to [${dest.absolute}]")
      FileUtils.moveFile(toFile, dest.toFile)
    }
  }
  def moveTo(dest: OutputLocation): this.type = {
    FileUtils.moveFile(toFile, dest.toFile)
    this
  }
  def deleteOrRenameIfExists: OutputLocation = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
  def deleteIfExists: this.type = {
    if (exists) {
      logger.info(s"delete existing $absolute")
      FileUtils.forceDelete(toFile)
    }
    this
  }
  def usingOutputStream(op: OutputStream => Unit): Unit =
    using(toOutputStream)(outputStream => op(outputStream))
  def usingPrintWriter(op: PrintWriter => Unit): this.type =
    using(toPrintWriter)(printWriter => { op(printWriter); this })
  def writeContent(content: String): this.type = usingPrintWriter(_.print(content))
  def appendContent(content: String) = withAppend.writeContent(content)
  def withAppend: this.type
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

trait InOutLocation extends InputLocation with OutputLocation {
}
trait RelativeLocationLike extends BaseLocation {
  def relativePath: String
  require(!relativePath.startsWith(SEP), s"The relative path $relativePath shouldn't start with file separator [$SEP].")
  override def toFile: File = ???
  override def toPath: Path = ???
  override def toInputStream: InputStream = ???
  override def absolute: String = ???
  override def nameAndBefore: String = relativePath
  def raw: String = relativePath
  def parent: this.type = new RelativeLocation(parentName).asInstanceOf[this.type]
  def child(child: String): this.type = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    new RelativeLocation(if (relativePath.isEmpty) child else relativePath + SEP + child).asInstanceOf[this.type]
  }
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
}
case class RelativeLocation(relativePath: String) extends RelativeLocationLike
case class FileLocation(fileFullPath: String, append: Boolean = false) extends FileLocationLike {
  def withAppend: this.type = this.copy(append = true).asInstanceOf[this.type]
}
trait FileLocationLike extends InOutLocation {
  def fileFullPath: String
  def append: Boolean

  override def parentName: String = toFile.getParentFile.getAbsolutePath
  def raw = fileFullPath
  def asInput: InputLocation = this
  lazy val toFile: File = new File(fileFullPath)
  override def toPath: Path = Paths.get(fileFullPath)
  override def toInputStream: InputStream = new FileInputStream(toFile)
  def child(child: String): this.type = new FileLocation(toPath.resolve(checkedChild(child)).toFile.getAbsolutePath).asInstanceOf[this.type]
  //should not throw exception but return Try?
  def checkedChild(child: String): String = { require(!child.endsWith(" "), "Child [" + child + "] has trailing spaces"); child }
  def parent: this.type = new FileLocation(parentName).asInstanceOf[this.type]
  def size = toFile.length()
  //import org.raisercostin.util.MimeTypesUtils2
  //def mimeType = MimeTypesUtils2.getMimeType(toPath)
  def asFile: FileLocationLike = this
}
case class MemoryLocation(val memoryName: String) extends RelativeLocationLike with InputLocation with OutputLocation {
  def relativePath: String = memoryName
  override def raw = memoryName
  def asInput: InputLocation = this
  def append: Boolean = false
  //val buffer: Array[Byte] = Array()
  lazy val outStream = new ByteArrayOutputStream()
  override def toFile: File = ???
  override def toOutputStream: OutputStream = outStream
  override def toInputStream: InputStream = new ByteArrayInputStream(outStream.toByteArray())
  //  def child(child: String): this.type = ???
  //  def parent: this.type = ???
  def withAppend: this.type = ???
  override def length: Long = outStream.size()
  override def mkdirOnParentIfNecessary: this.type = this
}
object ClassPathInputLocation {
  private def getDefaultClassLoader(): ClassLoader = {
    var cl: ClassLoader = null
    try {
      cl = Thread.currentThread().getContextClassLoader
    } catch {
      case ex: Throwable =>
    }
    if (cl == null) {
      cl = classOf[System].getClassLoader
    }
    cl
  }
  private def getSpecialClassLoader(): ClassLoader =
    //Option(Thread.currentThread().getContextClassLoader).orElse
    (Option(classOf[ClassPathInputLocation].getClassLoader)).orElse(Option(classOf[ClassLoader].getClassLoader)).get
}
/**
 * @see http://www.thinkplexx.com/learn/howto/java/system/java-resource-loading-explained-absolute-and-relative-names-difference-between-classloader-and-class-resource-loading
 */
case class ClassPathInputLocation(initialResourcePath: String) extends InputLocation {
  require(initialResourcePath != null)
  def raw = initialResourcePath
  import ClassPathInputLocation._
  val resourcePath = initialResourcePath.stripPrefix("/")
  val resource = {
    val res = getSpecialClassLoader.getResource(resourcePath);
    require(res != null, s"Couldn't get a stream from $this");
    res
  }
  override def toUrl: java.net.URL = resource
  override def exists = resource != null
  override def absolute: String = toUrl.toURI().getPath() //Try{toFile.getAbsolutePath()}.recover{case e:Throwable => Option(toUrl).map(_.toExternalForm).getOrElse("unfound classpath://" + resourcePath) }.get
  def toFile: File = Try { new File(toUrl.toURI()) }.recoverWith { case e: Throwable => Failure(new RuntimeException("Couldn't get file from " + this, e)) }.get
  override def toInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  def child(child: String): this.type = new ClassPathInputLocation(resourcePath + SEP + child).asInstanceOf[this.type]
  def parent: this.type = new ClassPathInputLocation(parentName).asInstanceOf[this.type]
  ///def toWrite = Locations.file(toFile.getAbsolutePath)
  override def unzip: ZipInputLocation = new ZipInputLocation(this, None)
  override def parentName = {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  def asFile: FileLocation = Locations.file(toFile)
}

case class ZipInputLocation(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) extends InputLocation {
  def raw = "ZipInputLocation[" + zip + "," + entry + "]"
  def parent: this.type = ???
  def child(child: String): this.type = entry match {
    case None =>
      ZipInputLocation(zip, Some(rootzip.getEntry(child))).asInstanceOf[this.type]
    case Some(entry) =>
      ZipInputLocation(zip, Some(rootzip.getEntry(entry.getName() + "/" + child))).asInstanceOf[this.type]
  }

  def toFile: File = zip.toFile
  override def toInputStream: InputStream = entry match {
    case None =>
      throw new RuntimeException("Can't read stream from zip folder " + this)
    case Some(entry) =>
      rootzip.getInputStream(entry)
  }
  override def list: Iterator[InputLocation] = Option(existing).map(_ => entries).getOrElse(Iterator()).map(entry => ZipInputLocation(zip, Some(entry)))

  private lazy val rootzip = new java.util.zip.ZipFile(Try { toFile }.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
  //private lazy val rootzip = new java.util.zip.ZipInputStream(zip.toInputStream)
  import collection.JavaConverters._
  private lazy val entries = rootzip.entries.asScala
  override def name = entry.map(_.getName).getOrElse(zip.name + "-unzipped")
  override def unzip: ZipInputLocation = new ZipInputLocation(Locations.temp.randomChild(name).copyFrom(Locations.stream(toInputStream)), None)
}

case class StreamLocation(val inputStream: InputStream) extends InputLocation {
  def raw = "inputStream[" + inputStream + "]"
  def child(child: String): this.type = ???
  def parent: this.type = ???
  def toFile: File = ???
  override def toInputStream: InputStream = inputStream
}
case class UrlLocation(url: java.net.URL) extends InputLocation {
  def raw = url.toExternalForm()
  def child(child: String): this.type = ???
  def parent: this.type = ???
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
  override def toInputStream: InputStream = url.openStream()
}
case class TempLocation(temp: File, append: Boolean = false) extends FileLocationLike {
  def withAppend: this.type = this.copy(append = true).asInstanceOf[this.type]
  def fileFullPath: String = temp.getAbsolutePath()
  def randomChild(prefix: String, suffix: String = "") = new TempLocation(File.createTempFile(prefix, suffix, toFile))
}
/**
 * file(*) - will reffer to the absolute path passed as parameter or to a file relative to current directory new File(".") which should be the same as System.getProperty("user.dir").
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
  def file(fileFullPath: String, optionalParent: BaseLocation): FileLocation =
    file(if (isAbsolute(fileFullPath)) fileFullPath else optionalParent.absolute + fileFullPath)

  private def isAbsolute(path: String) = new File(path).isAbsolute()
  private def createAbsoluteFile(path: String) = {
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
  val SEP = File.separator
  val SEP_STANDARD = "/"

  def relative(path: String = ""): RelativeLocation = RelativeLocation(path)
  def current(relative: String): FileLocation = file(new File(new File("."), relative).getCanonicalPath())
  def standard(path: String): String = path.replaceAllLiterally(SEP, SEP_STANDARD)
}