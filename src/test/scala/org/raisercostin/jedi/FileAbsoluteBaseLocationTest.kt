package org.raisercostin.jedi

import org.junit.runner.RunWith
import org.scalatest._
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.junit.Test

trait FileAbsoluteBaseLocationTest{
  def location: FileAbsoluteBaseLocation

  @Test def lengthShouldBeNotNull {
    assertTrue(location.length > 0)
    assertTrue(location.absolute.length > 0)
  }
}