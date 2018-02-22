package org.raisercostin.jedi

import java.nio.charset.CodingErrorAction

import org.apache.commons.io.FilenameUtils
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.Escape
import org.raisercostin.jedi.impl.MimeTypeDetectors
import scala.util.Try
import scala.util.Success

/**Location orthogonal dimension: Resolved/Unresolved: Can reach content/cannot.*/
trait LocationState
/**
 * Trait to mark if a location is not resolved to a file system. For example Relative locations or offline urls that
 * are not available in offline mode.
 */
trait UnresolvedLocationState extends LocationState
/**If a location has access to its content and metadata is said to be resolved.*/
trait ResolvedLocationState extends LocationState with IsFileOrFolder {
  type MetaRepr <: InputLocation

  /**The meta seen as another location.*/
  def metaLocation: Try[MetaRepr]
  def meta: Try[HierarchicalMultimap] = metaLocation.flatMap(_.existingOption.map(_.readContentAsText.map(x => HierarchicalMultimap(x))).getOrElse(Success(HierarchicalMultimap())))
}

/**
 * There might be ones that are both? Or none? Or undecided?
 */
trait IsFileOrFolder {
  /**Returns true if is file and file exists.*/
  def isFile: Boolean
  /**Returns true if is folder and folder exists.*/
  def isFolder: Boolean
  /**Returns true if is not an existing folder => so could be a file if created.*/
  def canBeFile: Boolean = !isFolder
  /**Returns true if is not an existing file => so could be a folder if created.*/
  def canBeFolder: Boolean = !isFile
}
trait IsFile extends IsFileOrFolder {
  override def isFile = true
  override def isFolder = false
}
trait IsFolder extends IsFileOrFolder {
  override def isFile = false
  override def isFolder = true
}
trait UnknownFileOrFolder extends IsFileOrFolder {
  override def isFile = throw new RuntimeException("Unknown if file or folder.")
  override def isFolder = throw new RuntimeException("Unknown if file or folder.")
}

trait BaseLocation extends IsFileOrFolder{
  def uri:String = raw
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
}