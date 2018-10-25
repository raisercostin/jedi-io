package org.raisercostin.jedi.impl

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ApacheFileUtils {
  fun forceDelete(path: Path): Unit = try {
    FileUtils.forceDelete(path.toFile())
  } catch (e: IOException) {
    val msg: String = "Unable to delete file: "
    if (e.message!=null && e.message!!.startsWith(msg)) {
        val deleteIfExists = Files.deleteIfExists(Paths.get(e.message!!.removePrefix(msg)))
    } else /* ERROR converting `throw e`*/
      throw e
  }
}