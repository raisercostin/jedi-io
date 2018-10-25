package org.raisercostin.jedi
interface IsFile : IsFileOrFolder {
  override fun isFile ()= true
  override fun isFolder ()= false
}