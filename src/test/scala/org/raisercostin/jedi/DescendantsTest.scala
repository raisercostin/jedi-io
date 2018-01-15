package org.raisercostin.jedi

//import org.scalatest._
//import org.junit.Assert._
//import Locations._
import org.scalatest.Matchers._
import org.junit.runners.JUnit4
import org.junit.runner.RunWith
import org.junit.Test
import org.scalatest._

//google: insolventa inurl:docx
@RunWith(classOf[JUnit4])
class DescendantsTest extends FunSuite {
  @Test
  def testAllListedChildrenAreAlsoDescendants() {
    val children = Locations.file("src").list.toSeq
    val all = Locations.file("src").descendants.toSeq
    all.size shouldBe 77
    children.size shouldBe 2
    
    val good = children.map(c => (c->all.contains(c))).filterNot(_._2)
    println(good.mkString("\n"))
    good.size shouldBe 0
    //println(children.mkString("\n"))
    //println(all.mkString("\n"))
  }
}