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
import org.junit.Test

trait NavigableInputLocationReusableTest{
  def location:NavigableInputLocation
  @Test def testDetectParentAncestor = testDetectParentAncestorTest(location)
  def testDetectParentAncestorTest(location:NavigableInputLocation) {
    location.child("junit/runner").ancestor(location.child("junit/rules")).name shouldBe "junit"
    location.child("junit/runner/notification").ancestor(location.child("junit/runner/manipulation")).name shouldBe "runner"    
  }
  @Test def testCopyFileToFolder {
    Locations.current("target").child("test14").backupExistingOne.mkdirIfNecessary.copyFrom(location).list.filter(_.name=="a b.jpg").toIndexedSeq(0).name shouldBe "a b.jpg"
  }
  @Test def testCopyFolderToFolder {
    copyFolderToFolderTest(location)
  }
  def copyFolderToFolderTest(location:NavigableInputLocation) {
    location.parent.list foreach println
    Locations.current("target").child("test15").backupExistingOne.mkdirIfNecessary.copyFrom(location.parent).list.filter(_.name=="a b.jpg").toIndexedSeq(0).name shouldBe "a b.jpg"
  }
}

class NavigableInputLocationTest extends FunSuite {
  import org.raisercostin.jedi.impl.LogTry._

  //d:\personal\work2\docs-proces>mklink /D docs-process-symlink docs-process
  //symbolic link created for docs-process-symlink <<===>> docs-process
  @Test def `file symlink on windows` {
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
  @Test def `folder symlink on windows` {
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
  test("copy a file with forced metadata") {
    val ex = intercept[IllegalArgumentException]{Locations.current("target").child("test14").mkdirIfNecessary.copyFrom(Locations.classpath("a b.jpg"))(CopyOptions.copyWithMetadata)}
    ex.getMessage.should(include(".meta"))
  }
  @Test def `copy a file with optional metadata` {
    Locations.current("target").child("test14").mkdirIfNecessary.copyFrom(Locations.classpath("a b.jpg"))(CopyOptions.copyWithOptionalMetadata)
  }
}
