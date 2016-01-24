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


trait NavigableLocation extends AbsoluteBaseLocation { self =>
  type Repr = self.type
  protected def repr: Repr = toRepr(self)
  implicit protected def toRepr[T<:NavigableLocation](location:T):Repr = location.asInstanceOf[Repr]

  def parent: Repr
  def child(child: String): Repr
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
  def extractPrefix(ancestor: NavigableLocation): Try[RelativeLocation] =
    extractAncestor(ancestor).map(x => relative(FileSystem.constructPath(x))(FileSystem.identityFormatter))
  def extractAncestor(ancestor: NavigableLocation): Try[Seq[String]] =
    diff(nameAndBefore, ancestor.nameAndBefore).map { FileSystem.splitPartialPath }
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
  def renamedIfExists: Repr = {
    @tailrec
    def findUniqueName(destFile: Repr, counter: Int): Repr =
      if (destFile.exists)
        findUniqueName(destFile.withBaseName { baseName: String => (baseName + "-" + counter) }, counter + 1)
      else
        destFile
    findUniqueName(repr, 1)
  }
}