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
    //println("inodedos="+location.attributes.dos.fileKey())
    //println("inodeposix="+location.attributes.posix.fileKey())
    println("inode="+location.attributes.inode)
    println("uniqueId="+location.uniqueId)
    println(location.attributes.basic.fileKey())
    assertNotNull(location.attributes.inode)
  }
  test("hardlinks should be detected with same uniqueId") {
    val dest = Locations.temp.randomFolderChild("test").child(location.name)
    location.copyAsHardLink(dest,true)
    val uniqueIdSrc = location.canonicalOverSymLinks
    val uniqueIdDest = dest.canonicalOverSymLinks
    assertNotNull(uniqueIdSrc)
    assertNotNull(uniqueIdDest)
    //This cannot be waranted in jdk8 on windows.
    //assertEquals(uniqueIdSrc,uniqueIdDest)
  }
  test("hardlinks on same drive") {
    val dest = Locations.temp.randomFolderChild("test").child(location.name)
    location.copyAsHardLink(dest,true)
    val dest2 = dest.parent.child(location.name).renamedIfExists
    dest.copyAsHardLink(dest2,true)
    val uniqueIdSrc = dest.canonicalOverSymLinks
    val uniqueIdDest = dest2.canonicalOverSymLinks
    assertNotNull(uniqueIdSrc)
    assertNotNull(uniqueIdDest)
    //This cannot be waranted in jdk8 on windows.
    //assertEquals(uniqueIdSrc,uniqueIdDest)
  }
}
