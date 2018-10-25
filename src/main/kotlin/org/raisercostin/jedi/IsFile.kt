package org.raisercostin.jedi
trait IsFile extends IsFileOrFolder {
  override def isFile = true
  override def isFolder = false
}