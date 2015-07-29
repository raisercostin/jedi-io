package org.raisercostin.jedi
import org.raisercostin.jedi.Locations._
import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner

import scala.util.Try

import java.util.regex.Pattern.Loop

import Locations._

@RunWith(classOf[JUnitRunner])
class LocationsTest extends FunSuite {

  import org.scalatest.Matchers._
  test("bug - test spaces in classpath filename") {
    val file = Locations.classpath("a b.jpg")
    assertEquals("a b.jpg", file.resourcePath)
    val last = "/a%20b.jpg".length
    assertEquals("/a%20b.jpg", file.toUrl.toString().takeRight(last))
    assertEquals("/a%20b.jpg", file.toUrl.toExternalForm().takeRight(last))
    assertEquals("/a%20b.jpg", file.toUrl.toURI().toString().takeRight(last))
    assertEquals("/a%20b.jpg", file.toUrl.toURI().toURL().toString().takeRight(last))
    assertEquals("/a%20b.jpg", file.toUrl.toURI().toASCIIString().takeRight(last))
    assertEquals("a%20b.jpg", new java.io.File(file.toUrl.getFile()).getName())
    assertEquals("a b.jpg", new java.io.File(file.toUrl.toURI()).getName())
    assertEquals("a b.jpg", file.toFile.getName())
    assertEquals("a b.jpg", file.absolute.takeRight("a b.jpg".length))
    assertEquals("a b.jpg", file.name)
  }

  test("unzip") {
    //Locations.classpath("location.zip").unzip.list.toSeq.foreach(x => pri ntln(x.name))
    assertEquals("""ZipInputLocation(ClassPathInputLocation(location.zip),Some(a.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(b.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/d.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/f.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/subzip.zip))""".replaceAll("\r", ""),
      Locations.classpath("location.zip").unzip.list.toSeq.sortBy(_.name).mkString("\n"))
    assertEquals("""a - file content""", Locations.classpath("location.zip").unzip.child("a.txt").readContent)
    assertEquals("""f content""", Locations.classpath("location.zip").unzip.child("c/e/f.txt").readContent)
    assertEquals("""ZipInputLocation[ClassPathInputLocation(location.zip),Some(c)]""", Locations.classpath("location.zip").unzip.child("c").raw)
    assertEquals("""ZipInputLocation[ClassPathInputLocation(location.zip),Some(c/e)]""", Locations.classpath("location.zip").unzip.child("c").child("e").raw)
    assertEquals("""ZipInputLocation[ClassPathInputLocation(location.zip),Some(c/e/f.txt)]""", Locations.classpath("location.zip").unzip.child("c").child("e").child("f.txt").raw)
    assertEquals("""f content""", Locations.classpath("location.zip").unzip.child("c").child("e").child("f.txt").readContent)
  }

  test("test relativeTo") {
    val base = Locations.temp.randomChild()
    val dest = base.child("""photos2-proposed1-good""")
    val from = base.child("""photos2""")
    val src = base.child("""photos2/1409153946085.jpg""")
    val baseName = "2014-08-27--18-39-03--------1409153946085.jpg"
    assertEquals(base.absolute + """/photos2/1409153946085.jpg""", src.absolute)
    assertEquals(base.absolute + """/photos2""", from.absolute)
    //assertEquals("""\1409153946085.jpg""",src.diff(src.absolute,from.absolute).get)
    //assertEquals("""1409153946085.jpg""",src.extractAncestor(from).get)
    assertEquals("""1409153946085.jpg""", src.extractPrefix(from).get.relativePath)
    val destFile = src.extractPrefix(from).map(dest.child).get.withName(_ => baseName)
    assertEquals(base.absolute + """/photos2-proposed1-good/2014-08-27--18-39-03--------1409153946085.jpg""", destFile.absolute)
  }
  test("copy from classpath") {
    val file = Locations.classpath("a b.jpg")
    val dest = Locations.memory("mem1")
    file.copyTo(dest)
    assertEquals(file.length, dest.length)

    val dest2 = Locations.memory("mem1")
    dest2.copyFrom(file)
    assertEquals(file.length, dest2.length)
  }
  test("copy from internet") {
    val file = Locations.url(new java.net.URL("http://google.com"))
    val dest = Locations.temp.randomChild("google.html")
    file.copyTo(dest)
    assertTrue(dest.length >= 0)
  }
  test("unzip and list location.zip") {
    assertEquals("""ZipInputLocation(ClassPathInputLocation(location.zip),Some(a.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(b.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/d.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/f.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/subzip.zip))""".replaceAll("\r", ""),
      Locations.classpath("location.zip").unzip.list.toSeq.sortBy(_.name).mkString("\n"))
  }
  test("unzip and list location.zip/c/subzip.zip") {
    assertEquals("""inside.txt
p.txt
q.txt
r/
r/s/
r/s/v.txt
r/u.txt""".replaceAll("\r", ""),
      Locations.classpath("location.zip").unzip.child("c/subzip.zip").unzip.list.toSeq.sortBy(_.name).map { _.name }.mkString("\n"))
  }
  test("parent of relative file") {

    import java.io.File
    assertTrue(new java.io.File(".").getCanonicalPath.length > "src".length)
    assertTrue(new File("src").getAbsolutePath().length > "src".length)
    assertNotNull(Locations.file("src").toPath.getParent())
    assertNotNull(Locations.file("src").toFile)
    assertNotNull(Locations.file("src").toFile.toString)
    assertNotNull(Locations.file("src").toPath)
    assertNotNull(Locations.file("src").toFile.getParentFile)
    assertNotNull(Locations.file("src").parentName)
    assertNotNull(Locations.file("src").parent)
  }
  test("parent of relative location") {
    assertEquals("2013", Locations.relative("""2013/2013-05-01 - trip to Monschau""").parentName)
    assertEquals("""2013/some space/second space""", Locations.relative("""2013/2013-05-01 - trip to Monschau""").
        parent.child("some space").child("second space").relativePath)
    assertEquals("""2013""", Locations.file(".").child(Locations.relative("""2013/2013-05-01 - trip to Monschau""")).parent.name)
  }
  test("bug with trailing child fiels") {
    Try { Locations.file(""".""").child("2013-05-01 - trip to ") }.toString should
      startWith("Failure(java.lang.IllegalArgumentException: requirement failed: Child [2013-05-01 - trip to ] has trailing spaces)")
  }
  test("current folder") {
    Locations.current("target").toString should not include ("./target")
  }
  test("relative location should not start with file separator") {
    Try { Locations.relative("/folder") }.toString should include("shouldn't start with file separator")
  }
  test("relative location parent") {
    Locations.relative("").child("a").raw.toString should equal("a")
    Locations.relative("folder").parentName.toString should equal("")
    Locations.relative("folder").parent.raw.toString should equal("")
    Locations.relative("folder").child("aaaa").parentName.toString should equal("folder")
    Locations.relative("folder").child("aaaa").parent.raw.toString should equal("folder")
  }
  test("relative computation") {
    assertEquals("", Locations.relative("path").extractPrefix(Locations.relative("path")).get.relativePath)
    assertEquals("", Locations.relative("img.jpg").extractPrefix(Locations.relative("img.jpg")).get.relativePath)
    assertEquals("b/img.jpg", Locations.relative("a/b/img.jpg").extractPrefix(Locations.relative("a")).get.relativePath)
    assertEquals("img.jpg", Locations.relative("a/b/img.jpg").extractPrefix(Locations.relative("a/b")).get.relativePath)
    assertEquals("b/img.jpg", Locations.relative("a\\b\\img.jpg").extractPrefix(Locations.relative("a")).get.relativePath)
  }

  import scala.io.Codec

  import java.nio.charset.Charset
  import java.nio.charset.CodingErrorAction
  import java.nio.charset.MalformedInputException

  //bug
  ignore("get lines from utf-8 file with BOM") {
    val result = Locations.classpath("/fileWithBom.txt").readContent
    assertEquals(66, result.size)
  }
  test("get lines from utf-8 file with BOM - throw exceptions") {
    val is = getClass.getResourceAsStream("/fileWithBom.txt")
    intercept[MalformedInputException] {
      val result = scala.io.Source.fromInputStream(is)(Codec("UTF-8")).getLines.exists(_ => false)
    }
    val result = Locations.classpath("/fileWithBom2.txt").readContent
    assertEquals(17, result.size)
  }
  test("read from resources in other jars") {
    //p rintln(com.gravity.goose.text.HashUtils.getClass().getClassLoader())
    //p rintln(Locations.getClass.getClassLoader)
    val s1 = Locations.stream(Locations.getClass.getClassLoader.getResourceAsStream("META-INF/maven/org.slf4j/slf4j-api/pom.properties")).readContentAsText.get
    val text = Locations.classpath("META-INF/maven/org.slf4j/slf4j-api/pom.properties").readContentAsText.get
    assertEquals(s1, text)
  }
  test("read from resources in local classpath") {
    val s2 = Locations.stream(Locations.getClass.getClassLoader.getResourceAsStream("fileWithBom.txt")).readContentAsText.get
    val text2 = Locations.classpath("fileWithBom.txt").readContentAsText.get
    assertEquals(s2, text2)
  }
  test("get lines from utf-8 file with BOM - low level implementation") {
    import org.apache.commons.io.input.BOMInputStream
    val is = getClass.getResourceAsStream("/fileWithBom.txt")
    val bis = new BOMInputStream(is, false)
    //p rintln(bis.getBOM())
    //p rintln(bis.getBOMCharsetName())
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    val result = scala.io.Source.fromInputStream(bis)(Codec(decoder)).getLines
    assertEquals(2, result.size)
  }
  test("mimeTypeFromName for MemoryLocation") {
    Locations.memory("file.jpg").mimeType.get.mimeType shouldBe "image/jpeg"
  }
}
