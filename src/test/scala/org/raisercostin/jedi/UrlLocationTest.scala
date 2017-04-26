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
  test("download following redirects with a 302") {
    val url = Locations.url("""http://httpstat.us/302""")
      .withBrowserHeader
      .withoutRedirect
    assertEquals("302 Found", url.readContentAsText.get)
  }
  test("download following redirects with a 302 then a 200") {
    val url = Locations.url("""http://httpstat.us/302""")
      .withBrowserHeader
    val text = url.readContentAsText
    println(text)
    assertEquals(6979, text.get.length)
  }
//  
//  test("download following redirects with a 302 then a 404 error") {
//    val url = Locations.url("""http://vintageparadise.ro/files/produse/th_1152_0.jpeg""")
//      .withBrowserHeader
//    println(url.readContentAsText)
//    assertEquals(6979, url.readContentAsText.get.length)
//  }
}
