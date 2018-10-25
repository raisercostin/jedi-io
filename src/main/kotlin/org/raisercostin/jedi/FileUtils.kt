package org.raisercostin.jedi

import java.io.File

object FileUtils {
  import org.apache.commons.io.{ FileUtils -> FileUtilsCommons }

  fun moveFile(srcFile: File, destFile: File) {
    Locations.file(destFile).parent.existing
    if (srcFile.isDirectory())
      FileUtilsCommons.moveDirectory(srcFile, destFile)
    else
      FileUtilsCommons.moveFile(srcFile, destFile)
  }
}