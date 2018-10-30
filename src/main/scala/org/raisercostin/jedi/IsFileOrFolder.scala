package org.raisercostin.jedi
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
