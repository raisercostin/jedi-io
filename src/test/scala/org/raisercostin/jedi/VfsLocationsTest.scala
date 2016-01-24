package org.raisercostin.jedi
import org.raisercostin.jedi.Locations._
import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import scala.util.Try
import java.util.regex.Pattern.Loop
import Locations._
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class VfsLocationsTest extends FunSuite {

  import org.scalatest.Matchers._
  test("vfs from zip") {
    val zip = Locations.classpath("location.zip")
    println(zip+"->"+zip.absolute)
    val vfs = Locations.vfs("zip:"+zip.absolute)
    println(vfs)
    vfs.name shouldBe ""
    println("["+vfs.file.getParent+"]")
    vfs.external.name shouldBe "location.zip"
    vfs.list.size shouldBe 3
    vfs.list.foreach(println)
    vfs.list.map(_.name) shouldBe ArrayBuffer("c","a.txt","b.txt")
  }
  test("vfs from zip inside zip") {
    //see http://stackoverflow.com/questions/9661214/uri-for-nested-zip-files-in-apaches-common-vfs
    val zip = Locations.classpath("location.zip")
    val subzip = Locations.vfs("zip:zip:"+zip.absolute+"!/c/subzip.zip!/")
    println(subzip)
    subzip.list.map(_.name) shouldBe ArrayBuffer("r","p.txt","inside.txt","q.txt")
  }
  test("vfs from zip inside zip with childs") {
    val zip = Locations.classpath("location.zip")
    val vfs = Locations.vfs("zip:"+zip.absolute)
    val subzip = vfs.child("c").child("subzip.zip").withProtocol("zip")
    println(subzip)
    subzip.list.map(_.name) shouldBe ArrayBuffer("r","p.txt","inside.txt","q.txt")
  }
  test("vfs sftp via vfs") {
    val vfs = Locations.vfs("http://stackoverflow.com/questions/9661214/uri-for-nested-zip-files-in-apaches-common-vfs")
    vfs.list.map(_.name) shouldBe ArrayBuffer("r","p.txt","inside.txt","q.txt")
  }
}
