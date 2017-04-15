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
    assertEquals(346622,Locations.classpath("""a b.jpg""").length)
    assertEquals(346622,Locations.classpath("""a b.jpg""").asUrl.length)
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
}
