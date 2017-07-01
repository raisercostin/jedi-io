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
import java.nio.file.Files
import java.nio.file.NotLinkException
import java.io.FileNotFoundException
import org.scalatest.words.ContainWord

@RunWith(classOf[JUnitRunner])
class FileLocationTest extends FunSuite with AbsoluteBaseLocationTest {
  import org.raisercostin.jedi.impl.LogTry._

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
    println("inode=" + location.attributes.inode)
    println("uniqueId=" + location.uniqueId)
    println(location.attributes.basic.fileKey())
    assertNotNull(location.attributes.inode)
  }
  test("hardlinks should be detected with same uniqueId") {
    val dest = Locations.temp.randomFolderChild("test").child(location.name)
    location.copyAsHardLink(dest, true)
    val uniqueIdSrc = location.canonicalOverSymLinks
    val uniqueIdDest = dest.canonicalOverSymLinks
    assertNotNull(uniqueIdSrc)
    assertNotNull(uniqueIdDest)
    //This cannot be waranted in jdk8 on windows.
    //assertEquals(uniqueIdSrc,uniqueIdDest)
  }
  test("hardlinks on same drive") {
    val dest = Locations.temp.randomFolderChild("test").child(location.name)
    location.copyAsHardLink(dest, true)
    val dest2 = dest.parent.child(location.name).renamedIfExists
    dest.copyAsHardLink(dest2, true)
    val uniqueIdSrc = dest.canonicalOverSymLinks
    val uniqueIdDest = dest2.canonicalOverSymLinks
    assertNotNull(uniqueIdSrc)
    assertNotNull(uniqueIdDest)
    //This cannot be waranted in jdk8 on windows.
    //assertEquals(uniqueIdSrc,uniqueIdDest)
  }
  //d:\personal\work2\docs-proces>mklink /D docs-process-symlink docs-process
  //symbolic link created for docs-process-symlink <<===>> docs-process
  test("file symlink on windows") {
    assume(Locations.environment.isWindows)
    val simlink = Locations.current("target/a-b-symlink.jpg").backupExistingOne.copyFromAsSymLinkAndGet(Locations.classpath("""a b.jpg""").asFile)
    println(simlink.attributes.toMap.mkString("\n"))
    println(simlink.size)
    simlink.isFile shouldBe true
    simlink.isFolder shouldBe false
    simlink.name shouldBe "a-b-symlink.jpg"
    simlink.isSymlink shouldBe true
    simlink.symlink.get.name shouldBe "a b.jpg"
  }
  test("folder symlink on windows") {
    assume(Locations.environment.isWindows)
    val simlink = Locations.current("target/a-b-symlink").backupExistingOne.copyFromAsSymLinkAndGet(Locations.classpath("""folder/a b.jpg""").asFile.parent)
    println(simlink.attributes.toMap.mkString("\n"))
    simlink.isFile shouldBe false
    simlink.isFolder shouldBe true
    simlink.name shouldBe "a-b-symlink"
    simlink.isSymlink shouldBe true
    simlink.symlink.get.name shouldBe "folder"
  }
  test("file symlink to folder should not work on windows") {
    //cannot do that since the decision on what kind of link is created depends on passed source
  }
  test("backupExisting symlink should work") {
    val symlink = Locations.current("target/invalid-a-b-symlink").backupExistingOne
    symlink.existsWithoutResolving shouldBe false
    val symlink2 = symlink.copyFromAsSymLink(Locations.classpath("""folder/a b.jpg""").asFile).get
    symlink2.existsWithoutResolving shouldBe true
    val symlink3 = symlink2.backupExistingOne
    symlink3.existsWithoutResolving shouldBe false
  }
  test("folder symlink to invalid path on windows") {
    //assume(Locations.environment.isWindows)
    val symlink = Locations.current("target/invalid-a-b-symlink").backupExistingOne.copyFromAsSymLinkAndGet(Locations.classpath("""folder/a b.jpg""").asFile.parent.child("folder2"))
    symlink.isFile shouldBe false
    symlink.isFolder shouldBe false
    symlink.exists shouldBe false
    symlink.name shouldBe "invalid-a-b-symlink"
    symlink.isSymlink shouldBe true
    symlink.symlink.get.name shouldBe "folder2"
  }
  test("symlink on file shouldn't work") {
    intercept[NotLinkException] {
      Locations.current("target").symlink.log.get
    }
  }
  test("throw an exception when create a symnlink under a non existing parent") {
    Locations.current("target/a").backupExistingOne.mkdirIfNecessary.child("b-symlink").copyFromAsSymLinkAndGet(Locations.classpath("""folder/a b.jpg""").asFile)
    intercept[RuntimeException] {
      Locations.current("target/a").backupExistingOne.child("b-symlink").copyFromAsSymLink(Locations.classpath("""folder/a b.jpg""").asFile).log.get
    }.getMessage should include(Locations.current("target/a").name)
  }
  test("cannot get an unsafeOutputStream from a folder") {
    val ex = intercept[RuntimeException] {
      val folder: FileLocation = Locations.current("target")
      folder.isFolder shouldBe true
      val is = folder.unsafeToOutputStream
    }
    ex.getMessage.should(include("folder"))
  }
  test("copy a file to a folder") {
    Locations.current("target").child("test14").mkdirIfNecessary.copyFromWithoutMetadata(Locations.classpath("a b.jpg")).name shouldBe "a b.jpg"
  }
  test("detect parent ancestor") {
    val parent = location
    parent.child("a/b/cdef").ancestor(parent.child("a/xyz")).name shouldBe "a"
    parent.child("a/b/cdef").ancestor(parent.child("a/b/cdxy")).name shouldBe "b"
  }
  test("copy a file with forced metadata") {
    val ex = intercept[IllegalArgumentException]{Locations.current("target").child("test14").mkdirIfNecessary.copyFrom(Locations.classpath("a b.jpg"))(CopyOptions.copyWithMetadata)}
    ex.getMessage.should(include(".meta"))
  }
  test("copy a file with optional metadata") {
    Locations.current("target").child("test14").mkdirIfNecessary.copyFrom(Locations.classpath("a b.jpg"))(CopyOptions.copyWithOptionalMetadata)
  }
}
