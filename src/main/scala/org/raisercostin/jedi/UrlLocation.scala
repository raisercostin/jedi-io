package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import org.raisercostin.jedi.impl.ResourceUtil
import sun.net.www.protocol.file.FileURLConnection
import org.raisercostin.jedi.impl.SlfLogger

case class UrlLocation(url: java.net.URL, agent: Option[String] = None, allowedRedirects: Int = 5, redirects: Seq[UrlLocation] = Seq()) extends InputLocation { self =>
  def raw = url.toExternalForm()
  override def toUrl: java.net.URL = url
  override def nameAndBefore: String = url.getPath
  def toFile: File = ???
  import java.net._
  override def length: Long = lengthTry.get
  def lengthTry: Try[Long] = ResourceUtil.cleanly(url.openConnection()) {
    case c: HttpURLConnection =>
      c.disconnect()
    case f: FileURLConnection =>
      f.close()
  } {
    case conn: HttpURLConnection =>
      //User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36
      agent.foreach(agent => conn.setRequestProperty("User-Agent", agent))
      conn.setRequestMethod("HEAD")
      if (conn.getResponseCode != 200)
        throw new RuntimeException("A redirect is needed. Cannot compute size!")
      val len = conn.getContentLengthLong()
      if (len < 0) throw new RuntimeException("Invalid length " + len + " received!")
      len
    case conn: FileURLConnection =>
      //conn.getInputStream
      val len = conn.getContentLengthLong()
      if (len < 0) throw new RuntimeException("Invalid length " + len + " received!")
      len
  }
  protected override def unsafeToInputStream: InputStream = {
    url.openConnection() match {
      case conn: HttpURLConnection =>
        agent.foreach(agent => conn.setRequestProperty("User-Agent", agent))
        import scala.collection.JavaConverters._
        if (SlfLogger.log.isDebugEnabled())
          SlfLogger.log.debug("header:\n" + conn.getHeaderFields.asScala.mkString("\n"))
        (conn.getResponseCode, conn.getHeaderField("Location")) match {
          case (200, _) =>
            conn.getInputStream
          case (code, location) if allowedRedirects > redirects.size && location!=null && location.nonEmpty && location != raw =>
            conn.disconnect()
            UrlLocation(new java.net.URL(location), agent, allowedRedirects, this +: redirects).unsafeToInputStream
          case (code, _) =>
            conn.disconnect()
            throw new RuntimeException(s"Got $code response from $this. A 200 code is needed to get an InputStream. The header is\n    " + conn.getHeaderFields.asScala.mkString("\n    ")
              + " After " + allowedRedirects + " redirects:\n    " + redirects.mkString("\n    "))
        }
      case conn =>
        conn.getInputStream
    }
  }

  def withAgent(newAgent: String) = this.copy(agent = Some(newAgent))
  def withoutAgent = this.copy(agent = None)
}