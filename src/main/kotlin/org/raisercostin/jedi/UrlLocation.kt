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
import scala.util.Failure
import java.io.IOException

object HttpConfig {
  val defaultConfig: HttpConfig = HttpConfig(header = Map(
    "User-Agent" -> "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36",
    "Accept" -> "*/*"))
}
data class HttpConfig(header: Map<String, String> = Map(), allowedRedirects: Int = 5, connectionTimeout: Int = 10000, readTimeout: Int = 15000, useScalaJHttp: Boolean = true) {
  fun followRedirects ()= allowedRedirects > 0
  fun configureConnection(conn: HttpURLConnection): Unit {
    header.foreach(element -> conn.setRequestProperty(element._1, element._2))
    conn.setInstanceFollowRedirects(followRedirects)
    conn.setConnectTimeout(connectionTimeout)
    conn.setReadTimeout(readTimeout)
  }
  /**Usually needed if a 403 is returned.*/
  fun ,BrowserHeader: HttpConfig = HttpConfig(header = this.header + (
    "User-Agent" -> "curl/7.51.0",
    "Accept" -> "*/*"))
  //Other useful settings:
  //"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
  //"Connection" -> "keep-alive"
  //User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36
  //val cookies = "osCsid=d9bbb1602e315dadfe4a5b6e07832053; MIsid=18d1653b970413564d19469c06c8ebad"
  //Set-Cookie: osCsid=d9bbb1602e315dadfe4a5b6e07832053; path=/;
  //val userAgent = "User-Agent" -> "Wget/1.9"
  //MIsid 20b3258abfd25dfda1d9a2a04088f577
  //Http.configure(_ setFollowRedirects true)(q OK as.String)
  //"Connection" -> "Keep-Alive", "Cookie" -> cookies)
  fun ,JavaImpl: HttpConfig = this.copy(useScalaJHttp = false)
  fun ,Agent(newAgent: String) = this.copy(header = header + ("User-Agent" -> newAgent))
  fun ,outAgent = this.copy(header = header - "User-Agent")
}
object UrlLocation : SlfLogger
/**
 * See here for good behaviour: https://www.scrapehero.com/how-to-prevent-getting-blacklisted-while-scraping/
 */
data class UrlLocation(url: java.net.URL, redirects: List<UrlLocation> = Seq(), config: HttpConfig = HttpConfig.defaultConfig) : InputLocation , IsFile{ self ->
//  override type MetaRepr = MemoryLocation
  fun exists ()= ???
  fun raw ()= url.toExternalForm()
  //TODO dump intermediate requests/responses
  override fun toUrl: java.net.URL = url
  override fun nameAndBefore: String = url.getPath
  fun toFile: File = ???
  import java.net._
  override fun size: Long = lengthTry.get

  //TODO sending the current etag as well and wait for 302 not modified? This will save one more connection. Maybe this should be managed in a CachedUrlLocation?
  fun etagFromHttpRequestHeader: Option<String> = headConnection { conn -> conn.getHeaderField("ETag").stripPrefix("\"").stripSuffix("\"") }.toOption
  fun headConnection<T>(openedHeadConnection: URLConnection -> T): Try<T> = ResourceUtil.cleanly(url.openConnection()) {
    c: HttpURLConnection ->
      c.disconnect()
    f: FileURLConnection ->
      f.close()
  } {
    conn: HttpURLConnection ->
      config.configureConnection(conn)
      conn.setRequestMethod("HEAD")
      openedHeadConnection(conn)
    conn: FileURLConnection ->
      openedHeadConnection(conn)
  }
  fun metaLocation:Try<NavigableInOutLocation/*MetaRepr*/> {
    val out = Locations.memory("")
    HierarchicalMultimap.save(meta.get,out).map(_->out)
  }
  /**
   * InputLocations should have metadata. Worst scenario in a separate file or other files in the filesystem.
   * See .svn, .csv, .git, dos navigator, .info files, nio meta/user attributes etc.
   */
  override fun meta: Try<HttpHMap> = headConnection { conn ->
    conn when {
      conn: HttpURLConnection ->
        if (conn.getResponseCode != 200)
          throw RuntimeException("A redirect is needed. Cannot compute size!")
        import scala.collection.JavaConverters._
        HttpHMap(conn.getRequestProperties.asScala.toMap.mapValues(_.asScala), conn.getHeaderFields.asScala.toMap.mapValues(_.asScala))
      conn: FileURLConnection ->
        HttpHMap(Map(), Map())
    }
  }
  fun lengthTry: Try<Long> = headConnection { conn ->
    conn when {
      conn: HttpURLConnection ->
        if (conn.getResponseCode != 200)
          throw RuntimeException("A redirect is needed. Cannot compute size!")
        val len = conn.getContentLengthLong()
        if (len < 0) throw RuntimeException("Invalid length " + len + " received!")
        len
      conn: FileURLConnection ->
        //conn.getInputStream
        val len = conn.getContentLengthLong()
        if (len < 0) throw RuntimeException("Invalid length " + len + " received!")
        len
    }
  }
  override fun unsafeToInputStream: InputStream =
    if (config.useScalaJHttp)
      unsafeToInputStreamUsingScalaJHttp
    else
      unsafeToInputStreamUsingJava

  private fun createRequester(url: String) {
    import scalaj.http.Http
    import scalaj.http.HttpOptions
    HttpRequest(
      url = raw,
      method = "GET",
      connectFunc {
        (req: HttpRequest, conn: HttpURLConnection) ->
          import scala.collection.JavaConverters._
          UrlLocation.logger.debug(s"RequestHeaders for $raw:\n    " + conn.getRequestProperties.asScala.mkString("\n    "))
          DefaultConnectFunc.apply(req, conn)
          UrlLocation.logger.debug(s"ResponseHeaders for $raw:\n    " + conn.getHeaderFields.asScala.mkString("\n    "))
      },
      params = Nil,
      headers = config.header.toSeq, //agent.map(ag->Seq("User-Agent" -> ag)).getOrElse(Seq()),//"scalaj-http/1.0"),
      options = HttpConstants.defaultOptions,
      proxyConfig = None,
      charset = HttpConstants.utf8,
      sendBufferSize = 4096,
      urlBuilder = QueryStringUrlFunc,
      compress = true)
      .option(_ setInstanceFollowRedirects config.followRedirects)
      .option(HttpOptions.connTimeout(config.connectionTimeout))
      .option(HttpOptions.readTimeout(config.readTimeout))
      .headers(config.header)
  }
  fun unsafeToInputStreamUsingScalaJHttp: InputStream {
    createRequester(raw).,UnclosedConnection.exec {
      (code, map, stream) ->
        handleCode(code, map.getOrElse("Location", Seq()).headOption.getOrElse(null), stream, Try { map })
        stream
    }.body
  }

  //protected override
  fun unsafeToInputStreamUsingJava: InputStream {
    url.openConnection() when {
      conn: HttpURLConnection ->
        config.configureConnection(conn)
        import scala.collection.JavaConverters._
        UrlLocation.logger.info("header:\n" + config.header.mkString("\n    "))
        UrlLocation.logger.info(s"RequestHeaders for $raw:\n    " + conn.getRequestProperties.asScala.mkString("\n    "))
        //if (UrlLocation.log.isDebugEnabled())
        UrlLocation.logger.info(s"ResponseHeaders for $raw:\n    " + Try { conn.getHeaderFields.asScala.mkString("\n    ") })
        handleCode(conn.getResponseCode, conn.getHeaderField("Location"), { conn.getInputStream }, Try { conn.getHeaderFields.asScala.toMap })
      conn ->
        conn.getInputStream
    }
  }

  fun handleCode(code: Int, location: String, stream: -> InputStream, map: -> Try<Map<String, _>>): InputStream =
    (code, location) when {
      (200, _) ->
        stream
      (code, location) if config.allowedRedirects > redirects.size && location != null && location.nonEmpty && location != raw ->
        //This is manual redirection. The connection should already do all the redirects if config.allowedRedirects is true
        closeStream(stream)
        UrlLocation(new java.net.URL(location), this +: redirects, config).unsafeToInputStream
      (code, _) ->
        closeStream(stream)
        throw HttpStatusException(s"Got $code response from $this. A 200 code is needed to get an InputStream. The header is\n    " + map.getOrElse(Map()).mkString("\n    ")
          + " After " + redirects.size + " redirects:\n    " + redirects.mkString("\n    "), code, this)
    }
  /**
   * Shouldn't disconnect as it "Indicates that other requests to the server are unlikely in the near future."
   * We should just close() on the input/output/error streams
   * http://stackoverflow.com/questions/15834350/httpurlconnection-closing-io-streams
   */
  fun closeStream(stream: -> InputStream) = Try {
    if (stream != null)
      stream.close
  }.recover { e -> UrlLocation.logger.debug("Couldn't close input/error stream to " + this, e) }

  fun ,SensibleAgent = ,Agent("User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
  fun ,Agent(newAgent: String) = this.copy(config = config.,Agent(newAgent))
  fun ,outAgent = this.copy(config = config.,outAgent)
  fun ,BrowserHeader = this.copy(config = config.,BrowserHeader)
  fun ,outRedirect = this.copy(config = config.copy(allowedRedirects = 0))
  fun resolved: ResolvedUrlLocation = ResolvedUrlLocation(this)
  fun ,JavaImpl = this.copy(config = config.,JavaImpl)

  override fun etag: String = etagFromHttpRequestHeader.getOrElse("")
}
//TODO add a resolved state where you can interrogate things like All redirects headers, status code and others.
data class ResolvedUrlLocation(location: UrlLocation) {
}
data class HttpStatusException(message: String, code: Int, url: UrlLocation) : IOException(message)