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
  test("compute etag for url file") {
    import CachedLocation.default
    val url = """https://commons.apache.org/proper/commons-io/javadocs/api-2.5/index.html"""
    val remote = Locations.url(url)
    remote.uri shouldBe url
    remote.raw shouldBe url
    remote.slug shouldBe "https-----commons--apache--org--proper--commons-io--javadocs--api-2.5--index.html"
    remote.etagFromHttpRequestHeader.get shouldBe "b26-531084169df69"
    remote.etag shouldBe "b26-531084169df69"    
    val cached = remote.cached.flush
    cached.baseName.should(endWith(remote.etag))
    println(s"""remote=$remote
               |cached=$cached""".stripMargin)
    //assertEquals(40, newlocation.etag.size)
    //assertTrue(newlocation.version.size > 20)
  }
  test("compute same etag for url file with forced client caching") {
    import CachedLocation.default
    val url = """https://archive.org"""
    val remote1 = Locations.url(url)
    val remote2 = Locations.url(url)
    println("etag1="+remote1.etag)
    println("etag2="+remote2.etag)
    println("version1="+remote1.version)
    println("version2="+remote2.version)
    remote1.etag shouldBe remote2.etag    
  }
}
