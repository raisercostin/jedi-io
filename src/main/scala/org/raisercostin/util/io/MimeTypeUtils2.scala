package org.raisercostin.util.io

import java.nio.file.Path

/**
 * See @link http://www.rgagnon.com/javadetails/java-0487.html
 * See @link http://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
 * See @link http://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
 */
object MimeType{
  lazy val binaries = Seq("application/octet-stream","application/pdf")
}
case class MimeType(mimeType:String){
  def isBinary = MimeType.binaries.contains(mimeType)
}
object MimeTypesUtils2{
  type Detector = String
  def getMimeTypeWithDefault(path: Path): MimeType = {
    getMimeType(path).getOrElse(MimeType("application/octet-stream"))
  }
  def getMimeType(path: Path): Option[MimeType] = {
    getMimeTypeWithDetector(path).map(_._1)
  }
  def getMimeTypeWithDetector(path: Path): Option[Tuple2[MimeType, Detector]] = {
    getMimeType1(path).orElse(getMimeType2(path)).orElse(getMimeType3(path))//.orElse(getMimeType4(path)).orElse(getMimeType5(path))
  }

  def getMimeType1(path: Path): Option[Tuple2[MimeType, Detector]] = {
    import java.net.URLConnection
    val mimeType = URLConnection.getFileNameMap.getContentTypeFor(path.toFile.getName)
    Option(mimeType).map(MimeType(_) -> "1 - UrlConnection getContentTypeFor")
  }
  def getMimeType6(path: Path): Option[Tuple2[MimeType, Detector]] = {
    import java.net.URLConnection
    val mimeType = URLConnection.guessContentTypeFromName(path.toFile.getName)
    Option(mimeType).map(MimeType(_) -> "6 - UrlConnection guessContentTypeFromName")
  }
  def getMimeType2(path: Path): Option[Tuple2[MimeType, Detector]] = {
    import javax.activation.MimetypesFileTypeMap
    val mimeTypesMap = new MimetypesFileTypeMap()
    val mimeType = mimeTypesMap.getContentType(path.toFile)
    if ("application/octet-stream".equals(mimeType))
      None
    else
      Option(mimeType).map(MimeType(_) -> "2 - javax.activation")
  }
  def getMimeType3(path: Path): Option[Tuple2[MimeType, Detector]] = {
    import java.nio.file.Files
    val mimeType = Files.probeContentType(path)
    Option(mimeType).map(MimeType(_) -> "3 - Files.probeContentType")
  }
//  def getMimeType4(path: Path): Option[Tuple2[MimeType, Detector]] = {
//    import resource._ //use scala-arm from http://jsuereth.com/scala-arm/
//    import java.io.BufferedInputStream
//    import java.io.FileInputStream
//    import java.net.URLConnection
//
//    val a = for {
//      in <- managed(new BufferedInputStream(new FileInputStream(path.toFile)))
//      mimeType = URLConnection.guessContentTypeFromStream(in)
//    } yield mimeType
//    val b = a.opt match {
//      case Some("null") => None
//      case Some(null) => None
//      case x: Option[String] => x
//    }
//    b.map(MimeType(_) -> "4 - URLConnection.guessContentTypeFromStream")
//  }
//  def getMimeType5(path: Path): Option[Tuple2[MimeType, Detector]] = {
//    import play.api.libs.MimeTypes
//
//    val mimeType = MimeTypes.forFileName(path.toFile.getName)
//    mimeType.map(MimeType(_) -> "5 - play.api.MimeTypes.forFileName")
//  }
}