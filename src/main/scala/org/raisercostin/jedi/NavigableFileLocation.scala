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
object BaseNavigableLocation{
  val stateSep = "--state#"
}
trait BaseNavigableLocation extends BaseLocation with LocationState { self =>
  type Repr = self.type
  protected def repr: Repr = toRepr(self)
  implicit protected def toRepr[T<:BaseNavigableLocation](location:T):Repr = location.asInstanceOf[Repr]
  def build(path:String): Repr 

  def parent: Repr = build(parentName)
  def child(child: String): Repr = build(childName(child))
  def childName(child:String):String = {
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
  def withState(state:String): Repr = {
      import BaseNavigableLocation._
      val index = baseName.lastIndexOf(stateSep)
      if(index == -1)
        if(state.isEmpty())
          //unchanged - now existing state, no new state
          this
        else
          //add new state
          withBaseName(_+stateSep+state)
      else
        if(state.isEmpty())
          //remove old state - no new state
          withBaseName(_ => baseName.substring(0,index))
        else
          //replace old state
          withBaseName(_ => baseName.substring(0,index+stateSep.length)+state)
    }
  def withoutState = withState("")
}
trait NavigableLocation extends BaseNavigableLocation{self=>
  
}
trait NavigableFileLocation extends FileAbsoluteBaseLocation with BaseNavigableLocation{ self =>
  override type Repr = self.type
  //TODO review these
  override protected def repr: Repr = toRepr2(self)
  implicit protected def toRepr2[T<:NavigableFileLocation](location:T):Repr = location.asInstanceOf[Repr]

  def list: Iterable[Repr] = Option(existing).map { x =>
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(buildNewFile)
  def descendants: Iterable[Repr] = {
    val all: Iterable[File] = Option(existing).map { x =>
      traverse.map(_._1.toFile).toIterable
    }.getOrElse(Iterable[File]())
    all.map(buildNewFile)
  }

  def buildNewFile(x: File): Repr = Locations.file(x)
  def backupExistingOne:Repr = {
    val newName = renamedIfExists
    if(!newName.equals(this))
      renameTo(newName)
    this
  }
  def renameTo[T <: FileAbsoluteBaseLocation](newName:T):T = {
     FileUtils.moveFile(toFile, newName.toFile)
     newName
  }
  def renamedIfExists: Repr = {
    @tailrec
    def findUniqueName(destFile: Repr, counter: Int): Repr = {
      val renamed = destFile.withBaseName { baseName: String => (baseName + "-" + counter) }
      if (renamed.exists)
        findUniqueName(destFile, counter + 1)
      else
        renamed
    }
    if (repr.exists)
      findUniqueName(repr, 1)
    else
      repr
  }
}