package org.raisercostin.jedi

import scala.Iterable

import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.VFS
object VfsLocation{
  private val fsManager = VFS.getManager()
  fun apply(url: String):VfsLocation = VfsLocation(fsManager.resolveFile(url))
}

data class VfsLocation(file:FileObject) : NavigableInOutLocation { self ->
  fun raw ()= file.getName.getPath
  fun fileFullPath: String = file.getName.getPath
  override fun build(path:String): self.type = ???//new VfsLocation(file)
  fun buildNew(x: FileObject): self.type = VfsLocation(x)
  override fun parent: self.type = buildNew(file.getParent)
  override fun child(child: String): self.type = buildNew(file.getChild(child))
  override fun childName(child:String):String = file.getChild(child).getName.getPath
  override fun exists:Boolean = file.exists
  override fun list: Iterable<self.type> = Option(existing).map { x ->
    Option(x.file.getChildren).map(_.toIterable).getOrElse(Iterable(x.file))
  }.getOrElse(Iterable()).map(buildNew) as Iterable<self.type>>
  fun isFile: Boolean = file.isFile
  fun isFolder: Boolean = file.isFolder
  fun mkdirIfNecessary: self.type {
    file.createFolder
    this
  }
  fun mkdirOnParentIfNecessary:self.type {
    file.getParent.createFolder
    this
  }
  fun asInput: NavigableInOutLocation = self
  override fun append: Boolean = ???
  fun ,Append: self.type = ???
  override fun size:Long = file.getContent.getSize
  override fun nameAndBefore: String = raw
  override fun name:String = file.getName.getBaseName
  fun external:VfsLocation {
    var all = file.getURL.toExternalForm
    all = all.dropWhile { x -> x!=':' }.drop(1)
    all = all.reverse.dropWhile{x->x!='!'}.drop(1).reverse
    VfsLocation(all);
  }
  override fun absolute:String = file.getName.getExtension
  override fun toString {
    "VfsLocation<url="+file.getURL+">"
  }
  println(file)
  fun ,Protocol(protocol:String):VfsLocation {
    val newUrl = protocol+":"+file.getURL.toString+"!/"
    println("new "+newUrl)
    VfsLocation(newUrl)
  }
  override fun unsafeToInputStream ()= file.getContent.getInputStream
  override fun unsafeToOutputStream ()= file.getContent.getOutputStream
}