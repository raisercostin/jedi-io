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
class CachedLocationTest extends FunSuite {
  def newlocation = Locations.temp.child("child1.txt").writeContent("content1")
  test("compute etag for temp file") {
    println("version="+newlocation.version)
    println("etag="+newlocation.etag)
    assertEquals(40, newlocation.etag.size)
    assertTrue(newlocation.version.size>20)
  }
  test("compute etag for classpath file") {
    val location = Locations.classpath("a b.jpg").asFile
    println("version="+location.version)
    println("etag="+location.etag)
    assertEquals(40, location.etag.size)
    assertTrue(location.version.size>20)
  }
  test("compute etags"){
    assertEquals(40, newlocation.etag.size)
    assertEquals(40, newlocation.shallowETag.size)
    assertEquals(40, newlocation.strongETag.size)
    //not implemented yet
    //assertEquals(40, newlocation.deepStrongETag.size)
    //assertEquals(40, newlocation.weakETag.size)
  }
}
