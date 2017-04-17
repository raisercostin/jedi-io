package org.raisercostin.jedi

import java.io.FileInputStream
import java.io.InputStream

import scala.io.Codec.decoder2codec
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try

import org.apache.commons.io.IOUtils

trait InputLocation extends AbsoluteBaseLocation{
  //import org.apache.commons.io.input.BOMInputStream
  //import org.apache.commons.io.IOUtils
  //def toBomInputStream: InputStream = new BOMInputStream(unsafeToInputStream,false)
  //def toSource: BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream, "UTF-8")

  protected def unsafeToInputStream: InputStream = new FileInputStream(absolute)
  protected def unsafeToReader: java.io.Reader = new java.io.InputStreamReader(unsafeToInputStream, decoder)
  protected def unsafeToSource: scala.io.BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStream)(decoder)
  def usingInputStream[T](op: InputStream => T): T = using(unsafeToInputStream)(op)
  def usingReader[T](reader: java.io.Reader => T): T = using(unsafeToReader)(reader)
  def usingSource[T](processor: scala.io.BufferedSource => T): T = using(unsafeToSource)(processor)

  def readLines: Iterable[String] = traverseLines.toIterable
  def traverseLines: Traversable[String] = new Traversable[String] {
    def foreach[U](f: String => U): Unit = {
      usingSource { x => x.getLines().foreach(f) }
    }
  }

  def bytes: Array[Byte] = org.apache.commons.io.FileUtils.readFileToByteArray(toFile)
  def copyToIfNotExists(dest: OutputLocation): this.type = { dest.nonExistingOption.map(_.copyFrom(this)); this }
  def copyTo(dest: OutputLocation):this.type = copyToOutputLocation(dest)
  def copyTo(dest: NavigableOutputLocation):this.type = {
    dest.mkdirOnParentIfNecessary
    copyToOutputLocation(dest)
  }
  private def copyToOutputLocation(dest: OutputLocation):this.type = {
    usingInputStream { source =>
      dest.usingOutputStream { output =>
        IOUtils.copyLarge(source, output)
      }
    }
    this
  }
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
  def copyAsHardLink(dest: OutputLocation, overwriteIfAlreadyExists: Boolean = false): this.type = {
    dest.copyFromAsHardLink(this, overwriteIfAlreadyExists);
    this
  }
  def unzip: ZipInputLocation = new ZipInputLocation(this, None)
}