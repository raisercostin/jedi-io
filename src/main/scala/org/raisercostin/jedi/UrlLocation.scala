package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import org.raisercostin.jedi.impl.ResourceUtil
import sun.net.www.protocol.file.FileURLConnection
import org.raisercostin.jedi.impl.SlfLogger
import org.raisercostin.jedi.impl.Http2
import org.raisercostin.jedi.impl.HttpRequest
import org.raisercostin.jedi.impl.DefaultConnectFunc
import scalaj.http.HttpConstants
import org.raisercostin.jedi.impl.QueryStringUrlFunc
import java.net.HttpURLConnection

case class UrlLocation(url: java.net.URL, agent: Option[String] = None, allowedRedirects: Int = 5, redirects: Seq[UrlLocation] = Seq(), header: Map[String, String] = Map()) extends InputLocation { self =>
  def raw = url.toExternalForm()
  //TODO dump intermediate requests/responses
  def followRedirects = allowedRedirects > 0
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
      header.foreach(element => conn.setRequestProperty(element._1, element._2))
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
    unsafeToInputStreamUsingScalaJHttp
    //unsafeToInputStreamUsingJava
  }
  def unsafeToInputStreamUsingScalaJHttp: InputStream = {
    createRequester(raw).withUnclosedConnection.exec {
      case (code, map, stream) =>
        stream
    }.body
  }

  private def createRequester(url: String) = {
    import scalaj.http.Http
    import scalaj.http.HttpOptions
    HttpRequest(
      url = raw,
      method = "GET",
      connectFunc = {
        case (req: HttpRequest, conn: HttpURLConnection) =>
          import scala.collection.JavaConverters._
          SlfLogger.log.info(s"RequestHeaders for $raw:\n    " + conn.getRequestProperties.asScala.mkString("\n    "))
          DefaultConnectFunc.apply(req, conn)
          SlfLogger.log.info(s"ResponseHeaders for $raw:\n    " + conn.getHeaderFields.asScala.mkString("\n    "))
      },
      params = Nil,
      headers = Seq("User-Agent" -> "scalaj-http/1.0"),
      options = HttpConstants.defaultOptions,
      proxyConfig = None,
      charset = HttpConstants.utf8,
      sendBufferSize = 4096,
      urlBuilder = QueryStringUrlFunc,
      compress = true)
        .option(_ setInstanceFollowRedirects followRedirects)
        .option(HttpOptions.connTimeout(1000))
        .option(HttpOptions.readTimeout(1500))
        .headers(header)
  }

  //protected override 
  def unsafeToInputStreamUsingJava: InputStream = {
    url.openConnection() match {
      case conn: HttpURLConnection =>
        agent.foreach(agent => conn.setRequestProperty("User-Agent", agent))
        header.foreach(element => conn.setRequestProperty(element._1, element._2))
        import scala.collection.JavaConverters._
        SlfLogger.log.info("header:\n" + header.mkString("\n    "))
        SlfLogger.log.info(s"RequestHeaders for $raw:\n    " + conn.getRequestProperties.asScala.mkString("\n    "))
        //if (SlfLogger.log.isDebugEnabled())
        SlfLogger.log.info(s"ResponseHeaders for $raw:\n    " + conn.getHeaderFields.asScala.mkString("\n    "))
        (conn.getResponseCode, conn.getHeaderField("Location")) match {
          case (200, _) =>
            conn.getInputStream
          case (code, location) if allowedRedirects > redirects.size && location != null && location.nonEmpty && location != raw =>
            conn.disconnect()
            UrlLocation(new java.net.URL(location), agent, allowedRedirects, this +: redirects).unsafeToInputStream
          case (code, _) =>
            conn.disconnect()
            throw new RuntimeException(s"Got $code response from $this. A 200 code is needed to get an InputStream. The header is\n    " + conn.getHeaderFields.asScala.mkString("\n    ")
              + " After " + redirects.size + " redirects:\n    " + redirects.mkString("\n    "))
        }
      case conn =>
        conn.getInputStream
    }
  }

  def withSensibleAgent = withAgent("User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
  def withAgent(newAgent: String) = this.copy(agent = Some(newAgent))
  def withoutAgent = this.copy(agent = None)
  //def withAcceptAll = this.copy(header = header + ("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", "Connection" -> "keep-alive"))
  /**Usually needed if a 403 is returned.*/
  def withBrowserHeader = this.copy(header = header + (
    "Host" -> url.getHost,
    "User-Agent" -> "curl/7.51.0",
    "Accept" -> "*/*"))
  def withoutRedirect = this.copy(allowedRedirects = 0)
  //val userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36"
  //val cookies = "osCsid=d9bbb1602e315dadfe4a5b6e07832053; MIsid=18d1653b970413564d19469c06c8ebad"
  //Set-Cookie: osCsid=d9bbb1602e315dadfe4a5b6e07832053; path=/;
  //val userAgent = "User-Agent" -> "Wget/1.9"
  //MIsid 20b3258abfd25dfda1d9a2a04088f577
  //Http.configure(_ setFollowRedirects true)(q OK as.String)
  //"Connection" -> "Keep-Alive", "Cookie" -> cookies)
  def resolved: ResolvedUrlLocation = ResolvedUrlLocation(this)
}
//TODO add a resolved state where you can interrogate things like All redirects headers, status code and others.  
case class ResolvedUrlLocation(location: UrlLocation) {
}
