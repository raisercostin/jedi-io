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
    assertEquals("index", file.baseName)
    assertEquals("index.html", file.name)
    assertEquals("html", file.extension)
    assertEquals("index", file.baseName)
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
}
