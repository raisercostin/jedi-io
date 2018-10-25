package org.raisercostin.jedi

import java.io.File
import java.io.InputStream
import java.nio.file.Path

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import org.raisercostin.jedi.impl.FileSystemFormatter
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.Predef2


/**
 * file(*) - will refer to the absolute path passed as parameter or to a file relative to current directory File(".") which should be the same as System.getProperty("user.dir") .
 * TODO: file separator agnosticisim: use an internal standard convention indifferent of the "outside" OS convention: Win,Linux,OsX
 * The output will be the same irrespective of the machine that the code is running on. @see org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator
 */
object Locations {
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
  implicit val unixAndWindowsToStandard = JediFileSystem.unixAndWindowsToStandard
  val logger = org.slf4j.LoggerFactory.getLogger("locations")
  fun classpath(resourcePath: String): ClassPathInputLocation = ClassPathInputLocation(resourcePath)
  fun file(path: Path): FileLocation =
    file(path.toFile)
  fun file(fileFullPath: String): FileLocation =
    createAbsoluteFile(fileFullPath)
  fun file(file: File): FileLocation =
    createAbsoluteFile(file.getAbsolutePath())
  fun file(file: File, subFile: String): FileLocation =
    createAbsoluteFile(file.getAbsolutePath()).child(subFile)
  fun file(fileFullPath: String, optionalParent: NavigableFileLocation): FileLocation =
    file(if (isAbsolute(fileFullPath)) fileFullPath else optionalParent.absolute + fileFullPath)

  private fun isAbsolute(path: String) = File(path).isAbsolute()
  private fun createAbsoluteFile(path: String) {
    Predef2.requireArgNotNull(path, "path")
    if (isAbsolute(path))
      FileLocation(path)
    else
      FileLocation(new File(path).getAbsolutePath())
  }
  fun memory(memoryName: String): MemoryLocation =
    MemoryLocation(memoryName)
  fun vfs(url: String): VfsLocation = VfsLocation(url)
  fun stream(stream: InputStream): StreamLocation = StreamLocation(stream)
  fun url(url: java.net.URL): UrlLocation = UrlLocation(url)
  fun url(url: String): UrlLocation = UrlLocation(new java.net.URL(url))
  fun temp: TempLocation = TempLocation(tmpdir)
  private val tmpdir = File(System.getProperty("java.io.tmpdir"))
  fun relative(path: String = "")(implicit fsf: FileSystemFormatter = JediFileSystem.unixAndWindowsToStandard): RelativeLocation = RelativeLocation(fsf.standard(path))
  fun current(relative: String): FileLocation = file(new File(new File("."), relative).getCanonicalPath())

  fun userHome: FileLocation = file(System.getProperty("user.home"))
  lazy val environment: RuntimeEnvironment = RuntimeEnvironment()
}
data class RuntimeEnvironment() {
  //TODO use https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
  val isWindows: Boolean = System.getProperty("os.name").startsWith("Windows")
}