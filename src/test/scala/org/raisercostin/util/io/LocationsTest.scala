package org.raisercostin.util.io
import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import scala.util.Try
import java.util.regex.Pattern.Loop

@RunWith(classOf[JUnitRunner])
class LocationsTest extends FunSuite {
  import org.scalatest.Matchers._
  test("bug - test spaces in classpath filename") {
    val file = Locations.classpath("a b.jpg")
    assertEquals("a b.jpg", file.resourcePath)
    assertEquals("/a%20b.jpg", file.toUrl.toString().takeRight(10))
    assertEquals("/a%20b.jpg", file.toUrl.toExternalForm().takeRight(10))
    assertEquals("/a%20b.jpg", file.toUrl.toURI().toString().takeRight(10))
    assertEquals("/a%20b.jpg", file.toUrl.toURI().toURL().toString().takeRight(10))
    assertEquals("/a%20b.jpg", file.toUrl.toURI().toASCIIString().takeRight(10))
    assertEquals("a%20b.jpg", new java.io.File(file.toUrl.getFile()).getName())
    assertEquals("a b.jpg", new java.io.File(file.toUrl.toURI()).getName())
    assertEquals("a b.jpg", file.toFile.getName())
    assertEquals("a b.jpg", file.absolute.takeRight(7))
    assertEquals("a b.jpg", file.name)
  }

  test("unzip") {
    assertEquals("""ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/d.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(a.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(b.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/f.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/subzip.zip))""".replaceAll("\r", ""), Locations.classpath("location.zip").unzip.list.mkString("\n"))
    assertEquals("""a - file content""", Locations.classpath("location.zip").unzip.child("a.txt").readContent)
    assertEquals("""f content""", Locations.classpath("location.zip").unzip.child("c/e/f.txt").readContent)
    assertEquals("""ZipInputLocation[ClassPathInputLocation(location.zip),Some(c)]""", Locations.classpath("location.zip").unzip.child("c").raw)
    assertEquals("""ZipInputLocation[ClassPathInputLocation(location.zip),Some(c/e)]""", Locations.classpath("location.zip").unzip.child("c").child("e").raw)
    assertEquals("""ZipInputLocation[ClassPathInputLocation(location.zip),Some(c/e/f.txt)]""", Locations.classpath("location.zip").unzip.child("c").child("e").child("f.txt").raw)
    assertEquals("""f content""", Locations.classpath("location.zip").unzip.child("c").child("e").child("f.txt").readContent)
  }

  test("test relativeTo") {
    val dest = Locations.file("""d:\personal\photos2-proposed1-good""")
    val from = Locations.file("""d:\personal\photos2\""")
    val src = Locations.file("""d:\personal\photos2\1409153946085.jpg""")
    val baseName = "2014-08-27--18-39-03--------1409153946085.jpg"
    assertEquals("""d:\personal\photos2\1409153946085.jpg""", src.absolute)
    assertEquals("""d:\personal\photos2""", from.absolute)
    //assertEquals("""\1409153946085.jpg""",src.diff(src.absolute,from.absolute).get)
    //assertEquals("""1409153946085.jpg""",src.extractAncestor(from).get)
    assertEquals("""1409153946085.jpg""", src.extractPrefix(from).relativePath)
    val destFile = dest.child(src.extractPrefix(from)).withName(_ => baseName).mkdirOnParentIfNecessary
    assertEquals("""d:\personal\photos2-proposed1-good\2014-08-27--18-39-03--------1409153946085.jpg""", destFile.absolute)
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
  test("unzip the subzip") {
    assertEquals("""ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/d.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(a.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(b.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/e/f.txt))
ZipInputLocation(ClassPathInputLocation(location.zip),Some(c/subzip.zip))""".replaceAll("\r", ""),
      Locations.classpath("location.zip").unzip.list.mkString("\n"))
    assertEquals("""c/
c/d.txt
a.txt
b.txt
c/e/
c/e/f.txt""".replaceAll("\r", ""),
      Locations.classpath("location.zip").unzip.child("c/subzip.zip").unzip.list.map { _.name }.mkString("\n"))
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
    assertEquals("2013", Locations.relative("""2013\2013-05-01 - trip to Monschau""").parentName)
    assertEquals("""2013\some space\second space""", Locations.relative("""2013\2013-05-01 - trip to Monschau""").parent.child("some space").child("second space").relativePath)
    assertEquals("""2013""", Locations.file(".").child(Locations.relative("""2013\2013-05-01 - trip to Monschau""")).parent.name)
  }
  test("bug with trailing child fiels") {
    Try { Locations.file(""".""").child("2013-05-01 - trip to ") }.toString should startWith("Failure(java.lang.IllegalArgumentException: requirement failed: Child [2013-05-01 - trip to ] has trailing spaces)")
  }
  test("current folder") {
    Locations.current("target").toString should not include ("." + Locations.SEP + "target")
  }
  test("relative location should not start with file separator") {
    Try { Locations.relative(Locations.SEP + "folder") }.toString should include("shouldn't start with file separator")
  }
  test("relative location parent") {
    Locations.relative("").child("a").raw.toString should equal("a")
    Locations.relative("folder").parentName.toString should equal("")
    Locations.relative("folder").parent.raw.toString should equal("")
    Locations.relative("folder").child("aaaa").parentName.toString should equal("folder")
    Locations.relative("folder").child("aaaa").parent.raw.toString should equal("folder")
  }
  test("relative computation") {
    assertEquals("", Locations.relative("path").extractPrefix(Locations.relative("path")).relativePath)
    assertEquals("", Locations.relative("img.jpg").extractPrefix(Locations.relative("img.jpg")).relativePath)
    assertEquals("/b/img.jpg", Locations.relative("a/b/img.jpg").extractPrefix(Locations.relative("a")).relativePath)
    assertEquals("/img.jpg", Locations.relative("a/b/img.jpg").extractPrefix(Locations.relative("a/b")).relativePath)
    assertEquals("b\\img.jpg", Locations.relative("a\\b\\img.jpg").extractPrefix(Locations.relative("a")).relativePath)
  }

  test("inheritance") {
    //How can I define an inherited operation that returns the current type (and not current type instance) in scala?
    //I already tried to return this.type but in case I need to create a new instance will not work.
    object Test {
      def main(args: Array[String]) {
        trait A {
          //return current instance type
          def op: this.type = { println("opA"); this }
          //return current type (but must be overriden) 
          def op2: A = { println("op2A"); this }
          //return current type (without the need to override)
          def op3[T >: A]: T = { println("opA"); this }
        }
        case class B extends A {
          def doB = println("doB")

          override def op: this.type = {
            println("opB");
            this
            //new B() - compilation error: type mismatch;  found: B  required: B.this.type 
          }
          override def op2: B = {
            println("op2B");
            new B()
            //here it works
          }
          override def op3[T >: B]: B = {
            println("op3B");
            //here it works
            new B()
          }
        }
        //Inherits both op2 and op3 from A but op2 and op3 need to return a C type 
        case class C extends A {
          def doC = println("doC")
        }
        B().op.doB
        C().op.doC

        B().op2.doB
        //C().op2.doC //compilation error => value doC is not a member of A

        B().op3.doB
        //C().op3.doC //compilation error => value doC is not a member of type parameter T

        /**
         * Output:
         * opB
         * doB
         * opA
         * doC
         * op2B
         * doB
         * op3B
         * doB
         */
      }
    }
    Test.main(Array())
  }
}
