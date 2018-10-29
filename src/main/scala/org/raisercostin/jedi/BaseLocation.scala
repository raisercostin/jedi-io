package org.raisercostin.jedi

import java.nio.charset.CodingErrorAction

import org.apache.commons.io.FilenameUtils
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.Escape
import org.raisercostin.jedi.impl.MimeTypeDetectors
import scala.util.Try
import scala.util.Success



trait BaseLocation extends IsFileOrFolder {
  def uri: String = raw
  def raw: String
  /**A part of the location that will be used to retrieve name, baseName, extension.*/
  def nameAndBefore: String
  def name: String = FilenameUtils.getName(nameAndBefore)
  def extension: String = FilenameUtils.getExtension(nameAndBefore)
  def baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  def mimeType = mimeTypeFromName
  def mimeTypeFromName = MimeTypeDetectors.mimeTypeFromName(nameAndBefore)
  //TODO improve slug
  def slug = Escape.toSlug(uri)

  def decoder = {
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    decoder
  }
  def standard(text: => String): String = JediFileSystem.standard(text)
  def standard(selector: this.type => String): String = JediFileSystem.standard(selector(this))
  def standardWindows(selector: this.type => String): String = JediFileSystem.standardWindows(selector(this))
  def pathInRaw: String = raw.replaceAll("""^([^*]*)[*].*$""", "$1")
  //def list: Seq[FileLocation] = Option(existing.toFile.listFiles).getOrElse(Array[File]()).map(Locations.file(_))

  def inspect(message: (this.type) => Any): this.type = {
    message(this)
    this
  }
  /**Splits nameAndBefore by folder parts: `foo.folder/bar/bar.jpg` into `[foo.folder,bar,bar.jpg]` */
  def nameAndBeforeParts: Array[String] = JediFileSystem.splitPartialPath(nameAndBefore)
}