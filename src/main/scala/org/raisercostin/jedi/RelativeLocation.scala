package org.raisercostin.jedi
trait RelativeLocationLike extends NavigableLocation { self =>
  override type Repr = self.type
  def relativePath: String
  override def nameAndBefore: String = relativePath
  def raw: String = relativePath
  def isEmpty: Boolean = relativePath.isEmpty
  def nonEmpty: Boolean = !isEmpty
}
case class RelativeLocation(relativePath: String) extends RelativeLocationLike {self=>
  override type Repr = self.type
  FileSystem.requireRelativePath(relativePath)
  //TODO to remove
  def toFile = ???
  override def parent: Repr = new RelativeLocation(parentName)
  override def child(child: String): Repr = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    new RelativeLocation(if (relativePath.isEmpty) child else FileSystem.addChild(relativePath, child))
  }
}