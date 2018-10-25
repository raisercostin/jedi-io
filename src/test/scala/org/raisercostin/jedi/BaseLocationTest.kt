package org.raisercostin.jedi

import org.junit.runner.RunWith
import org.scalatest._
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.Matchers._

trait BaseLocationTest extends FunSuite {
  def location: BaseLocation

  test("baseLocation should work") {
    assertEquals("index", location.baseName)
    assertEquals("index.html", location.name)
    assertEquals("html", location.extension)
    assertEquals("index", location.baseName)
  }
  test("raw should return something") {
    assertTrue(location.raw.nonEmpty)
  }
}