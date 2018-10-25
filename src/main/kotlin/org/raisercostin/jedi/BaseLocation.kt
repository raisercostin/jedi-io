package org.raisercostin.jedi

import java.nio.charset.CodingErrorAction

import org.apache.commons.io.FilenameUtils
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.Escape
import org.raisercostin.jedi.impl.MimeTypeDetectors
import scala.util.Try
import scala.util.Success


interface IsFolder : IsFileOrFolder {
  override fun isFile ()= false
  override fun isFolder ()= true
}
interface UnknownFileOrFolder : IsFileOrFolder {
  override fun isFile ()= throw RuntimeException("Unknown if file or folder.")
  override fun isFolder ()= throw RuntimeException("Unknown if file or folder.")
}

interface BaseLocation : IsFileOrFolder {
  fun uri: String = raw
  fun raw: String
  /**A part of the location that will be used to retrieve name, baseName, extension.*/
  fun nameAndBefore: String
  fun name: String = FilenameUtils.getName(nameAndBefore)
  fun extension: String = FilenameUtils.getExtension(nameAndBefore)
  fun baseName: String = FilenameUtils.getBaseName(nameAndBefore)
  fun mimeType ()= mimeTypeFromName
  fun mimeTypeFromName ()= MimeTypeDetectors.mimeTypeFromName(nameAndBefore)
  //TODO improve slug
  fun slug ()= Escape.toSlug(uri)

  fun decoder {
    import java.nio.charset.Charset
    import java.nio.charset.CodingErrorAction
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    decoder
  }
  fun standard(text: -> String): String = JediFileSystem.standard(text)
  fun standard(selector: this.type -> String): String = JediFileSystem.standard(selector(this))
  fun standardWindows(selector: this.type -> String): String = JediFileSystem.standardWindows(selector(this))
  fun pathInRaw: String = raw.replaceAll("""^(<^*>*)<*>.*$""", "$1")
  //def list: List<FileLocation> = Option(existing.toFile.listFiles).getOrElse(Array<File>()).map(Locations.file(_))

  fun inspect(message: (this.type) -> Any): this.type {
    message(this)
    this
  }
  /**Splits nameAndBefore by folder parts: `foo.folder/bar/bar.jpg` into `<foo.folder,bar,bar.jpg>` */
  fun nameAndBeforeParts: Array<String> = JediFileSystem.splitPartialPath(nameAndBefore)
}