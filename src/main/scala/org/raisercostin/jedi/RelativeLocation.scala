package org.raisercostin.jedi

import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.FileSystemFormatter

object RelativeLocation {
  def apply(relativePath: String) = RelativeLocationImpl(relativePath)
}
trait RelativeLocation extends BaseNavigableLocation with UnresolvedLocationState { self =>
  override type Repr = self.type
  override def nameAndBefore: String = relativePath
  override def raw: String = relativePath

  def relativePath: String
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
  override def childName(child: String): String = if (relativePath.isEmpty) child else JediFileSystem.addChild(relativePath, child)
  def relativePath(separator: String): String =
    FileSystemFormatter(separator).standard(relativePath)
  override def build(path: String): Repr = RelativeLocation(path)
}
case class RelativeLocationImpl(relativePath: String) extends RelativeLocation with UnknownFileOrFolder{
  JediFileSystem.requireRelativePath(relativePath)
}