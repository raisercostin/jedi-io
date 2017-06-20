package org.raisercostin.jedi

import scala.Iterable

import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.VFS
object VfsLocation{
  private val fsManager = VFS.getManager()
  def apply(url: String):VfsLocation = VfsLocation(fsManager.resolveFile(url))
}

case class VfsLocation(file:FileObject) extends NavigableInOutLocation { self =>
  override type Repr = self.type
  def raw = file.getName.getPath
  def fileFullPath: String = file.getName.getPath
  override def build(path:String): Repr = ???//new VfsLocation(file)
  def buildNew(x: FileObject): Repr = new VfsLocation(x)
  override def parent: Repr = buildNew(file.getParent)
  override def child(child: String): Repr = buildNew(file.getChild(child))
  override def childName(child:String):String = file.getChild(child).getName.getPath
  override def exists:Boolean = file.exists
  override def list: Iterable[Repr] = Option(existing).map { x =>
    Option(x.file.getChildren).map(_.toIterable).getOrElse(Iterable(x.file))
  }.getOrElse(Iterable()).map(buildNew)
  def isFile: Boolean = file.isFile
  def isFolder: Boolean = file.isFolder
  def mkdirOnParentIfNecessary:Repr = {
    file.getParent.createFolder
    this
  }
  def asInput: NavigableInOutLocation = self
  def append: Boolean = ???
  def withAppend: Repr = ???
  override def nameAndBefore: String = raw
  override def name:String = file.getName.getBaseName
  def external:VfsLocation = {
    var all = file.getURL.toExternalForm
    all = all.dropWhile { x => x!=':' }.drop(1)
    all = all.reverse.dropWhile{x=>x!='!'}.drop(1).reverse
    VfsLocation(all);
  }
  override def absolute:String = file.getName.getExtension
  override def toString = {
    "VfsLocation[url="+file.getURL+"]"
  }
  println(file)
  def withProtocol(protocol:String):VfsLocation = {
    val newUrl = protocol+":"+file.getURL.toString+"!/"
    println("new "+newUrl)
    VfsLocation(newUrl)
  }
  override def unsafeToInputStream = file.getContent.getInputStream
  override def unsafeToOutputStream = file.getContent.getOutputStream
}