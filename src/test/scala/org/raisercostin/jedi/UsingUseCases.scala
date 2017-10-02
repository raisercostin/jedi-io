package org.raisercostin.jedi
import org.raisercostin.jedi.Locations._
import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._

import scala.util.Try

import java.util.regex.Pattern.Loop

import Locations._
import org.scalatest.Matchers._
import java.nio.file.Files
import java.nio.file.NotLinkException
import java.io.FileNotFoundException
import org.scalatest.words.ContainWord
import org.junit.Test

//@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class UsingUseCases extends FunSuite {
  import org.raisercostin.jedi.impl.LogTry._

  @Test def usingTest {
    val content = Locations.memory("a").usingPrintWriterAndContinue(new RuntimeException("myException").printStackTrace).readContent
    assertTrue(content.contains("myException"))
  }
}
