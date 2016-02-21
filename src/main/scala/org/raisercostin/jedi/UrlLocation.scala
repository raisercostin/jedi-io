package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls

case class UrlLocation(url: java.net.URL) extends InputLocation {self=>
  def raw = url.toExternalForm()
  def toFile: File = ???
  import java.net._
  override def length: Long = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("HEAD")
      conn.getInputStream
      val len = conn.getContentLengthLong()
      if (len < 0) throw new RuntimeException("Invalid length received!")
      len
    } catch {
      case e: java.io.IOException => -1
    } finally {
      conn.disconnect()
    }
  }
  protected override def unsafeToInputStream: InputStream = url.openStream()
}