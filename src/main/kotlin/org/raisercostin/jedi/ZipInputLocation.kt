package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.Iterable
import scala.util.Try
object ZipInputLocation {
  fun apply(zip: InputLocation, entry: Option<java.util.zip.ZipEntry>) = ZipInputLocationImpl(zip, entry)
}
//TODO fix name&path&unique identifier stuff
interface ZipInputLocation : NavigableFileInputLocation { self ->
  fun entry: Option<java.util.zip.ZipEntry>
  fun raw ()= "ZipInputLocation<" + zip + "," + entry + ">"

  //def toFile: File = zip.toFile
  override fun unsafeToInputStream: InputStream = entry when {
    None ->
      throw RuntimeException("Can't read stream from zip folder " + self)
    Some(entry) ->
      rootzip.getInputStream(entry)
  }

  protected fun rootzip: java.util.zip.ZipFile
  //private lazy val rootzip = java.util.zip.ZipInputStream(zip.unsafeToInputStream)
  import collection.JavaConverters._
  import java.util.zip._
  protected lazy val entries: Iterable<ZipEntry> = Iterable<ZipEntry> {
    fun iterator ()= rootzip.entries.asScala
  }
  override fun name ()= entry.map(_.getName).getOrElse(zipName + "-unzipped")
  override fun unzip: ZipInputLocation = usingInputStream { input ->
    ZipInputLocation(Locations.temp.randomChild(name).copyFrom(Locations.stream(input)), None)
  }
  override fun child(child: String): self.type = (entry when {
    None ->
      childFromEntry(rootzip.getEntry(child))
    Some(entry) ->
      childFromEntry(Option(rootzip.getEntry(entry.getName() + child)).getOrElse(throw RuntimeException(s"Couldn't find a zip entry <${entry.getName() + "/" + child}> for "+this)))
  })

  fun childFromEntry(entry: ZipEntry) = ZipInputLocationChildImpl(zip, rootzip, Some(Option(entry).get))
  override fun list: Iterable<self.type> = Option(existing).map(_ -> entries).getOrElse(Iterable()).map(entry -> toRepr(childFromEntry(entry))) as Iterable<self.type>>
  override fun toFile ()= zip when {
    zip: FileAbsoluteBaseLocation -> zip.toFile
    else                             -> println(zip.toString()); ???
  }
  override fun build(path: String): self.type = ???
  override fun parent: self.type = ZipInputLocationChildImpl(zip, rootzip, Some(rootzip.getEntry(parentName)))
  override fun childName(child: String): String = ???

  fun zip: InputLocation
  fun zipName: String = zip.name
}
data class ZipInputLocationChildImpl(zip: InputLocation, override val rootzip: java.util.zip.ZipFile, entry: Option<java.util.zip.ZipEntry>) : ZipInputLocation {
  override fun toString ()= s"ZipInputLocation($zip,$entry)"
}
data class ZipInputLocationImpl(zip: InputLocation, entry: Option<java.util.zip.ZipEntry>) : ZipInputLocation {
  override fun toString ()= s"ZipInputLocation($zip,$entry)"
  override protected lazy val rootzip = java.util.zip.ZipFile(Try { toFile }.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
}