

import org.raisercostin.jedi.Locations
import org.raisercostin.jedi.FileLocation
import scala.util.Try

object OrganizeFiles {
  def main(args: Array[String]): Unit = auto
  def auto = {
    val downloadFolders = autodetectDownloadFolders
    val executables = Seq("exe", "msi")
    downloadFolders.map { x =>
      println(s"analyze $x")
      val dest = x.child("_kits").mkdirIfNecessary
      import org.raisercostin.jedi.impl.LogTry._
      x.list.filter(ext => executables.contains(ext.extension)).map { f =>
        Try {
          println(s"move $f")
          f.moveInto(dest)
        }.log
      }
    }
    println("download from " + downloadFolders)
  }

  def autodetectDownloadFolders: Seq[FileLocation] = {
    //use these shell commands (not from run) http://www.winhelponline.com/blog/shell-commands-to-access-the-special-folders/
    Seq(Locations.file("""d:\Downloads"""))
  }
}