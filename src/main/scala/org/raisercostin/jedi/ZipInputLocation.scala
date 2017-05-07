package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.Iterable
import scala.util.Try
case class ZipInputLocation(zip: InputLocation, entry: Option[java.util.zip.ZipEntry]) extends ZipInputLocationLike {self=>
  override type Repr = self.type
  override def build(path:String): Repr = ???
  override def parent: Repr = ZipInputLocation(zip, Some(rootzip.getEntry(parentName)))
  override def childName(child:String):String = ???
  override def child(child: String): Repr = (entry match {
    case None =>
      ZipInputLocation(zip, Some(rootzip.getEntry(child)))
    case Some(entry) =>
      ZipInputLocation(zip, Some(rootzip.getEntry(entry.getName() + "/" + child)))
  })
  override def list: Iterable[Repr] = Option(existing).map(_ => entries).getOrElse(Iterable()).map(entry => toRepr(ZipInputLocation(zip, Some(entry))))
  override def toFile = zip match {
    case zip:FileAbsoluteBaseLocation => zip.toFile
    case _ => println(zip.toString()); ???
  }
}
//TODO fix name&path&unique identifier stuff
trait ZipInputLocationLike extends NavigableInputLocation { self =>
  override type Repr = self.type
  def zip: InputLocation
  def entry: Option[java.util.zip.ZipEntry]
  def raw = "ZipInputLocation[" + zip + "," + entry + "]"

  //def toFile: File = zip.toFile
  override def unsafeToInputStream: InputStream = entry match {
    case None =>
      throw new RuntimeException("Can't read stream from zip folder " + self)
    case Some(entry) =>
      rootzip.getInputStream(entry)
  }

  protected lazy val rootzip = new java.util.zip.ZipFile(Try { toFile }.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
  //private lazy val rootzip = new java.util.zip.ZipInputStream(zip.unsafeToInputStream)
  import collection.JavaConverters._
  import java.util.zip._
  protected lazy val entries: Iterable[ZipEntry] = new Iterable[ZipEntry] {
    def iterator = rootzip.entries.asScala
  }
  override def name = entry.map(_.getName).getOrElse(zip.name + "-unzipped")
  override def unzip: ZipInputLocation = usingInputStream { input =>
    new ZipInputLocation(Locations.temp.randomChild(name).copyFrom(Locations.stream(input)), None)
  }
}
