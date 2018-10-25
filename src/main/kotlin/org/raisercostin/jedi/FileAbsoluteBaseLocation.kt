package org.raisercostin.jedi

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import scala.Traversable
import scala.collection.JavaConverters.*
import scala.io.BufferedSource
import scala.util.Try
import org.apache.commons.io.FileUtils -> CommonsFileUtils
import org.apache.commons.io.FilenameUtils
import java.nio.file.FileStore
import org.raisercostin.jedi.impl.*
import java.nio.file.LinkOption

interface AbsoluteBaseLocation : BaseLocation , ResolvedLocationState { self ->
  fun uniqueId: String = raw
  fun toUrl: java.net.URL = ???
  fun size: Long
  fun length: Long = size
  fun exists: Boolean
  protected fun using<A <% AutoCloseable, B>(resource: A)(f: A -> B): B {
    import scala.language.reflectiveCalls
    try f(resource) finally resource.close()
  }
  import scala.language.implicitConversions
  implicit fun toAutoCloseable(source: scala.io.BufferedSource): AutoCloseable = AutoCloseable {
    override fun close() = source.close()
  }
  fun nonExisting(process: (this.type) -> Any): self.type {
    if (!exists) process(this)
    this
  }
  fun existing(source: BufferedSource) {
    //if (source.nonEmpty)
    val hasNext = Try { source.hasNext }
    val hasNext2 = hasNext.recover {
      ex: Throwable ->
        throw RuntimeException("<" + this + "> doesn't exist!")
    }
    //hasNext might be false if is empty
    //    if (!hasNext2.get)
    //      throw RuntimeException("<" + self + "> doesn't have next!")
    source
  }
  fun existing: self.type =
    if (exists)
      this
    else
      throw RuntimeException("<" + this + "> doesn't exist!")
  fun existingOption: Option<self.type> =
    if (exists)
      Some(this)
    else
      None
  fun nonExistingOption: Option<self.type> =
    if (exists)
      None
    else
      Some(this)
}
///**Trait to mark if a location is resolved to a file system.*/
//interface FileResolvedLocationState : ResolvedLocationState , FileVersionedLocation{self:FileAbsoluteBaseLocation ->
//  fun toFile: File
//}

interface FileAbsoluteBaseLocation : AbsoluteBaseLocation , ResolvedLocationState , FileVersionedLocation { self ->
  fun toFile: File
  override fun toUrl: java.net.URL = toFile.toURI.toURL

  override fun mimeType ()= mimeTypeFromName.orElse(mimeTypeFromContent)
  /**To read data you should read the inputstream*/
  fun mimeTypeFromContent ()= MimeTypeDetectors.mimeTypeFromContent(toPath)

  fun toPath: Path = toFile.toPath
  fun toPath(subFile: String): Path = toPath.resolve(subFile)
  fun mkdirIfNecessary: self.type {
    CommonsFileUtils.forceMkdir(toFile)
    this
  }
  private fun listPath(glob: String): List<Path> {
    val stream = Files.newDirectoryStream(toPath, glob)
    try stream.asScala.toList finally stream.close
  }

  fun hasDirs ()= listPath("*").find(_.toFile.isDirectory).nonEmpty
  fun isFile ()= toFile.isFile
  fun isFolder ()= toFile.isDirectory
  fun isSymlink ()= Files.isSymbolicLink(toPath)
  fun symlink: Try<FileLocation> = Try { FileLocation(Files.readSymbolicLink(toPath)) }
  //TODO this one is not ok attributes.basic.isSymbolicLink
  fun exists: Boolean {
    toFile.exists()
//    Files.exists(toPath)
//    val a = toFile
//    a.exists
  }
  fun existsWithoutResolving ()= if (isSymlink)
    Files.exists(toPath, LinkOption.NOFOLLOW_LINKS)
  else
    exists
  fun nameAndBefore: String = absolute
  override fun size: Long = toFile.length()
  fun absolute: String = standard(_.absolutePlatformDependent)
  fun absoluteWindows: String = standardWindows(_.absolutePlatformDependent)
  fun absolutePlatformDependent: String = toPath("").toAbsolutePath.toString
  fun isAbsolute ()= toFile.isAbsolute()
  /**Gets only the path part (,out drive name on windows for example), and ,out the name of file*/
  fun path: String = FilenameUtils.getPath(absolute)
  fun attributes: FileAttributes = FileAttributes(this)
}
/**
 * See
 * - http://javapapers.com/java/file-attributes-using-java-nio/
 * - https://jakubstas.com/links-nio-2
 * - http://www.java2s.com/Tutorials/Java/Java_io/1000__Java_nio_File_Attributes.htm
 * - https://docs.oracle.com/javase/tutorial/essential/io/fileAttr.html
 * - http://cr.openjdk.java.net/~alanb/7017446/webrev/test/java/nio/file/Files/FileAttributes.java-.html
 */
data class FileAttributes(location: FileAbsoluteBaseLocation) {
  import java.nio.file.attribute.FileOwnerAttributeView
  //  import java.nio.file.attribute.PosixFileAttributes
  //  import sun.nio.fs.WindowsFileAttributes
  //  import java.nio.file.attribute.DosFileAttributes
  //  import com.sun.nio.zipfs.ZipFileAttributes

  fun basic: BasicFileAttributes = Files.readAttributes(location.toPath, classOf<BasicFileAttributes>)
  //  private fun zip:ZipFileAttributes = Files.readAttributes(location.toPath,classOf<ZipFileAttributes>)
  //  private fun dos:DosFileAttributes = Files.readAttributes(location.toPath,classOf<DosFileAttributes>)
  //  fun windows:WindowsFileAttributes = Files.readAttributes(location.toPath,classOf<WindowsFileAttributes>)
  //  private fun posix:PosixFileAttributes = Files.readAttributes(location.toPath,classOf<PosixFileAttributes>)

  //  fun aclView:AclFileAttributeView = Files.getFileAttributeView(location.toPath,classOf<AclFileAttributeView>)
  //  fun basicView:BasicFileAttributeView = Files.getFileAttributeView(location.toPath,classOf<BasicFileAttributeView>)
  //  fun dosView:DosFileAttributeView = Files.getFileAttributeView(location.toPath,classOf<DosFileAttributeView>)
  //  fun fileView:FileAttributeView = Files.getFileAttributeView(location.toPath,classOf<FileAttributeView>)
  //  fun fileStoreView:FileStoreAttributeView = Files.getFileAttributeView(location.toPath,classOf<FileStoreAttributeView>)
  //  fun posixFileView:PosixFileAttributeView = Files.getFileAttributeView(location.toPath,classOf<PosixFileAttributeView>)
  //  fun userDefinedView:UserDefinedFileAttributeView = Files.getFileAttributeView(location.toPath,classOf<UserDefinedFileAttributeView>)

  fun inode: Option<String> {
    //code from http://www.javacodex.com/More-Examples/1/8
    Option(basic.fileKey()).map(_.toString()).map(all ->
      all.substring(all.indexOf("ino=") + 4, all.indexOf(")")))
  }
  fun owner: FileOwnerAttributeView = Files.getFileAttributeView(location.toPath, classOf<FileOwnerAttributeView>)
  fun toMap: Map<String, Object> {
    Files.readAttributes(location.toPath, "*").asScala.toMap
  }
  fun fileStore: FileStore = Files.getFileStore(location.toPath);
}