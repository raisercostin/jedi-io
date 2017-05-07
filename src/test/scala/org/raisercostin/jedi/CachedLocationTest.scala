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
    val remote = Locations.url("""http://google.com/index.html""")
    val cached = remote.cached

    println("versionRemote=" + remote.version)
    println("versionCached=" + cached.version)
    //assertEquals(40, newlocation.etag.size)
    //assertTrue(newlocation.version.size > 20)
  }
}
