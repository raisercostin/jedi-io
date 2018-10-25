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
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4]) //needed to see it as a junit4 in eclipse
class ClasspathLocationTest extends FunSuite with NavigableInputLocationReusableTest {
  override val location = Locations.classpath("folder/a b.jpg")

  @Test def `classpath location` {
    val res = Locations.classpath("folder/a b.jpg")
    res.outerPrefix shouldBe res.absolute.stripSuffix("folder/a b.jpg")
    res.innerPath shouldBe "folder/a b.jpg"
  }
  @Test def `list in classpath` {
    val list = Locations.classpath("folder/a b.jpg").parent.list.toIndexedSeq
    list.size shouldBe 1
    list(0).initialResourcePath shouldBe "folder/a b.jpg"
  }
  @Test def `descendents in classpath` {
    val list = Locations.classpath("folder/a b.jpg").parent.parent.descendants.filter(_.extension == "jpg").toIndexedSeq
    list foreach println
    list.size shouldBe 2
    list(0).initialResourcePath shouldBe "a b.jpg"
    list(1).initialResourcePath shouldBe "folder/a b.jpg"
  }
  @Test def detectExistingFolder {
    Locations.classpath("folder/a b.jpg").exists shouldBe true
    Locations.classpath("folder/a b.jpg").isFile shouldBe true
    Locations.classpath("folder/a b.jpg").isFolder shouldBe false
    Locations.classpath("folder/a b.jpg/").exists shouldBe true
    println(Locations.classpath("folder/a b.jpg/").list.head.name)
    Locations.classpath("folder/a b.jpg/").isFile shouldBe true
    Locations.classpath("folder/a b.jpg/").isFolder shouldBe false
    Locations.classpath("folder/").exists shouldBe true
    Locations.classpath("folder").exists shouldBe true
  }
  @Test def detectIsFileAndFolderIfExists {
    Locations.classpath("folder/a b.jpg").exists shouldBe true
    Locations.classpath("folder/a b.jpg").isFolder shouldBe false
    Locations.classpath("folder/a b.jpg").isFile shouldBe true

    Locations.classpath("folder/").exists shouldBe true
    Locations.classpath("folder/").list foreach println
    Locations.classpath("folder/").isFile shouldBe false
    Locations.classpath("folder/").isFolder shouldBe true
  }
  @Test def detectIsFileAndFolderIfNotExists {
    Locations.classpath("folder").exists shouldBe true
    Locations.classpath("folder").isFile shouldBe false
    Locations.classpath("folder").isFolder shouldBe true

    //    Locations.classpath("folder").parent.child("folder-inexistent").exists shouldBe false
    //    Locations.classpath("folder-inexistent").exists shouldBe false
    //    Locations.classpath("folder-inexistent").isFile shouldBe false
    //    Locations.classpath("folder-inexistent").isFolder shouldBe false
    //
    //    Locations.classpath("file-inexistent.jpg").exists shouldBe false
    //    Locations.classpath("file-inexistent.jpg").isFile shouldBe false
    //    Locations.classpath("file-inexistent.jpg").isFolder shouldBe false
    //
    //    Locations.classpath("folder-inexistent/a b.jpg").exists shouldBe false
    //    Locations.classpath("folder-inexistent/a b.jpg").isFile shouldBe false
    //    Locations.classpath("folder-inexistent/a b.jpg").isFolder shouldBe false
  }
  @Test def `copy all from classpath` {
    val list = Locations.classpath("folder/a b.jpg").parent.list.toIndexedSeq
    //    list.foreach(println(_))
    //    Locations.current("target").child("test14").mkdirIfNecessary.copyFrom(Locations.classpath("folder/a b.jpg").parent).name shouldBe "a b.jpg"
  }
  @Test override def testCopyFolderToFolder {
    copyFolderToFolderTest(location)
  }
  @Test override def testDetectParentAncestor = testDetectParentAncestorTest(Locations.classpath("org"))
}
