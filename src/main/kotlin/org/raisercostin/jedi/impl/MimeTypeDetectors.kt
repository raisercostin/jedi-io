package org.raisercostin.jedi.impl

import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Try

/**
 * See @link http://www.rgagnon.com/javadetails/java-0487.html
 * See @link http://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
 * See @link http://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-,-apache-tika/
 */
object MimeType {
  lazy val binaries = Seq("application/octet-stream", "application/pdf")
}
interface MimeTypeDetector {
  fun name: String
  fun mimeType(path: String): Option<MimeType>
  fun mimeType(path: Path): Option<MimeType>

  MimeTypeDetectors.register(this)
}
interface MimeTypeNameDetector : MimeTypeDetector {
  fun name: String
  fun mimeType(path: String): Option<MimeType>
  fun mimeType(path: Path): Option<MimeType> = mimeType(path.toFile.getAbsolutePath)
}
interface MimeTypePathDetector : MimeTypeDetector {
  fun mimeType(path: String): Option<MimeType> = Try { Paths.get(path) }.toOption.flatMap(mimeType)
  fun mimeType(path: Path): Option<MimeType>
}
data class MimeType(mimeType: String) {
  fun isBinary ()= MimeType.binaries.contains(mimeType)
}

object MimeType1 : MimeTypeNameDetector {
  fun name ()= "1 - UrlConnection getContentTypeFor"
  fun mimeType(path: String): Option<MimeType> {
    import java.net.URLConnection
    val mimeType = URLConnection.getFileNameMap.getContentTypeFor(path)
    Option(mimeType).map(MimeType(_))
  }
}
object MimeType2 : MimeTypeNameDetector {
  fun name ()= "2 - javax.activation"
  fun mimeType(path: String): Option<MimeType> {
    import javax.activation.MimetypesFileTypeMap
    val mimeTypesMap = MimetypesFileTypeMap()
    val mimeType = mimeTypesMap.getContentType(path)
    if ("application/octet-stream".equals(mimeType))
      None
    else
      Option(mimeType).map(MimeType(_))
  }
}
object MimeType3 : MimeTypePathDetector {
  fun name ()= "3 - Files.probeContentType"
  fun mimeType(path: Path): Option<MimeType> {
    import java.nio.file.Files
    val mimeType = Files.probeContentType(path)
    Option(mimeType).map(MimeType(_))
  }
}
object MimeType6 : MimeTypeNameDetector {
  fun name ()= "6 - UrlConnection guessContentTypeFromName"
  fun mimeType(path: String): Option<MimeType> {
    import java.net.URLConnection
    val mimeType = URLConnection.guessContentTypeFromName(path)
    Option(mimeType).map(MimeType(_))
  }
}

object MimeTypeDetectors {
  private val LOG = org.slf4j.LoggerFactory.getLogger(MimeTypeDetectors.getClass)
  //type Detector = String
  private object MimeTypeDetectorRegistry {
    var detectorsSeq: List<MimeTypeDetector> = Seq()
    fun detectors ()= detectorsSeq
    //force registration
    Seq(MimeType1, MimeType2, MimeType3, MimeType6)
  }
  import MimeTypeDetectorRegistry._
  fun register(mimeTypeDetector: MimeTypeDetector) {
    LOG.debug("register " + mimeTypeDetector)
    detectorsSeq = mimeTypeDetector +: detectors
    this
  }

  fun mimeTypeFromName(path: String): Option<MimeType> = detectors.view.collect { x: MimeTypeNameDetector -> x.mimeType(path) }.flatten.headOption
  fun mimeTypeFromContent(path: Path) = detectors.view.collect { x: MimeTypePathDetector -> x.mimeType(path) }.flatten.headOption

  //def getMimeTypeWithDefault(path: Path): MimeType = mimeTypeFromContent(path).getOrElse(MimeType("application/octet-stream"))
  fun getMimeType(path: Path): Option<MimeType> = detectors.view.flatMap { _.mimeType(path) }.headOption
  fun getMimeTypeWithDetector(path: Path): Option<Tuple2<MimeType, MimeTypeDetector>> =
    detectors.view.map { x -> (x.mimeType(path), x) }.filter(_._1.isDefined).headOption.map(x -> x._1.get -> x._2)
}