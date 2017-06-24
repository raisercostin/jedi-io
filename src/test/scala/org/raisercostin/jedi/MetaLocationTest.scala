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
class MetaLocationTest extends FunSuite {
  test("Meta location for a file") {
    Locations.current("""target/child/ab.txt""").mkdirOnParentIfNecessary.writeContent("ab").metaLocation.get.name shouldBe "ab.txt.meta"
  }
}
