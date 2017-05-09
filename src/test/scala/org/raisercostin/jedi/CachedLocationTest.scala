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
    implicit val cache: CacheConfig = EtagCacheConfig(Locations.temp.child("default-cache"))
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
  test("compute slug") {
    Locations.url("https://archive.org").slug shouldBe "https-----archive--org"
    Locations.url("https://commons.archive.org/a.b/c.d").slug shouldBe "https-----commons--archive--org--a.b--c.d"
  }
  test("compute same etag for url file with forced client caching") {
    implicit val cache: CacheConfig = TimeSensitiveCachedEntry(Locations.temp.child("default-cache"))
    val url = """https://archive.org"""
    val remote1 = Locations.url(url).cached.flush
    val remote2 = Locations.url(url).cached.flush
    remote1.cache shouldBe remote2.cache
    println("url1=" + remote1.cache)
    println("url2=" + remote2.cache)
    println("url1=" + remote1)
    println("url2=" + remote2)
    println("etag1=" + remote1.etag)
    println("etag2=" + remote2.etag)
    println("version1=" + remote1.version)
    println("version2=" + remote2.version)
    remote1.etag shouldBe remote2.etag
  }
  test("compute with etag and date") {
    implicit val cache: CacheConfig = TimeSensitiveEtagCachedEntry(Locations.temp.child("default-cache"))
    Locations.url("""https://archive.org""").cached.cache.absolute should (include("date") and not include("etag"))
    Locations.url("""https://commons.apache.org""").cached.cache.absolute should (include("etag") and not include("date"))
  }
}
