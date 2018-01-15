package org.raisercostin.jedi

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import scala.Traversable
import scala.collection.JavaConverters._
import scala.io.BufferedSource
import scala.util.Try
import org.apache.commons.io.{ FileUtils => CommonsFileUtils }
import org.apache.commons.io.FilenameUtils
import java.nio.file.FileStore
import org.raisercostin.jedi.impl._
import java.nio.file.LinkOption

trait AbsoluteBaseLocation extends BaseLocation with ResolvedLocationState { self =>
  type Repr = self.type
  def uniqueId: String = raw
  def toUrl: java.net.URL = ???
  def size: Long
  final def length: Long = size
  def exists: Boolean
  protected def using[A <% AutoCloseable, B](resource: A)(f: A => B): B = {
    import scala.language.reflectiveCalls
    try f(resource) finally resource.close()
  }
  import scala.language.implicitConversions
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

  override def mimeType = mimeTypeFromName.orElse(mimeTypeFromContent)
  /**To read data you should read the inputstream*/
  def mimeTypeFromContent = MimeTypeDetectors.mimeTypeFromContent(toPath)

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