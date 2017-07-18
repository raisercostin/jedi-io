

import org.raisercostin.jedi.Locations
import org.raisercostin.jedi.FileLocation
object OrganizeFiles {
  def main(args: Array[String]): Unit = auto
  def auto = {
    val downloadFolders = autodetectDownloadFolders
    val executables=Seq("exe","msi")
    downloadFolders.foreach{x=>
      val dest = x.child("_kits").mkdirIfNecessary
      x.list.filter(ext => executables.contains(ext)).foreach{f=>
        f.moveInside(dest)
      }
    }
    println("download from "+downloadFolders)
  }

  def autodetectDownloadFolders: Seq[FileLocation] = {
    //use these shell commands (not from run) http://www.winhelponline.com/blog/shell-commands-to-access-the-special-folders/
    Seq(Locations.file("""d:\Downloads"""))
  }
}