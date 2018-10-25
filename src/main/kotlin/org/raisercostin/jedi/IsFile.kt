package org.raisercostin.jedi

interface IsFile : IsFileOrFolder {
  override fun isFile(): Boolean = true
  override fun isFolder(): Boolean = false
}