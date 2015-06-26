package org.raisercostin.util.io

import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Try

/**
 * See @link http://www.rgagnon.com/javadetails/java-0487.html
 * See @link http://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
 * See @link http://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
 */
object MimeType {
  lazy val binaries = Seq("application/octet-stream", "application/pdf")
}
trait MimeTypeDetector {
  def name: String
  def mimeType(path: String): Option[MimeType]
  def mimeType(path: Path): Option[MimeType]

  MimeTypeDetectors.register(this)
}
trait MimeTypeNameDetector extends MimeTypeDetector {
  def name: String
  def mimeType(path: String): Option[MimeType]
  def mimeType(path: Path): Option[MimeType] = mimeType(path.toFile.getAbsolutePath)
}
trait MimeTypePathDetector extends MimeTypeDetector {
  def mimeType(path: String): Option[MimeType] = Try { Paths.get(path) }.toOption.flatMap(mimeType)
  def mimeType(path: Path): Option[MimeType]
}
case class MimeType(mimeType: String) {
  def isBinary = MimeType.binaries.contains(mimeType)
}

case object MimeType1 extends MimeTypeNameDetector {
  def name = "1 - UrlConnection getContentTypeFor"
  def mimeType(path: String): Option[MimeType] = {
    import java.net.URLConnection
    val mimeType = URLConnection.getFileNameMap.getContentTypeFor(path)
    Option(mimeType).map(MimeType(_))
  }
}
case object MimeType2 extends MimeTypeNameDetector {
  def name = "2 - javax.activation"
  def mimeType(path: String): Option[MimeType] = {
    import javax.activation.MimetypesFileTypeMap
    val mimeTypesMap = new MimetypesFileTypeMap()
    val mimeType = mimeTypesMap.getContentType(path)
    if ("application/octet-stream".equals(mimeType))
      None
    else
      Option(mimeType).map(MimeType(_))
  }
}
case object MimeType3 extends MimeTypePathDetector {
  def name = "3 - Files.probeContentType"
  def mimeType(path: Path): Option[MimeType] = {
    import java.nio.file.Files
    val mimeType = Files.probeContentType(path)
    Option(mimeType).map(MimeType(_))
  }
}
case object MimeType6 extends MimeTypeNameDetector {
  def name = "6 - UrlConnection guessContentTypeFromName"
  def mimeType(path: String): Option[MimeType] = {
    import java.net.URLConnection
    val mimeType = URLConnection.guessContentTypeFromName(path)
    Option(mimeType).map(MimeType(_))
  }
}

object MimeTypeDetectors {
  type Detector = String
  private object MimeTypeDetectorRegistry {
    var detectorsSeq: Seq[MimeTypeDetector] = Seq()
    def detectors = detectorsSeq
    //force registration
    Seq(MimeType1, MimeType2, MimeType3, MimeType6)
  }
  import MimeTypeDetectorRegistry._
  def register(mimeTypeDetector: MimeTypeDetector) = {
    println("register " + mimeTypeDetector)
    detectorsSeq = mimeTypeDetector +: detectors
    this
  }

  def mimeTypeFromName(path: String): Option[MimeType] = detectors.view.collect { case x: MimeTypeNameDetector => x.mimeType(path) }.flatten.headOption
  def mimeTypeFromContent(path: Path) = detectors.view.collect { case x: MimeTypePathDetector => x.mimeType(path) }.flatten.headOption

  //def getMimeTypeWithDefault(path: Path): MimeType = mimeTypeFromContent(path).getOrElse(MimeType("application/octet-stream"))
  def getMimeType(path: Path): Option[MimeType] = detectors.view.flatMap { _.mimeType(path) }.headOption
  def getMimeTypeWithDetector(path: Path): Option[Tuple2[MimeType, MimeTypeDetector]] = detectors.view.map { x => (x.mimeType(path), x) }.filter(_._1.isDefined).headOption.map(x => x._1.get -> x._2)
}