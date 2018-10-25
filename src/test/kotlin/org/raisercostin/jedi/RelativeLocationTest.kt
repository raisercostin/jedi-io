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
class RelativeLocationTest extends FunSuite with BaseLocationTest /*with AbsoluteBaseLocationTest*/ {
  def location:RelativeLocation = Locations.relative("""folder/index.html""")
  test("length should be not null") {
    assertEquals("folder/index.html/one-more-child", location.child("one-more-child").relativePath)
    assertEquals("folder--index.html--one-more-child", location.child("one-more-child").relativePath("--"))
  }
//
//  test("basename, extension, name for urls") {
//    val file = Locations.url("""http://google.com/index.html""")
//    assertEquals("index.html", file.name)
//    assertEquals("index", file.baseName)
//    assertEquals("html", file.extension)
//  }
//  test("basename, extension, name for urls with parameters") {
//    val file = Locations.url("""https://fotobartesch.files.wordpress.com/2017/01/ecler-french-revolution.jpg?w=1200&h=200""")
//    assertEquals("ecler-french-revolution.jpg", file.name)
//    assertEquals("ecler-french-revolution", file.baseName)
//    assertEquals("jpg", file.extension)
//  }
//  test("basename, extension, name for urls with anchors") {
//    val file = Locations.url("""https://fotobartesch.files.wordpress.com/2017/01/ecler-french-revolution.jpg#anchor""")
//    assertEquals("ecler-french-revolution.jpg", file.name)
//    assertEquals("ecler-french-revolution", file.baseName)
//    assertEquals("jpg", file.extension)
//  }
//  test("basename, extension, name for urls with parameters and anchors") {
//    val file = Locations.url("""https://fotobartesch.files.wordpress.com/2017/01/ecler-french-revolution.jpg?w=1200#anchor""")
//    assertEquals("ecler-french-revolution.jpg", file.name)
//    assertEquals("ecler-french-revolution", file.baseName)
//    assertEquals("jpg", file.extension)
//  }
//  test("basename, extension, name for url might be missing") {
//    val file = Locations.url("""http://google.com/""")
//    assertEquals("", file.name)
//    assertEquals("", file.extension)
//    assertEquals("", file.baseName)
//  }
//  test("basename, extension, name for url might be missing without path at end") {
//    val file = Locations.url("""http://google.com""")
//    assertEquals("", file.name)
//    assertEquals("", file.extension)
//    assertEquals("", file.baseName)
//  }
//  test("copy from url") {
//    val file = Locations.url("""http://google.com/""")
//    val dest = Locations.temp.child("dest")
//    val dest2 = dest.renamedIfExists.copyFrom(file)
//    assertEquals("<!doctype", dest2.readContentAsText.get.take(9))
//  }
//  test("copy to url") {
//    val file = Locations.url("""http://google.com/""")
//    val dest = Locations.temp.child("dest")
//    val dest2 = file.copyTo(dest.renamedIfExists)
//    assertEquals("<!doctype", dest2.readContentAsText.get.take(9))
//  }
//  test("copy from url then delete without problems since the stream should be closed") {
//    val file = Locations.url("""http://google.com/""")
//    val dest = Locations.temp.child("dest")
//    val dest2 = dest.renamedIfExists.copyFrom(file)
//    assertEquals("<!doctype", dest2.readContentAsText.get.take(9))
//    val dest3 = Locations.file(dest2.absolute)
//    dest3.delete
//  }
//  test("download with special agent") {
//    //"Server returned HTTP response code: 403 for URL: http://www.restograf.ro/wp-content/uploads/2015/08/french.jpg"
//    val url = Locations.url("""http://www.restograf.ro/wp-content/uploads/2015/08/french.jpg""").
//      withAgent("User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
//    assertEquals(45418, url.readContentAsText.get.length)
//  }
}
