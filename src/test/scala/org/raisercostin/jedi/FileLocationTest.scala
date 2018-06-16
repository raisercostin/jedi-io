package org.raisercostin.jedi
import org.junit.Assert._
import org.junit.Test
import org.scalatest._

import scala.util.Try

//@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class FileLocationTest extends FunSuite with FileAbsoluteBaseLocationTest with NavigableInputLocationReusableTest{

  override def location: FileLocation = Locations.classpath("""a b.jpg""").asFile

  @Test def `basic attributes` {
    println(location.attributes.toMap.mkString("\n"))
    assertNotNull(location.attributes.basic.lastModifiedTime())
  }
  @Test def `owner attributes` {
    assertNotNull(location.attributes.owner.getOwner())
  }
  @Test def `inode on linux sistems` {
    //println("inodedos="+location.attributes.dos.fileKey())
    //println("inodeposix="+location.attributes.posix.fileKey())
    println("inode=" + location.attributes.inode)
    println("uniqueId=" + location.uniqueId)
    println(location.attributes.basic.fileKey())
    assertNotNull(location.attributes.inode)
  }
  @Test def `hardlinks should be detected with same uniqueId` {
    val dest = Locations.temp.randomFolderChild("test").child(location.name)
    location.copyAsHardLink(dest)(CopyOptions.default.withOverwriteIfAlreadyExists)
    val uniqueIdSrc = location.canonicalOverSymLinks
    val uniqueIdDest = dest.canonicalOverSymLinks
    assertNotNull(uniqueIdSrc)
    assertNotNull(uniqueIdDest)
    //This cannot be waranted in jdk8 on windows.
    //assertEquals(uniqueIdSrc,uniqueIdDest)
  }
  @Test def `hardlinks on same drive` {
    val dest = Locations.temp.randomFolderChild("test").child(location.name)
    location.copyAsHardLink(dest)(CopyOptions.default.withOverwriteIfAlreadyExists)
    val dest2 = dest.parent.child(location.name).renamedIfExists
    dest.copyAsHardLink(dest2)(CopyOptions.default.withOverwriteIfAlreadyExists)
    val uniqueIdSrc = dest.canonicalOverSymLinks
    val uniqueIdDest = dest2.canonicalOverSymLinks
    assertNotNull(uniqueIdSrc)
    assertNotNull(uniqueIdDest)
    //This cannot be waranted in jdk8 on windows.
    //assertEquals(uniqueIdSrc,uniqueIdDest)
  }
  @Test def `child starting with separator should not be allowed` {
    val dest = Try{Locations.temp.randomFolderChild("test").child("/test-child")}
    assertTrue(dest.isFailure)
  }
  @Test def `copy to itself detected` {
    val test = Try{
      val dest = Locations.temp.randomFolderChild("test")
      dest.copyTo(dest)
    }
    assertTrue(test.failed.get.getMessage.contains("to itself"))
    assertTrue(test.isFailure)
  }
  @Test def `copy parent folder to child folder detected` {
    val test = Try{
      val parent = Locations.temp.randomFolderChild("test")
      val child = parent.child("childFolder")
      val content = child.child("childFile.txt").mkdirOnParentIfNecessary.writeContent("content")
      parent.copyTo(child)(CopyOptions.copyWithoutMetadata)
    }
    assertTrue(test.isFailure)
    //test.failed.get.printStackTrace()
    assertTrue(test.failed.get.getMessage.contains("to child"))
  }
}
