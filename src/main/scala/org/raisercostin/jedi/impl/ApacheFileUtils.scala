package org.raisercostin.jedi.impl

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Path

object ApacheFileUtils {
  def forceDelete(path: Path) = try {
    FileUtils.forceDelete(path.toFile)
  } catch {
    case e: IOException =>
      val msg = "Unable to delete file: "
      if (e.getMessage.startsWith(msg)) {
        Files.deleteIfExists(Paths.get(e.getMessage.stripPrefix(msg)))
      } else
        throw e
  }
}