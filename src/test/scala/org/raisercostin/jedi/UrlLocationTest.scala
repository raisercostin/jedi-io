package org.raisercostin.jedi
import org.raisercostin.jedi.Locations._
import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner

import scala.util.Try

import java.util.regex.Pattern.Loop

import Locations._
import org.scalatest.Matchers._
import java.io.FileNotFoundException
import scala.util.Failure
import scala.collection.immutable.SortedMap

@RunWith(classOf[JUnitRunner])
class UrlLocationTest extends FunSuite with BaseLocationTest {

  def location = Locations.url("""http://google.com/index.html""")
  test("length should be not null") {
    assertEquals(346622, Locations.classpath("""a b.jpg""").length)
    assertEquals(346622, Locations.classpath("""a b.jpg""").asUrl.length)
  }

  test("basename, extension, name for urls") {
    val file = Locations.url("""http://google.com/index.html""")
    assertEquals("index.html", file.name)
    assertEquals("index", file.baseName)
    assertEquals("html", file.extension)
  }
  test("basename, extension, name for urls with parameters") {
    val file = Locations.url("""https://fotobartesch.files.wordpress.com/2017/01/ecler-french-revolution.jpg?w=1200&h=200""")
    assertEquals("ecler-french-revolution.jpg", file.name)
    assertEquals("ecler-french-revolution", file.baseName)
    assertEquals("jpg", file.extension)
  }
  test("basename, extension, name for urls with anchors") {
    val file = Locations.url("""https://fotobartesch.files.wordpress.com/2017/01/ecler-french-revolution.jpg#anchor""")
    assertEquals("ecler-french-revolution.jpg", file.name)
    assertEquals("ecler-french-revolution", file.baseName)
    assertEquals("jpg", file.extension)
  }
  test("basename, extension, name for urls with parameters and anchors") {
    val file = Locations.url("""https://fotobartesch.files.wordpress.com/2017/01/ecler-french-revolution.jpg?w=1200#anchor""")
    assertEquals("ecler-french-revolution.jpg", file.name)
    assertEquals("ecler-french-revolution", file.baseName)
    assertEquals("jpg", file.extension)
  }
  test("basename, extension, name for url might be missing") {
    val file = Locations.url("""http://google.com/""")
    assertEquals("", file.name)
    assertEquals("", file.extension)
    assertEquals("", file.baseName)
  }
  test("basename, extension, name for url might be missing without path at end") {
    val file = Locations.url("""http://google.com""")
    assertEquals("", file.name)
    assertEquals("", file.extension)
    assertEquals("", file.baseName)
  }
  test("copy from url") {
    val file = Locations.url("""http://google.com/""")
    val dest = Locations.temp.child("dest")
    val dest2 = dest.renamedIfExists.copyFrom(file)
    assertEquals("<!doctype", dest2.readContentAsText.get.take(9))
  }
  test("copy to url") {
    val file = Locations.url("""http://google.com/""")
    val dest = Locations.temp.child("dest")
    val dest2 = file.copyTo(dest.renamedIfExists)
    assertEquals("<!doctype", dest2.readContentAsText.get.take(9))
  }
  test("copy from url then delete without problems since the stream should be closed") {
    val file = Locations.url("""http://google.com/""")
    val dest = Locations.temp.child("dest")
    val dest2 = dest.renamedIfExists.copyFrom(file)
    assertEquals("<!doctype", dest2.readContentAsText.get.take(9))
    val dest3 = Locations.file(dest2.absolute)
    dest3.delete
  }
  test("download with special agent") {
    //"Server returned HTTP response code: 403 for URL: http://www.restograf.ro/wp-content/uploads/2015/08/french.jpg"
    val url = Locations.url("""http://www.restograf.ro/wp-content/uploads/2015/08/french.jpg""").
      withAgent("User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
    assertEquals(45418, url.readContentAsText.get.length)
  }
  //TODO start a server with a redirect http://doc.akka.io/docs/akka-http/current/scala/http/introduction.html#using-akka-http
  test("download following redirects") {
    val url = Locations.url("""http://www.altshop.ro/poze_produse/83096/mari/televizor-led-philips-32phh4309-88-seria-phh4309-81cm-negru-hd-ready_0.jpg""").
      withAgent("User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
    assertEquals(35127, url.readContentAsText.get.length)
  }
  test("download with a 302 no redirects") {
    val url = Locations.url("""http://httpstat.us/302""")
      .withBrowserHeader
      .withoutRedirect
    val thrown: HttpStatusException = url.readContentAsText.failed.map { case e: HttpStatusException => e }.get
    assert(thrown.code === 302)
  }
  test("download following redirects with a 302 then a 200") {
    val url = Locations.url("""http://httpstat.us/302""")
      .withBrowserHeader
    val text = url.readContentAsText
    text.get.length should be > (5000)
    val text2 = url.withJavaImpl.readContentAsText
    text.get.length should be > (5000)
    assert(text.get === text2.get)
  }
  test("slow connection") {
    val url = Locations.url("""http://vintageparadise.ro/files/produse/th_1682_0.jpeg""")
    val resp = url.withoutAgent.readContentAsText.failed.map { case e: HttpStatusException => e }.get
    assert(resp.code === 403)
  }
  test("download following redirects with a 302 then a 404 error") {
    val url = Locations.url("""http://vintageparadise.ro/files/produse/th_1682_0.jpeg""")
    val resp = url.withBrowserHeader.readContentAsText.failed.map { case e: HttpStatusException => e }.get
    //resp.printStackTrace()
    assert(resp.code === 404)
  }
  test("test multiple urls") {
    val res = """https://www.altshop.ro/poze_produse/52984/mari/tetier%C4%83-7-inch-pni-hm700a-b-negru-cu-fermoar_0.jpg""".stripMargin.lines.map(urlString => {
      val url = Locations.url(urlString)
      (url, url.readContentAsText.map(_.length))
    }).map {
      case (url, res) =>
        println(s"\n*******************\n\n$url\n\n" + res)
        res
    }.collect { case Failure(e) => e }
    assert(res.size === 0, "No error should be thrown here.")
  }
  test("compute etag for url file") {
    val url = """https://commons.apache.org/proper/commons-io/javadocs/api-2.5/index.html"""
    val remote = Locations.url(url)
    remote.uri shouldBe url
    remote.raw shouldBe url
    remote.slug shouldBe "https-----commons--apache--org--proper--commons-io--javadocs--api-2.5--index.html"
    remote.etagFromHttpRequestHeader.get shouldBe "b26-531084169df69"
    remote.etag shouldBe "b26-531084169df69"
  }
  test("copy url with meta") {
    val url = """https://commons.apache.org/proper/commons-io/javadocs/api-2.5/index.html"""
    println(Locations.url(url).meta.toString)
    val src = Locations.url(url)
    val dest = Locations.current("target/copy/").backupExistingOne.mkdirIfNecessary.copyFrom(src)
    println(src.meta.get.request.toSortedMap.mkString("\n"))
    //println(dest.meta.get.request.toSortedMap.mkString("\n"))
    //meta.get.request.-("Cache-Control").-("Pragma").toSortedMap.
  }
  test("a url should always have a pair meta file") {
    val url = """https://commons.apache.org/proper/commons-io/javadocs/api-2.5/index.html"""
    println(Locations.url(url).meta.toString)
    Locations.url(url).meta.get.request.-("Cache-Control").-("Pragma").toSortedMap.
      mkString("\n") shouldBe
      """ |Accept -> Buffer(*/*)
        |Connection -> Buffer(keep-alive)
        |HEAD /proper/commons-io/javadocs/api-2.5/index.html HTTP/1.1 -> Buffer(null)
        |Host -> Buffer(commons.apache.org)
        |User-Agent -> Buffer(Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36)""".stripMargin
    Locations.url(url).meta.get.response.-(null).toSortedMap.updated("Date", "--deleted--").updated("Keep-Alive", "Buffer(timeout=30, max=98)--manuallyChangedInTest").
      mkString("\n") shouldBe
      //|null -> Buffer(HTTP/1.1 200 OK)
      """|Accept-Ranges -> Buffer(bytes)
      |Connection -> Buffer(Keep-Alive)
      |Content-Length -> Buffer(2854)
      |Content-Type -> Buffer(text/html)
      |Date -> --deleted--
      |ETag -> Buffer("b26-531084169df69")
      |Keep-Alive -> Buffer(timeout=30, max=98)--manuallyChangedInTest
      |Last-Modified -> Buffer(Fri, 22 Apr 2016 00:53:30 GMT)
      |Server -> Buffer(Apache/2.4.7 (Ubuntu))
      |Vary -> Buffer(Accept-Encoding)""".stripMargin
  }

  implicit class ToSortedMap[A, B](tuples: TraversableOnce[(A, B)])(implicit ordering: Ordering[A]) {
    def toSortedMap =
      SortedMap(tuples.toSeq: _*)
  }
}
