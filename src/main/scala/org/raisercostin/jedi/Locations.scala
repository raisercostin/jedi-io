package org.raisercostin.jedi

import java.io.File
import java.io.InputStream
import java.nio.file.Path

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import org.raisercostin.jedi.impl.{ FileSystemFormatter, JediFileSystem }
import org.raisercostin.jedi.impl.Predef2


/**
 * file(*) - will refer to the absolute path passed as parameter or to a file relative to current directory new File(".") which should be the same as System.getProperty("user.dir") .
 * TODO: file separator agnosticisim: use an internal standard convention indifferent of the "outside" OS convention: Win,Linux,OsX
 * The output will be the same irrespective of the machine that the code is running on. @see org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator
 */
object Locations {
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
  implicit val unixAndWindowsToStandard = JediFileSystem.unixAndWindowsToStandard
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

  def userHome: FileLocation = file(System.getProperty("user.home"))
  lazy val environment: RuntimeEnvironment = RuntimeEnvironment()
}
case class RuntimeEnvironment() {
  //TODO use https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
  val isWindows: Boolean = System.getProperty("os.name").startsWith("Windows")
}