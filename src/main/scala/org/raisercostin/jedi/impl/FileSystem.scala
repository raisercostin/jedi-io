package org.raisercostin.jedi.impl

import java.io.File
import java.util.regex.Pattern

/**
 * A formatter and parser for paths in java. The internal code should always use the internal FileSystem.SEP_STANDARD convention.
 * //TODO All the operations that take paths as strings should specify the way the string is parsed.
 * @see https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/FilenameUtils.html
 * @see http://stackoverflow.com/questions/2417485/file-separator-vs-slash-in-paths
 */
trait FileSystemFormatter {
  def standard(path: String): String
  def inverse: FileSystemFormatter
}

object FileSystemFormatter {
  def apply(separator: String) = SimpleFileSystemFormatter(JediFileSystem.SEP_STANDARD, separator)
}

case class SimpleFileSystemFormatter(from: String, to: String) extends FileSystemFormatter {
  override def standard(path: String): String = path.replaceAllLiterally(from, to)
  def inverse: FileSystemFormatter = SimpleFileSystemFormatter(to, from)
}

object JediFileSystem {
  final val SEP = File.separator
  final val SEP_STANDARD = "/"
  final val WINDOWS_SEPARATOR = "\\"
  val identityFormatter = new FileSystemFormatter() {
    def standard(path: String): String = path
    def inverse: FileSystemFormatter = this
  }
  val unixAndWindowsToStandard = SimpleFileSystemFormatter(WINDOWS_SEPARATOR, SEP_STANDARD)

  def standard(path: String): String = path.replaceAllLiterally(SEP, SEP_STANDARD)
  def requireStandad(path: String) = {
    //TODO: test that the file is not in a form specific to windows or other non linux(standard) way
  }
  def requireRelativePath(path: String) =
    require(!path.startsWith(JediFileSystem.SEP_STANDARD), s"The relative path $path shouldn't start with file separator [${SEP_STANDARD}].")
  def splitRelativePath(path: String): Array[String] = {
    requireRelativePath(path)
    splitPartialPath(path)
  }
  def splitPartialPath(path: String): Array[String] = {
    path.split(Pattern.quote(SEP_STANDARD)).filterNot(_.trim.isEmpty)
  }
  def constructPath(names: Seq[String]): String = names.foldLeft("")((x, y) => (if (x.isEmpty) "" else (x + SEP_STANDARD)) + y)
  def addChild(path: String, child: String): String = path + JediFileSystem.SEP_STANDARD + child
  def normalize(path: String): String = path.replaceAllLiterally(SEP, SEP_STANDARD)
}
