package org.raisercostin.jedi

import java.io.File

object FileUtils {
  import org.apache.commons.io.{FileUtils=>FileUtilsCommons}

  def moveFile(srcFile: File, destFile: File) = {
    Locations.file(destFile).parent.existing
    FileUtilsCommons.moveFile(srcFile, destFile)
  }
}