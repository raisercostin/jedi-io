package org.raisercostin.jedi

import java.nio.charset.CodingErrorAction

import org.apache.commons.io.FilenameUtils
import org.raisercostin.jedi.impl.JediFileSystem

trait BaseLocation {
  def raw: String
  /**A part of the location that will be used to retrieve name, baseName, extension.*/
  def nameAndBefore: String
  def name: String = FilenameUtils.getName(nameAndBefore)
  def extension: String = FilenameUtils.getExtension(nameAndBefore)
  def baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  def mimeType = mimeTypeFromName
  def mimeTypeFromName = MimeTypeDetectors.mimeTypeFromName(nameAndBefore)

  def decoder = {
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    decoder
  }
  def standard(selector: this.type => String): String = JediFileSystem.standard(selector(this))
  def pathInRaw: String = raw.replaceAll("""^([^*]*)[*].*$""", "$1")
  //def list: Seq[FileLocation] = Option(existing.toFile.listFiles).getOrElse(Array[File]()).map(Locations.file(_))

  def inspect(message: (this.type) => Any): this.type = {
    message(this)
    this
  }
}