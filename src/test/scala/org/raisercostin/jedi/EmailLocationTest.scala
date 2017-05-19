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
import java.io.FileNotFoundException
import scala.util.Failure

@RunWith(classOf[JUnitRunner])
class EmailLocationTest extends FunSuite with BaseLocationTest {
  def location = EmailLocation("smtp.mailtrap.io",2525,"82626b3fd61334","61773d3f7714d9")
  test("email list") {
    location.list.mkString("\n") shouldBe """"""
  }
}