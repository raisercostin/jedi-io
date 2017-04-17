package org.raisercostin.jedi

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import scala.Traversable
import scala.collection.JavaConverters._
import scala.io.BufferedSource
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import org.apache.commons.io.{FileUtils=>CommonsFileUtils}
import org.apache.commons.io.FilenameUtils

trait AbsoluteBaseLocation extends BaseLocation with FileResolvedLocationState{
  def toUrl: java.net.URL = toFile.toURI.toURL

  override def mimeType = mimeTypeFromName.orElse(mimeTypeFromContent)
  /**To read data you should read the inputstream*/
  def mimeTypeFromContent = MimeTypeDetectors.mimeTypeFromContent(toPath)

  def toPath: Path = toFile.toPath
  def toPath(subFile: String): Path = toPath.resolve(subFile)
  def size = toFile.length()
  def absolutePlatformDependent: String = toPath("").toAbsolutePath.toString
  def mkdirIfNecessary: this.type = {
    CommonsFileUtils.forceMkdir(toFile)
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
    try stream.asScala.toList finally stream.close
  }

  def hasDirs = listPath("*").find(_.toFile.isDirectory).nonEmpty
  def isFile = toFile.isFile
  def exists = toFile.exists
  def nonExisting(process: (this.type) => Any): this.type = {
    if (!exists) process(this)
    this
  }

  def existing: this.type =
    if (exists)
      this
    else
      throw new RuntimeException("[" + this + "] doesn't exist!")
  def existingOption: Option[this.type] =
    if (exists)
      Some(this)
    else
      None
  def nonExistingOption: Option[this.type] =
    if (exists)
      None
    else
      Some(this)
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
