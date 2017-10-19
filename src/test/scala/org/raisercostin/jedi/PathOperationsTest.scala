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
class PathOperationsTest extends FunSuite {
  import org.raisercostin.jedi.impl.LogTry._

  @Test def `check parent is ancestor` {
    val child = Locations.temp.randomChild("thePrefix","theSuffix")
    assertEquals(child.ancestor(child.parent),child.parent)
    assertTrue(child.childOf(child.parent))
  }
  @Test def `check grandparent is ancestor` {
    val child = Locations.temp.randomChild("thePrefix","theSuffix").child("aaa")
    assertEquals(child.ancestor(child.parent),child.parent)
    assertTrue(child.childOf(child.parent))
  }
  @Test def `check ancestor` {
    val parent = Locations.temp.randomChild("thePrefix","theSuffix")
    val child = parent.child("aaa/bbb/cccc/ddd.html")
    assertEquals(child.ancestor(parent),parent)
    assertTrue(child.childOf(parent))
  }
}
