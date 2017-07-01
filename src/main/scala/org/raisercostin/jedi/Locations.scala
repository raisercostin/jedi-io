package org.raisercostin.jedi

import java.io.File
import java.io.InputStream
import java.nio.file.Path

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import org.raisercostin.jedi.impl.{ FileSystemFormatter, JediFileSystem }
import org.raisercostin.jedi.impl.Predef2
import scala.util.Try
import scala.util.Success

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
  def copyFromFolder(src: NavigableInputLocation)(implicit option:CopyOptions=CopyOptions.simpleCopy): Repr = {
    if (!src.isFolder)
      throw new RuntimeException(s"Src $src is not a folder")
    src.descendants.map { x =>
      val rel = x.extractPrefix(src).get
      val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
      println(f"""copy ${rel.raw}%-40s $x -> $y""")
    }
    this
  }
  def copyFromFileToFileOrFolder(from: InputLocation)(implicit option:CopyOptions=CopyOptions.simpleCopy): Repr = {
    def copyMeta(meta:Try[MetaRepr]):Unit={
      if (option.copyMeta){
        if(!option.optionalMeta || meta.isSuccess && meta.get.exists)
          meta.get.copyFromInputLocation(from.metaLocation.get)
        else
          option.monitor.warn("Optional meta "+meta+" doesn't exists. Ignored.")
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

/**
 * There might be ones that are both? Or none? Or undecided?
 */
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

/**
 * file(*) - will refer to the absolute path passed as parameter or to a file relative to current directory new File(".") which should be the same as System.getProperty("user.dir") .
 * TODO: file separator agnosticisim: use an internal standard convention indifferent of the "outside" OS convention: Win,Linux,OsX
 * The output will be the same irrespective of the machine that the code is running on. @see org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator
 */
object Locations {
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
  val logger = org.slf4j.LoggerFactory.getLogger("locations")
  def classpath(resourcePath: String): ClassPathInputLocation = ClassPathInputLocation(resourcePath)
  def file(path: Path): FileLocation =
    file(path.toFile)
  def file(fileFullPath: String): FileLocation =
    createAbsoluteFile(fileFullPath)
  def file(file: File): FileLocation =
    createAbsoluteFile(file.getAbsolutePath())
  def file(file: File, subFile: String): FileLocation =
    createAbsoluteFile(file.getAbsolutePath()).child(subFile)
  def file(fileFullPath: String, optionalParent: NavigableFileLocation): FileLocation =
    file(if (isAbsolute(fileFullPath)) fileFullPath else optionalParent.absolute + fileFullPath)

  private def isAbsolute(path: String) = new File(path).isAbsolute()
  private def createAbsoluteFile(path: String) = {
    Predef2.requireArgNotNull(path, "path")
    if (isAbsolute(path))
      FileLocation(path)
    else
      FileLocation(new File(path).getAbsolutePath())
  }
  def memory(memoryName: String): MemoryLocation =
    new MemoryLocation(memoryName)
  def vfs(url: String): VfsLocation = VfsLocation(url)
  def stream(stream: InputStream): StreamLocation = new StreamLocation(stream)
  def url(url: java.net.URL): UrlLocation = UrlLocation(url)
  def url(url: String): UrlLocation = UrlLocation(new java.net.URL(url))
  def temp: TempLocation = TempLocation(tmpdir)
  private val tmpdir = new File(System.getProperty("java.io.tmpdir"))
  def relative(path: String = "")(implicit fsf: FileSystemFormatter = JediFileSystem.unixAndWindowsToStandard): RelativeLocation = RelativeLocation(fsf.standard(path))
  def current(relative: String): FileLocation = file(new File(new File("."), relative).getCanonicalPath())

  implicit val unixAndWindowsToStandard = JediFileSystem.unixAndWindowsToStandard
  def userHome: FileLocation = file(System.getProperty("user.home"))
  lazy val environment: RuntimeEnvironment = RuntimeEnvironment()
}
case class RuntimeEnvironment() {
  //TODO use https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
  val isWindows: Boolean = System.getProperty("os.name").startsWith("Windows")
}