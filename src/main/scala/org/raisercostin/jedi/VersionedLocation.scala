package org.raisercostin.jedi

import java.io.File
import org.apache.commons.codec.digest.DigestUtils
import java.util.UUID
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Path
import java.nio.file.attribute.FileAttributeView
import scala.annotation.tailrec

/**
 * A versioned location needs to be resolved since the version refers to the content.
 * For a versioned location you can get an etag that can be used later to detect if file was changed.
 * This might be needed for implementing caching mechanisms.
 *
 * Type of etags:
 * - weak - they only indicate two representations are semantically equivalent.
 * - strong
 * -- shallow - see apache implementation http://httpd.apache.org/docs/2.0/mod/core.html#fileetag based on
 * --- inode - they vary from system to system - see inode in java http://www.javacodex.com/More-Examples/1/8
 * --- mtime
 * --- size
 * --- all
 * -- deep
 *
 * Resources:
 * - https://bitworking.org/news/150/REST-Tip-Deep-etags-give-you-more-benefits
 * - https://unix.stackexchange.com/questions/192800/does-the-inode-change-when-renaming-or-moving-a-file
 * - http://bitworking.org/news/ETags__This_stuff_matters
 * - https://www.infoq.com/articles/java7-nio2
 */
trait VersionedLocation extends ResolvedLocationState {
  /**In worst case every location is considered to have a different version indifferent of content
  Two files with same version should likely be identical.
  Problems:
  - files with same content on different servers
     => Compute a fast md5 on size, start, end?
     => Make sure they are replicated with the same **controlled** name, timestamp etc.
   Solution)
    A file could have a name like: <name>-<changeTimestamp>-<counter>.<extension>
     - When reading the file with specific name and latest changeTimestamp is returned. Version is the <changeTimestamp>-<counter>.
     - On replicated they should see the same "change" so a file with identical name.
     - A separator is needed for versioned files that implement this policy. 
  */
  def version: String = UUID.randomUUID().toString()
  def versionOfContent: String = ???
  /**The default etag is based on the strongETag.*/
  def etag: String = strongETag
  /**The efficient strong tag is a shallow one.*/
  def strongETag: String = shallowETag
  /**A not so efficient strong tag that is based on the content.*/
  def strongDeepETag: String = DigestUtils.sha1Hex(versionOfContent)
  /**The shallowETag shouldn't need access to content. The default one is a sha1Hex of the `version`.*/
  def shallowETag: String = DigestUtils.sha1Hex(version)
  /**A weak ETag doesn't change if two representations are semantically equivalent.
   * After removal of a timestamp from content for example.
   * It is hard to compute and is not sure what it means.*/
  @deprecated("Use strongETag since a weak etag is not clear how to be computed.", "0.33")
  def weakETag: String = throw new RuntimeException("Is not clear how to compute them.")
}

trait FileVersionedLocation extends VersionedLocation { self: FileAbsoluteBaseLocation =>
  def uniqueId: String = attributes.inode.getOrElse(DigestUtils.sha1Hex(canonicalOverSymLinks))
  def canonical = toFile.getCanonicalPath
  def canonicalOverSymLinks = {
    target(toPath,10).right.get.toFile().getCanonicalPath
  }
  //TODO transform it into a Stream or better an Observable?
  @tailrec private def target(path: Path, maxDepth: Int): Either[Path, Path] =
    if (maxDepth <= 0)
      Left(path)
    else if (Files.isSymbolicLink(path))
      target(Files.readSymbolicLink(path), maxDepth - 1)
    else
      Right(path)

  def versionFromUniqueId: String = uniqueId
  def versionFromModificationTime: String = attributes.basic.lastModifiedTime().toMillis().toString
  private def versionFromSize: String = toFile.length().toString


  //inspired by http://httpd.apache.org/docs/2.0/mod/core.html#fileetag
  override def version: String = versionFromUniqueId + "-" + versionFromModificationTime + "-" + versionFromSize
}