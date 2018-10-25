package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.Iterable
import scala.util.Try
object ZipInputLocation {
  def apply(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) = ZipInputLocationImpl(zip, entry)
}
//TODO fix name&path&unique identifier stuff
trait ZipInputLocation extends NavigableFileInputLocation { self =>
  def entry: Option[java.util.zip.ZipEntry]
  def raw = "ZipInputLocation[" + zip + "," + entry + "]"

  //def toFile: File = zip.toFile
  override def unsafeToInputStream: InputStream = entry match {
    case None =>
      throw new RuntimeException("Can't read stream from zip folder " + self)
    case Some(entry) =>
      rootzip.getInputStream(entry)
  }

  protected def rootzip: java.util.zip.ZipFile
  //private lazy val rootzip = new java.util.zip.ZipInputStream(zip.unsafeToInputStream)
  import collection.JavaConverters._
  import java.util.zip._
  protected lazy val entries: Iterable[ZipEntry] = new Iterable[ZipEntry] {
    def iterator = rootzip.entries.asScala
  }
  override def name = entry.map(_.getName).getOrElse(zipName + "-unzipped")
  override def unzip: ZipInputLocation = usingInputStream { input =>
    ZipInputLocation(Locations.temp.randomChild(name).copyFrom(Locations.stream(input)), None)
  }
  override def child(child: String): self.type = (entry match {
    case None =>
      childFromEntry(rootzip.getEntry(child))
    case Some(entry) =>
      childFromEntry(Option(rootzip.getEntry(entry.getName() + child)).getOrElse(throw new RuntimeException(s"Couldn't find a zip entry [${entry.getName() + "/" + child}] for "+this)))
  })

  def childFromEntry(entry: ZipEntry) = ZipInputLocationChildImpl(zip, rootzip, Some(Option(entry).get))
  override def list: Iterable[self.type] = Option(existing).map(_ => entries).getOrElse(Iterable()).map(entry => toRepr(childFromEntry(entry))).asInstanceOf[Iterable[self.type]]
  override def toFile = zip match {
    case zip: FileAbsoluteBaseLocation => zip.toFile
    case _                             => println(zip.toString()); ???
  }
  override def build(path: String): self.type = ???
  override def parent: self.type = ZipInputLocationChildImpl(zip, rootzip, Some(rootzip.getEntry(parentName)))
  override def childName(child: String): String = ???

  def zip: InputLocation
  def zipName: String = zip.name
}
case class ZipInputLocationChildImpl(zip: InputLocation, override val rootzip: java.util.zip.ZipFile, entry: Option[java.util.zip.ZipEntry]) extends ZipInputLocation {
  override def toString = s"ZipInputLocation($zip,$entry)"
}
case class ZipInputLocationImpl(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) extends ZipInputLocation {
  override def toString = s"ZipInputLocation($zip,$entry)"
  override protected lazy val rootzip = new java.util.zip.ZipFile(Try { toFile }.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
}
