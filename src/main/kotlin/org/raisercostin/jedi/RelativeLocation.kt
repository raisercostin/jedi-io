package org.raisercostin.jedi

import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.FileSystemFormatter

object RelativeLocation {
  fun apply(relativePath: String) = RelativeLocationImpl(relativePath)
}
interface RelativeLocation : BaseNavigableLocation , UnresolvedLocationState { self ->
  override fun nameAndBefore: String = relativePath
  override fun raw: String = relativePath

  fun relativePath: String
  fun isEmpty: Boolean = relativePath.isEmpty
  fun nonEmpty: Boolean = !isEmpty
  override fun childName(child: String): String = if (relativePath.isEmpty) child else JediFileSystem.addChild(relativePath, child)
  fun relativePath(separator: String): String =
    FileSystemFormatter(separator).standard(relativePath)
  override fun build(path: String): self.type = RelativeLocation(path)
}
data class RelativeLocationImpl(relativePath: String) : RelativeLocation , UnknownFileOrFolder{
  JediFileSystem.requireRelativePath(relativePath)
}