package org.raisercostin.jedi

import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.FileSystemFormatter

trait RelativeLocationLike extends BaseNavigableLocation with UnresolvedLocationState{ self =>
  override type Repr = self.type
  override def nameAndBefore: String = relativePath
  override def raw: String = relativePath

  def relativePath: String
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
}
case class RelativeLocation(relativePath: String) extends RelativeLocationLike {self=>
  override type Repr = self.type
  JediFileSystem.requireRelativePath(relativePath)
  override def build(path:String): Repr = new RelativeLocation(path)
  override def childName(child:String):String = if (relativePath.isEmpty) child else JediFileSystem.addChild(relativePath, child)
  
  def relativePath(separator:String):String =
    FileSystemFormatter(separator).standard(relativePath)
}