package org.raisercostin.jedi

import java.io.File

import scala.Iterable
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.apache.commons.io.FilenameUtils

import Locations.relative
import org.raisercostin.jedi.impl.JediFileSystem
object BaseNavigableLocation {
  val stateSep = "--state#"
}
trait BaseNavigableLocation extends BaseLocation with LocationState { self =>
  type Repr = self.type
  protected def repr: Repr = toRepr(self)
  implicit protected def toRepr[T <: BaseNavigableLocation](location: T): Repr = location.asInstanceOf[Repr]
  def build(path: String): Repr

  def parent: Repr = build(parentName)
  def child(child: String): Repr = build(childName(child))
  def childName(child: String): String = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    JediFileSystem.addChild(raw, child)
  }

  def child(childText: Option[String]): Repr = childText match {
    case None => repr
    case Some(s) if s.trim.isEmpty => repr
    case Some(s) => child(s)
  }
  def child(childLocation: RelativeLocation): Repr = {
    if (childLocation.isEmpty) {
      repr
    } else {
      child(childLocation.relativePath)
    }
  }
  def descendant(childs: Seq[String]): Repr = if (childs.isEmpty) repr else child(childs.head).descendant(childs.tail)
  def withParent(process: (Repr) => Any): Repr = {
    process(parent)
    repr
  }
  def withSelf(process: (Repr) => Any): Repr = {
    process(repr)
    repr
  }
  /**This one if folder otherwise the parent*/
  def folder: Repr = {
    if(isFile)
      parent
    else
      this
  }
  /** A folder should end in `/`. */
  def ancestor[T<:BaseLocation](src:T*):Repr = build(src.foldLeft(this.nameAndBefore)((x,file)=>folderCommonPrefix(x,file.nameAndBefore)))
//  private def ancestor2[T<:BaseLocation](a:String,b:String):Repr = {
//    build(getFolderCommonPrefix(a,b))
//  }
  private def ancestor3(a:String,b:String):String = {
    folderCommonPrefix(a,b)
  }
  private def folderCommonPrefix(a:String, b:String):String = {
    val prefix = commonPrefix(a+JediFileSystem.SEP_STANDARD,b+JediFileSystem.SEP_STANDARD)
    val index = prefix.lastIndexOf(JediFileSystem.SEP_STANDARD)
    if(index != -1)
      prefix.substring(0, index)
    else
      prefix
  }
  //see org.apache.commons.lang3.StringUtils
  private def commonPrefix(a:String, b:String):String = {
    var i = 0
    val maxi = Math.min(a.length,b.length)
    while(i<maxi && a(i)==b(i)) i+=1
    a.substring(0,i)
  }
  def parentName: String = //toFile.getParentFile.getAbsolutePath
    Option(FilenameUtils.getFullPathNoEndSeparator(nameAndBefore)).getOrElse("")
  def extractPrefix(ancestor: BaseNavigableLocation): Try[RelativeLocation] =
    extractAncestor(ancestor).map(x => relative(JediFileSystem.constructPath(x))(JediFileSystem.identityFormatter))
  def extractAncestor(ancestor: BaseNavigableLocation): Try[Seq[String]] =
    diff(nameAndBefore, ancestor.nameAndBefore).map { JediFileSystem.splitPartialPath }
  def diff(text: String, prefix: String) =
    if (text.startsWith(prefix))
      Success(text.substring(prefix.length))
    else
      Failure(new RuntimeException(s"Text [$text] doesn't start with [$prefix]."))
  def withBaseName(baseNameSupplier: String => String): Repr = parent.child(withExtension2(baseNameSupplier(baseName), extension))
  def withBaseName2(baseNameSupplier: String => Option[String]): Repr =
    baseNameSupplier(baseName).map { x => parent.child(withExtension2(x, extension)) }.getOrElse(repr)
  def withName(nameSupplier: String => String): Repr = parent.child(nameSupplier(name))
  def withExtension(extensionSupplier: String => String): Repr = parent.child(withExtension2(baseName, extensionSupplier(extension)))
  protected def withExtension2(name: String, ext: String) =
    if (ext.length > 0)
      name + "." + ext
    else name

  /** State is a part before extension that can be used to add minimal metadata to your file.*/
  def withState(state: String): Repr = {
    import BaseNavigableLocation._
    val index = baseName.lastIndexOf(stateSep)
    if (index == -1)
      if (state.isEmpty())
        //unchanged - now existing state, no new state
        this
      else
        //add new state
        withBaseName(_ + stateSep + state)
    else if (state.isEmpty())
      //remove old state - no new state
      withBaseName(_ => baseName.substring(0, index))
    else
      //replace old state
      withBaseName(_ => baseName.substring(0, index + stateSep.length) + state)
  }
  def withoutState = withState("")
}
trait NavigableLocation extends BaseNavigableLocation with AbsoluteBaseLocation{ self =>
  override type Repr = self.type
  def list: Iterable[Repr]
  def descendants: Iterable[Repr] = ???
  def absolute: String
//  def loop(h: Int, n: Int): Stream[Int] = h #:: loop(n, h + n)
//  loop(1, 1)
//  def descendantsStream: Stream[Repr] =
//  def descendants: Iterable[Repr] = {
//    val all: Iterable[File] = Option(existing).map {  x =>
//      list.map()
//      traverse.map(_._1.toFile).toIterable
//    }.getOrElse(Iterable[File]())
//    all.map(buildNewFile)
//  }
}
trait NavigableFileLocation extends FileAbsoluteBaseLocation with BaseNavigableLocation with NavigableLocation{ self =>
  override type Repr = self.type
  //TODO review these
  override protected def repr: Repr = toRepr2(self)
  implicit protected def toRepr2[T <: NavigableFileLocation](location: T): Repr = location.asInstanceOf[Repr]
  def isEmptyFolder = list.isEmpty

  def list: Iterable[Repr] = Option(existing).map { x =>
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(buildNewFile)
  override def descendants: Iterable[Repr] = {
    val all: Iterable[File] = Option(existing).map { x =>
      traverse.map(_._1.toFile).toIterable
    }.getOrElse(Iterable[File]())
    all.map(buildNewFile)
  }

  def buildNewFile(x: File): Repr = Locations.file(x)
  def renamedIfExists: Repr = renamedIfExists(true)
  def renamedIfExists(renameIfEmptyToo:Boolean = false): Repr = {
    @tailrec
    def findUniqueName(destFile: Repr, counter: Int): Repr = {
      val renamed = destFile.withBaseName { baseName: String => (baseName + "-" + counter) }
      if (renamed.existsWithoutResolving)
        if(renameIfEmptyToo && exists && list.isEmpty)
          renamed
        else
          findUniqueName(destFile, counter + 1)
      else
        renamed
    }
    if (repr.existsWithoutResolving)
      findUniqueName(repr, 1)
    else
      repr
  }
}