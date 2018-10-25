package org.raisercostin.jedi
/**
 * There might be ones that are both? Or none? Or undecided?
 */
interface IsFileOrFolder {
  /**Returns true if is file and file exists.*/
  fun isFile: Boolean
  /**Returns true if is folder and folder exists.*/
  fun isFolder: Boolean
  /**Returns true if is not an existing folder -> so could be a file if created.*/
  fun canBeFile: Boolean = !isFolder
  /**Returns true if is not an existing file -> so could be a folder if created.*/
  fun canBeFolder: Boolean = !isFile
}