package org.raisercostin.jedi.impl

import scalaj.http.{HttpRequest=>HttpRequestOriginal}
import scalaj.http._
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.zip.InflaterInputStream
import java.util.zip.GZIPInputStream
import scala.collection.immutable.TreeMap
import java.net.{URL,Proxy,HttpCookie}

/** Immutable builder for creating an http request
  *
  * This is the workhorse of the scalaj-http library.
  *
  * You shouldn't need to construct this manually. Use [[scalaj.http.Http.apply]] to get an instance
  *
  * The params, headers and options methods are all additive. They will always add things to the request. If you want to 
  * replace those things completely, you can do something like {{{.copy(params=newparams)}}}
  *
  */
case class HttpRequest(
  url: String,
  method: String,
  connectFunc: (HttpRequest, HttpURLConnection) => Unit,
  params: Seq[(String,String)], 
  headers: Seq[(String,String)],
  options: Seq[HttpOptions.HttpOption],
  proxyConfig: Option[Proxy],
  charset: String,
  sendBufferSize: Int,
  urlBuilder: (HttpRequest => String),
  compress: Boolean,
  closeStreamsAtReturn: Boolean = true
) extends SlfLogger{
  def withUnclosedConnection:HttpRequest = copy(closeStreamsAtReturn = false)
  /** Add params to the GET querystring or POST form request */
  def params(p: Map[String, String]): HttpRequest = params(p.toSeq)
  /** Add params to the GET querystring or POST form request */
  def params(p: Seq[(String,String)]): HttpRequest = copy(params = params ++ p)
  /** Add params to the GET querystring or POST form request */
  def params(p: (String,String), rest: (String, String)*): HttpRequest = params(p +: rest)
  /** Add a param to the GET querystring or POST form request */
  def param(key: String, value: String): HttpRequest = params(key -> value)

  /** Add http headers to the request */
  def headers(h: Map[String, String]): HttpRequest = headers(h.toSeq)
  /** Add http headers to the request */
  def headers(h: Seq[(String,String)]): HttpRequest = copy(headers = headers ++ h)
  /** Add http headers to the request */
  def headers(h: (String,String), rest: (String, String)*): HttpRequest = headers(h +: rest)
  /** Add a http header to the request */
  def header(key: String, value: String): HttpRequest = headers(key -> value)

  /** Add Cookie header to the request */
  def cookie(name: String, value: String): HttpRequest = header("Cookie", name + "=" + value + ";")
  /** Add Cookie header to the request */
  def cookie(ck: HttpCookie): HttpRequest = cookie(ck.getName, ck.getValue)
  /** Add multiple cookies to the request. Usefull for round tripping cookies from HttpResponse.cookies */
  def cookies(cks: Seq[HttpCookie]): HttpRequest = header(
    "Cookie",
    cks.map(ck => ck.getName + "=" + ck.getValue).mkString("; ")
  )

  /** Entry point fo modifying the [[java.net.HttpURLConnection]] before the request is executed */
  def options(o: Seq[HttpOptions.HttpOption]): HttpRequest = copy(options = o ++ options)
  /** Entry point fo modifying the [[java.net.HttpURLConnection]] before the request is executed */
  def options(o: HttpOptions.HttpOption, rest: HttpOptions.HttpOption*): HttpRequest = options(o +: rest)
  /** Entry point fo modifying the [[java.net.HttpURLConnection]] before the request is executed */
  def option(o: HttpOptions.HttpOption): HttpRequest = options(o)
  
  /** Add a standard basic authorization header */
  def auth(user: String, password: String) = header("Authorization", "Basic " + HttpConstants.base64(user + ":" + password))
  
  /** OAuth v1 sign the request with the consumer token */
  def oauth(consumer: Token): HttpRequest = oauth(consumer, None, None)
  /** OAuth v1 sign the request with with both the consumer and client token */
  def oauth(consumer: Token, token: Token): HttpRequest = oauth(consumer, Some(token), None)
  /** OAuth v1 sign the request with with both the consumer and client token and a verifier*/
  def oauth(consumer: Token, token: Token, verifier: String): HttpRequest = oauth(consumer, Some(token), Some(verifier))
  /** OAuth v1 sign the request with with both the consumer and client token and a verifier*/
  def oauth(consumer: Token, token: Option[Token], verifier: Option[String]): HttpRequest = {
    //OAuth.sign(this, consumer, token, verifier)
    //TODO this should be converted
    ???
  }

  /** Change the http request method. 
    * The library will allow you to set this to whatever you want. If you want to do a POST, just use the
    * postData, postForm, or postMulti methods. If you want to setup your request as a form, data or multi request, but 
    * want to change the method type, call this method after the post method:
    *
    * {{{Http(url).postData(dataBytes).method("PUT").asString}}}
    */
  def method(m: String): HttpRequest = copy(method=m)

  /** Should HTTP compression be used
    * If true, Accept-Encoding: gzip,deflate will be sent with request.
    * If the server response with Content-Encoding: (gzip|deflate) the client will automatically handle decompression
    *
    * This is on by default
    *
    * @param c should compress
    */
  def compress(c: Boolean): HttpRequest = copy(compress=c)

  /** Send request via a standard http proxy */
  def proxy(host: String, port: Int): HttpRequest = proxy(host, port, Proxy.Type.HTTP)
  /** Send request via a proxy. You choose the type (HTTP or SOCKS) */
  def proxy(host: String, port: Int, proxyType: Proxy.Type): HttpRequest = {
    copy(proxyConfig = Some(HttpConstants.proxy(host, port, proxyType)))
  }
  /** Send request via a proxy */
  def proxy(proxy: Proxy): HttpRequest = {
    copy(proxyConfig = Some(proxy))
  }
  
  /** Change the charset used to encode the request and decode the response. UTF-8 by default */
  def charset(cs: String): HttpRequest = copy(charset = cs)

  /** The buffer size to use when sending Multipart posts */
  def sendBufferSize(numBytes: Int): HttpRequest = copy(sendBufferSize = numBytes)

  /** The socket connection and read timeouts in milliseconds. Defaults are 1000 and 5000 respectively */
  def timeout(connTimeoutMs: Int, readTimeoutMs: Int): HttpRequest = options(
    Seq(HttpOptions.connTimeout(connTimeoutMs), HttpOptions.readTimeout(readTimeoutMs))
  )
  
  /** Executes this request
    *
    * Keep in mind that if you're parsing the response to something other than String, you may hit parsing error if
    * the server responds with a different content type for error cases.
    *
    * @tparam T the type returned by the input stream parser
    * @param parser function to process the response body InputStream. Will be used for all response codes
    */
  def execute[T](
    parser: InputStream => T = (is: InputStream) => HttpConstants.readString(is, charset)
  ): HttpResponse[T] = {
    exec((code: Int, headers: Map[String, IndexedSeq[String]], is: InputStream) => parser(is))
  }

  /** Executes this request
    *
    * This is a power user method for parsing the response body. The parser function will be passed the response code,
    * response headers and the InputStream
    *
    * @tparam T the type returned by the input stream parser
    * @param parser function to process the response body InputStream
    */
  def exec[T](parser: (Int, Map[String, IndexedSeq[String]], InputStream) => T): HttpResponse[T] = {
    doConnection(parser, new URL(urlBuilder(this)), connectFunc)
  }

  private def doConnection[T](
    parser: (Int, Map[String, IndexedSeq[String]], InputStream) => T,
    urlToFetch: URL,
    connectFunc: (HttpRequest, HttpURLConnection) => Unit
  ): HttpResponse[T] = {
    proxyConfig.map(urlToFetch.openConnection).getOrElse(urlToFetch.openConnection) match {
      case conn: HttpURLConnection =>
        conn.setInstanceFollowRedirects(false)
        HttpOptions.method(method)(conn)
        if (compress) {
          conn.setRequestProperty("Accept-Encoding", "gzip,deflate")
        }
        headers.reverse.foreach{ case (name, value) => 
          conn.setRequestProperty(name, value)
        }
        options.reverse.foreach(_(conn))

        connectFunc(this, conn)
        try {
          toResponse(conn, parser, conn.getInputStream)
        } catch {
          case e: java.io.IOException if conn.getResponseCode > 0 =>
            toResponse(conn, parser, conn.getErrorStream)
        } finally {
          if (closeStreamsAtReturn)
            closeStreams(conn)
          else
            closeErrorStream(conn)
        }
    }
  }

  private def toResponse[T](
    conn: HttpURLConnection,
    parser: (Int, Map[String, IndexedSeq[String]], InputStream) => T,
    inputStream: InputStream
  ): HttpResponse[T] = {
    val responseCode: Int = conn.getResponseCode
    val headers: Map[String, IndexedSeq[String]] = getResponseHeaders(conn)
    val encoding: Option[String] = headers.get("Content-Encoding").flatMap(_.headOption)
    // HttpURLConnection won't redirect from https <-> http, so we handle manually here
    (if (conn.getInstanceFollowRedirects && (responseCode == 301 || responseCode == 302)) {
      headers.get("Location").flatMap(_.headOption).map(location => {
        doConnection(parser, new URL(location), DefaultConnectFunc)
      })
    } else None).getOrElse{
      val body: T = {
        val shouldDecompress = compress && inputStream != null
        val theStream = if (shouldDecompress && encoding.exists(_.equalsIgnoreCase("gzip"))) {
          new GZIPInputStream(inputStream)
        } else if(shouldDecompress && encoding.exists(_.equalsIgnoreCase("deflate"))) {
          new InflaterInputStream(inputStream)
        } else inputStream
        parser(responseCode, headers, theStream)
      }
      HttpResponse[T](body, responseCode, headers)
    }
  }

  private def getResponseHeaders(conn: HttpURLConnection): Map[String, IndexedSeq[String]] = {
    // There can be multiple values for the same response header key (this is common with Set-Cookie)
    // http://stackoverflow.com/questions/4371328/are-duplicate-http-response-headers-acceptable

    // according to javadoc, there can be a headerField value where the HeaderFieldKey is null
    // at the 0th row in some implementations.  In that case it's the http status line
    new TreeMap[String, IndexedSeq[String]]()(Ordering.by(_.toLowerCase)) ++ {
      Stream.from(0).map(i => i -> conn.getHeaderField(i)).takeWhile(_._2 != null).map{ case (i, value) =>
        Option(conn.getHeaderFieldKey(i)).getOrElse("Status") -> value
      }.groupBy(_._1).mapValues(_.map(_._2).toIndexedSeq)
    }
  }
  
  private def closeStreams(conn: HttpURLConnection) {
    try {
      Option(conn.getInputStream).foreach(_.close)
    } catch {
      case e: Exception => //ignore
        log.debug("When closing connection's stream to "+conn.getURL,e)
    }
    closeErrorStream(conn)
  }
  
  private def closeErrorStream(conn: HttpURLConnection) {
    try {
      Option(conn.getErrorStream).foreach(_.close)
    } catch {
      case e: Exception => //ignore
        log.debug("When closing connection's error stream to "+conn.getURL,e)
    }
  }

  /** Standard form POST request */
  def postForm: HttpRequest = postForm(Nil)

  /** Standard form POST request and set some parameters. Same as .postForm.params(params) */
  def postForm(params: Seq[(String, String)]): HttpRequest = {
    copy(method="POST", connectFunc=FormBodyConnectFunc, urlBuilder=PlainUrlFunc)
      .header("content-type", "application/x-www-form-urlencoded").params(params)
  }

  /** Raw data POST request. String bytes written out using configured charset */
  def postData(data: String): HttpRequest = body(data).method("POST")

  /** Raw byte data POST request */
  def postData(data: Array[Byte]): HttpRequest = body(data).method("POST")

  /** Raw data PUT request. String bytes written out using configured charset */
  def put(data: String): HttpRequest = body(data).method("PUT")

  /** Raw byte data PUT request */
  def put(data: Array[Byte]): HttpRequest = body(data).method("PUT")

  private def body(data: String): HttpRequest = copy(connectFunc=StringBodyConnectFunc(data))
  private def body(data: Array[Byte]): HttpRequest = copy(connectFunc=ByteBodyConnectFunc(data))

  /** Multipart POST request.
    *
    * This is probably what you want if you need to upload a mix of form data and binary data (like a photo)
    */
//  def postMulti(parts: MultiPart*): HttpRequest = {
//    copy(method="POST", connectFunc=MultiPartConnectFunc(parts), urlBuilder=PlainUrlFunc)
//  }
  
  /** Execute this request and parse http body as Array[Byte] */
  def asBytes: HttpResponse[Array[Byte]] = execute(HttpConstants.readBytes)
  /** Execute this request and parse http body as String using server charset or configured charset*/
  def asString: HttpResponse[String] = exec((code: Int, headers: Map[String, IndexedSeq[String]], is: InputStream) => {
    val reqCharset: String = headers.get("content-type").flatMap(_.headOption).flatMap(ct => {
      HttpConstants.CharsetRegex.findFirstMatchIn(ct).map(_.group(1))
    }).getOrElse(charset)
    HttpConstants.readString(is, reqCharset)
  })
  /** Execute this request and parse http body as query string key-value pairs */
  def asParams: HttpResponse[Seq[(String, String)]] = execute(HttpConstants.readParams(_, charset))
  /** Execute this request and parse http body as query string key-value pairs */
  def asParamMap: HttpResponse[Map[String, String]] = execute(HttpConstants.readParamMap(_, charset))
  /** Execute this request and parse http body as a querystring containing oauth_token and oauth_token_secret tupple */
  def asToken: HttpResponse[Token] = execute(HttpConstants.readToken)
}
case object PlainUrlFunc extends Function1[HttpRequest, String] {
  def apply(req: HttpRequest): String = req.url

  override def toString = "QueryStringUrlFunc"
}
case object DefaultConnectFunc extends Function2[HttpRequest, HttpURLConnection, Unit] {
  def apply(req: HttpRequest, conn: HttpURLConnection): Unit = {
    conn.connect
  }

  override def toString = "DefaultConnectFunc"
}
case class StringBodyConnectFunc(data: String) extends Function2[HttpRequest, HttpURLConnection, Unit] {
  def apply(req: HttpRequest, conn: HttpURLConnection): Unit = {
    conn.setDoOutput(true)
    conn.connect
    conn.getOutputStream.write(data.getBytes(req.charset))
  }

  override def toString = "StringBodyConnectFunc(" + data + ")"
}
case class ByteBodyConnectFunc(data: Array[Byte]) extends Function2[HttpRequest, HttpURLConnection, Unit] {
  def apply(req: HttpRequest, conn: HttpURLConnection): Unit = {
    conn.setDoOutput(true)
    conn.connect
    conn.getOutputStream.write(data)
  }

  override def toString = "ByteBodyConnectFunc(Array[Byte]{" + data.length + "})"
}
case object FormBodyConnectFunc extends Function2[HttpRequest, HttpURLConnection, Unit] {
  def apply(req: HttpRequest, conn: HttpURLConnection): Unit = {
    conn.setDoOutput(true)
    conn.connect
    conn.getOutputStream.write(HttpConstants.toQs(req.params, req.charset).getBytes(req.charset))
  }

  override def toString = "FormBodyConnectFunc"
}
case object QueryStringUrlFunc extends Function1[HttpRequest, String] {
  def apply(req: HttpRequest): String = {
    HttpConstants.appendQs(req.url, req.params, req.charset)
  }

  override def toString = "QueryStringUrlFunc"
}


/** Default entry point to this library */
object Http2 extends BaseHttp2

/**
  * Extends and override this class to setup your own defaults
  *
  * @param proxyConfig http proxy; defaults to the Java default proxy (see http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html).
 *              You can use [[scalaj.http.HttpConstants.proxy]] to specify an alternate proxy, or specify
 *              [[java.net.Proxy.NO_PROXY]] to explicitly use not use a proxy.
  * @param options set things like timeouts, ssl handling, redirect following
  * @param charset charset to use for encoding request and decoding response
  * @param sendBufferSize buffer size for multipart posts
  * @param userAgent User-Agent request header
  * @param compress use HTTP Compression
  */
class BaseHttp2 (
  proxyConfig: Option[Proxy] = None,
  options: Seq[HttpOptions.HttpOption] = HttpConstants.defaultOptions,
  charset: String = HttpConstants.utf8,
  sendBufferSize: Int = 4096,
  userAgent: String = "scalaj-http/1.0",
  compress: Boolean = true
) {

  /** Create a new [[scalaj.http.HttpRequest]]
   *
   * @param url the full url of the request. Querystring params can be added to a get request with the .params methods
   */
  def apply(url: String): HttpRequest = HttpRequest(
    url = url,
    method = "GET",
    connectFunc = DefaultConnectFunc,
    params = Nil,
    headers = Seq("User-Agent" -> userAgent),
    options = options,
    proxyConfig = proxyConfig,
    charset = charset,
    sendBufferSize = sendBufferSize,
    urlBuilder = QueryStringUrlFunc,
    compress = compress
  )  
}