package org.raisercostin.jedi

trait IsFolder {
  /**Returns true if is folder and folder exists.*/
  def isFolder:Boolean = true
}

trait IsFile /*extends IsFileOrFolder2*/{
  /**Returns true if is file and file exists.*/
  def isFile: Boolean = true
}
//trait UnknownFileOrFolder2 extends IsFileOrFolder2 {
//  
//  
//}

/**
 * There might be ones that are both? Or none? Or undecided?
 */
trait IsFileOrFolder extends IsFile with IsFolder {
  override def isFile:Boolean = throw new RuntimeException("Unknown if file or folder.")
  override def isFolder:Boolean = throw new RuntimeException("Unknown if file or folder.")

  /**Returns true if is not an existing folder => so could be a file if created.*/
  def canBeFile: Boolean = !isFolder
  /**Returns true if is not an existing file => so could be a folder if created.*/
  def canBeFolder: Boolean = !isFile
}