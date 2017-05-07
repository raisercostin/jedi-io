package org.raisercostin.jedi

import java.io.File
import java.io.InputStream
import java.nio.file.Path

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import org.raisercostin.jedi.impl.{FileSystemFormatter,JediFileSystem}
import org.raisercostin.jedi.impl.Predef2

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
trait NavigableInputLocation extends InputLocation with NavigableLocation
trait NavigableInOutLocation extends InOutLocation with NavigableInputLocation with NavigableOutputLocation

/**Location orthogonal dimension: Resolved/Unresolved: Can reach content/cannot.*/
trait LocationState
/**Trait to mark if a location is not resolved to a file system. For example Relative locations or offline urls that 
 * are available in offline mode.*/
trait UnresolvedLocationState extends LocationState
trait ResolvedLocationState extends LocationState

/**
 * file(*) - will refer to the absolute path passed as parameter or to a file relative to current directory new File(".") which should be the same as System.getProperty("user.dir") .
 * TODO: file separator agnosticisim: use an internal standard convention indifferent of the "outside" OS convention: Win,Linux,OsX
 * The output will be the same irrespective of the machine that the code is running on. @see org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator
 */
object Locations {
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
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
    Predef2.requireArgNotNull(path, "path")
    if (isAbsolute(path))
      new FileLocation(path)
    else
      new FileLocation(new File(path).getAbsolutePath())
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
  def userHome:FileLocation = file(System.getProperty("user.home"))
}