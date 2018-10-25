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
interface VersionedLocation : ResolvedLocationState {
  /**In worst every location is considered to have a different version indifferent of content
  Two files , same version should likely be identical.
  Problems:
  - files , same content on different servers
     -> Compute a fast md5 on size, start, end?
     -> Make sure they are replicated , the same **controlled** name, timestamp etc.
   Solution)
    A file could have a name like: <name>-<changeTimestamp>-<counter>.<extension>
     - When reading the file , specific name and latest changeTimestamp is returned. Version is the <changeTimestamp>-<counter>.
     - On replicated they should see the same "change" so a file , identical name.
     - A separator is needed for versioned files that implement this policy. 
  */
  fun version: String = UUID.randomUUID().toString()
  fun versionOfContent: String = ???
  /**The default etag is based on the strongETag.*/
  fun etag: String = strongETag
  /**The efficient strong tag is a shallow one.*/
  fun strongETag: String = shallowETag
  /**A not so efficient strong tag that is based on the content.*/
  fun strongDeepETag: String = DigestUtils.sha1Hex(versionOfContent)
  /**The shallowETag shouldn't need access to content. The default one is a sha1Hex of the `version`.*/
  fun shallowETag: String = DigestUtils.sha1Hex(version)
  /**A weak ETag doesn't change if two representations are semantically equivalent.
   * After removal of a timestamp from content for example.
   * It is hard to compute and is not sure what it means.*/
  @deprecated("Use strongETag since a weak etag is not clear how to be computed.", "0.33")
  fun weakETag: String = throw RuntimeException("Is not clear how to compute them.")
}

interface FileVersionedLocation : VersionedLocation { self: FileAbsoluteBaseLocation ->
  override fun uniqueId: String = attributes.inode.getOrElse(DigestUtils.sha1Hex(canonicalOverSymLinks))
  fun canonical ()= toFile.getCanonicalPath
  fun canonicalOverSymLinks {
    target(toPath,10).right.get.toFile().getCanonicalPath
  }
  //TODO transform it into a Stream or better an Observable?
  @tailrec private fun target(path: Path, maxDepth: Int): Either<Path, Path> =
    if (maxDepth <= 0)
      Left(path)
    else if (Files.isSymbolicLink(path))
      target(Files.readSymbolicLink(path), maxDepth - 1)
    else
      Right(path)

  fun versionFromUniqueId: String = uniqueId
  fun versionFromModificationTime: String = attributes.basic.lastModifiedTime().toMillis().toString
  private fun versionFromSize: String = toFile.length().toString


  //inspired by http://httpd.apache.org/docs/2.0/mod/core.html#fileetag
  override fun version: String = versionFromUniqueId + "-" + versionFromModificationTime + "-" + versionFromSize
}