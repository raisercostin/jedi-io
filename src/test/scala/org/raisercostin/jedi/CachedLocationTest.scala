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
  test("compute etag for temp file") {
    import CachedLocation.default
    val url = """https://commons.apache.org/proper/commons-io/javadocs/api-2.5/index.html"""
    val remote = Locations.url(url)
    remote.uri shouldBe url
    remote.raw shouldBe url
    remote.slug shouldBe "https-----commons--apache--org--proper--commons-io--javadocs--api-2.5--index.html"

    val cached = remote.cached.flush
    println("slugRemote=" + remote.slug)
    println("slugCached=" + cached.slug)
    println("versionRemote=" + remote.version)
    println("versionCached=" + cached.version)
    println(s"""remote=$remote
               |cached=$cached""".stripMargin)
    //assertEquals(40, newlocation.etag.size)
    //assertTrue(newlocation.version.size > 20)
  }
}
