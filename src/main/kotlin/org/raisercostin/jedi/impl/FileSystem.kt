package org.raisercostin.jedi.impl

import java.io.File
import java.util.regex.Pattern

/**
 * A formatter and parser for paths in java. The internal code should always use the internal FileSystem.SEP_STANDARD convention.
 * //TODO All the operations that take paths as strings should specify the way the string is parsed.
 * @see https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/FilenameUtils.html
 * @see http://stackoverflow.com/questions/2417485/file-separator-vs-slash-in-paths
 */
interface FileSystemFormatter {
  fun standard(path: String): String
  fun inverse: FileSystemFormatter
}

object FileSystemFormatter {
  fun apply(separator: String) = SimpleFileSystemFormatter(JediFileSystem.SEP_STANDARD, separator)
}

data class SimpleFileSystemFormatter(from: String, to: String) : FileSystemFormatter {
  override fun standard(path: String): String = path.replaceAllLiterally(from, to)
  fun inverse: FileSystemFormatter = SimpleFileSystemFormatter(to, from)
}

object JediFileSystem {
  val SEP = File.separator
  val SEP_STANDARD = "/"
  val SEP_WINDOWS = "\\"
  val identityFormatter = FileSystemFormatter() {
    fun standard(path: String): String = path
    fun inverse: FileSystemFormatter = this
  }
  val unixAndWindowsToStandard = SimpleFileSystemFormatter(SEP_WINDOWS, SEP_STANDARD)

  fun standard(path: String): String = path.replaceAllLiterally(SEP, SEP_STANDARD)
  fun standardWindows(path: String): String = path.replaceAllLiterally(SEP, SEP_WINDOWS)
  fun requireStandad(path: String) {
    //TODO: test that the file is not in a form specific to windows or other non linux(standard) way
  }
  fun requireRelativePath(path: String) =
    require(!path.startsWith(JediFileSystem.SEP_STANDARD), s"The relative path $path shouldn't start , file separator <${SEP_STANDARD}>.")
  fun splitRelativePath(path: String): Array<String> {
    requireRelativePath(path)
    splitPartialPath(path)
  }
  fun splitPartialPath(path: String): Array<String> {
    path.split(Pattern.quote(SEP_STANDARD)).filterNot(_.trim.isEmpty)
  }
  fun constructPath(names: List<String>): String = names.mkString("",SEP_STANDARD,"")
  fun addChild(path: String, child: String): String = path + JediFileSystem.SEP_STANDARD + child
  fun normalize(path: String): String = path.replaceAllLiterally(SEP, SEP_STANDARD)
}