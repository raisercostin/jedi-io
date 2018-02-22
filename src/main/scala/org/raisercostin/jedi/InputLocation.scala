package org.raisercostin.jedi

import java.io.FileInputStream
import java.io.InputStream

import scala.io.Codec.decoder2codec
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try

import org.apache.commons.io.IOUtils

trait InputLocation extends AbsoluteBaseLocation with ResolvedLocationState with VersionedLocation { self =>
  def unsafeToInputStream: InputStream
  def unsafeToInputStreamIfFile: InputStream = {
    //Return the InputStream only if this is a file. Classpath folder is returning an InputStream with the list of the files.
    if (!isFile)
      throw new RuntimeException("Cannot create inputStream since [" + this + "] is not a file!")
    unsafeToInputStream
  }
  def unsafeToReader: java.io.Reader = new java.io.InputStreamReader(unsafeToInputStreamIfFile, decoder)
  def unsafeToSource: scala.io.BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStreamIfFile)(decoder)
  def bytes: Array[Byte] = {
    //TODO implement
    ??? //IOUtils.readFully(x$1, x$2)
  }

  def usingInputStream[T](op: InputStream => T): T = using(unsafeToInputStreamIfFile)(op)
  def usingReader[T](reader: java.io.Reader => T): T = using(unsafeToReader)(reader)
  def usingSource[T](processor: scala.io.BufferedSource => T): T = using(unsafeToSource)(processor)

  def usingInputStreamAndContinue(op: InputStream => Any): self.type = {using(unsafeToInputStreamIfFile)(op);this}
  def usingReaderAndContinue(reader: java.io.Reader => Any): self.type = {using(unsafeToReader)(reader);this}
  def usingSourceAndContinue(processor: scala.io.BufferedSource => Any): self.type = {using(unsafeToSource)(processor);this}

  def readLines: Iterable[String] = traverseLines.toIterable
  def traverseLines: Traversable[String] = new Traversable[String] {
    def foreach[U](f: String => U): Unit = {
      usingSource { x => x.getLines().foreach(f) }
    }
  }

  def copyToIfNotExists(dest: OutputLocation): self.type = { dest.nonExistingOption.map(_.copyFrom(this)); this }
  def copyTo(dest: OutputLocation)(implicit option: CopyOptions = CopyOptions.simpleCopy): self.type = { dest.copyFrom(self); this}

  def readContent = {
    // Read a file into a string
    //    import rapture._
    //    import core._, io._, net._, uri._, json._, codec._
    //    import encodings.`UTF-8`
    //    val src = uri"http://rapture.io/sample.json".slurp[Char]
    //existing(toSource).getLines mkString ("\n")
    usingReader { reader =>
      try { IOUtils.toString(reader) } catch { case x: Throwable => throw new RuntimeException("While reading " + this, x) }
    }
  }
  def readContentAsText: Try[String] =
    Try(readContent)
  //Try(existing(toSource).getLines mkString ("\n"))
  //def unzip: ZipInputLocation = ???
  def unzip: ZipInputLocation = ZipInputLocation(this, None)
  def cached(implicit cacheConfig: CacheConfig = DefaultCacheConfig): CachedLocation[this.type] = CachedLocation(cacheConfig, this)
  /**Sometimes we want the content to be available locally in the filesystem.*/
  def asFileInputLocation: FileInputLocation = cached.flush
}
