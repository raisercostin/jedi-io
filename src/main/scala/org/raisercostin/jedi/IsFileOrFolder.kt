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