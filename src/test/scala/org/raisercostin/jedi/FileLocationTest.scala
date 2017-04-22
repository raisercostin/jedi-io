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
class FileLocationTest extends FunSuite with AbsoluteBaseLocationTest {
  override def location: FileLocation = Locations.classpath("""a b.jpg""").asFile

  test("basic attributes") {
    println(location.attributes.toMap.mkString("\n"))
    assertNotNull(location.attributes.basic.lastModifiedTime())
  }
  test("owner attributes") {
    assertNotNull(location.attributes.owner.getOwner())
  }
  test("inode on linux sistems") {
    println("inode="+location.attributes.inode)
    println(location.attributes.basic.fileKey().toString())
    assertNotNull(location.attributes.inode)
  }
}
