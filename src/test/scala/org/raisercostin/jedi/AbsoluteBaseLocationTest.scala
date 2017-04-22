package org.raisercostin.jedi

import org.junit.runner.RunWith
import org.scalatest._
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.Matchers._

trait AbsoluteBaseLocationTest extends FunSuite {
  def location:AbsoluteBaseLocation

  test("length should be not null") {
    assertTrue(location.length > 0)
    assertTrue(location.absolute.length > 0)
  }
}