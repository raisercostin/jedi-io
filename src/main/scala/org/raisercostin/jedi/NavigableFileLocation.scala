package org.raisercostin.jedi

import java.io.File
import java.nio.file.{Path, Paths}
import java.nio.file.PathMatcher
import scala.Iterable
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.apache.commons.io.FilenameUtils
import Locations.relative
import io.reactivex.Flowable
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.TraversePath
import rx.lang.scala.Observable

object BaseNavigableLocation {
  val stateSep = "--state#"
}
trait BaseNavigableLocation extends BaseLocation with LocationState with IsFileOrFolder{ self =>
  protected def repr: self.type = toRepr(self)
  implicit protected def toRepr[T <: BaseNavigableLocation](location: T): self.type = location.asInstanceOf[self.type]
  def build(path: String): self.type

  def parent: self.type = build(parentName)
  def child(child: String): self.type = build(childName(child))
  def childName(child: String): String = {
    require(child.trim.nonEmpty, s"An empty child [$child] cannot be added.")
    JediFileSystem.addChild(raw, child)
  }

  def child(childText: Option[String]): self.type = childText match {
    case None                      => repr
    case Some(s) if s.trim.isEmpty => repr
    case Some(s)                   => child(s)
  }
  def child(childLocation: RelativeLocation): self.type = {
    if (childLocation.isEmpty) {
      repr
    } else {
      child(childLocation.relativePath)
    }
  }
  def descendant(childs: Seq[String]): self.type = if (childs.isEmpty) repr else child(childs.head).descendant(childs.tail)
  def withParent(process: (self.type) => Any): self.type = {
    process(parent)
    repr
  }
  def withSelf(process: (self.type) => Any): self.type = {
    process(repr)
    repr
  }
  /**This one if folder otherwise the parent*/
  def folder: self.type = {
    if (isFile)
      parent
    else
      this
  }
  def ancestorOf[T <: BaseLocation](folder: T): Boolean = ancestor(folder) == this
  @deprecated("use childOf")
  def hasAncestor[T <: BaseLocation](folder: T): Boolean = childOf(folder)
  def childOf[T <: BaseLocation](folder: T): Boolean = ancestor(folder) == folder
  /** Finds the common ancestor of current Location and the src location. A folder should end in `/`. */
  def ancestor[T <: BaseLocation](src: T*): self.type = build(src.foldLeft(this.nameAndBefore)((x, file) => folderCommonPrefix(x, file.nameAndBefore)))
  //  private def ancestor2[T<:BaseLocation](a:String,b:String):Repr = {
  //    build(getFolderCommonPrefix(a,b))
  //  }
  private def ancestor3(a: String, b: String): String = {
    folderCommonPrefix(a, b)
  }
  private def folderCommonPrefix(a: String, b: String): String = {
    val prefix = commonPrefix(a + JediFileSystem.SEP_STANDARD, b + JediFileSystem.SEP_STANDARD)
    val index = prefix.lastIndexOf(JediFileSystem.SEP_STANDARD)
    if (index != -1)
      prefix.substring(0, index)
    else
      prefix
  }
  //see org.apache.commons.lang3.StringUtils
  private def commonPrefix(a: String, b: String): String = {
    var i = 0
    val maxi = Math.min(a.length, b.length)
    while (i < maxi && a(i) == b(i)) i += 1
    a.substring(0, i)
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
  def withBaseName(baseNameSupplier: String => String): self.type = parent.child(withExtension2(baseNameSupplier(baseName), extension))
  def withBaseName2(baseNameSupplier: String => Option[String]): self.type =
    baseNameSupplier(baseName).map { x => parent.child(withExtension2(x, extension)) }.getOrElse(repr)
  def withName(nameSupplier: String => String): self.type = parent.child(nameSupplier(name))
  def withExtension(extensionSupplier: String => String): self.type = parent.child(withExtension2(baseName, extensionSupplier(extension)))
  protected def withExtension2(name: String, ext: String) =
    if (ext.length > 0)
      name + "." + ext
    else name

  /** State is a part before extension that can be used to add minimal metadata to your file.*/
  def withState(state: String): self.type = {
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
trait NavigableLocation extends BaseNavigableLocation with AbsoluteBaseLocation { self =>
  def list: Iterable[self.type]
  import scala.collection.JavaConverters._
  import scala.compat.java8.FunctionConverters._

  val function: java.util.function.Function[Path, self.type] = new java.util.function.Function[Path,self.type](){
    override def apply(t: Path): NavigableLocation.this.type =
      build(t.toFile.getAbsolutePath)
  }
  def visit(matcher:String = FileTraversals.GLOB_ALL, ignoreCase:Boolean = true):Flowable[self.type] =
    visitFull(matcher,ignoreCase = ignoreCase)

  /**See java.nio.file.PathMatcher for regex.
    * @param pruningMatcher filters in/out folders when searching
    */
  def visitFull(matcher:String = FileTraversals.GLOB_ALL, pruningMatcher:String = FileTraversals.GLOB_NONE, ignoreCase:Boolean = true):Flowable[self.type] =
    FileTraversals
      //.traverseUsingWalk()
      .traverseUsingGuavaAndDirectoryStream()
      .traverse(Paths.get(absolute),matcher, pruningMatcher, ignoreCase ,function)
      //Flowable.fromIterable(descendants.asJava)
      .asInstanceOf[Flowable[self.type]]
  def visit2(matcher:PathMatcher, pruningMatcher:PathMatcher, ignoreCase:Boolean):Flowable[self.type] =
    FileTraversals
      //.traverseUsingWalk()
      .traverseUsingGuavaAndDirectoryStream()
      .traverse(Paths.get(absolute),matcher, pruningMatcher, ignoreCase, function)
      //Flowable.fromIterable(descendants.asJava)
      .asInstanceOf[Flowable[self.type]]
  final def descendants: Iterable[self.type] = descendantsWithOptions(true)
  def descendantsWithOptions(traverseDir:Boolean): Iterable[self.type] = ???
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
  /**Return pairs of files that has the same relative names under src and dest. Each location can be tested for existence with `exists` or existingOption.*/
  def compare[T2 <: NavigableLocation](dest: T2): Observable[Tuple2[self.type, T2]] = compare(this,dest)
  /**Return pairs of files that has the same relative names under src and dest. Each location can be tested for existence with `exists` or existingOption.*/
  def compare[T1 <: NavigableLocation,T2 <: NavigableLocation](src: T1, dest: T2): Observable[Tuple2[T1, T2]] = {
    val a = src.descendants.map { x =>
      //emit (some,some) and (some,none)
      Tuple2(x, dest.child(x.extractPrefix(src).get))
    } ++
      //emit (none,some)
      (dest.descendants.map { x =>
        Tuple2(src.child(x.extractPrefix(dest).get), x)
      }.filter(!_._1.exists))
    Observable.from(a)
  }
}
trait NavigableFileLocation extends FileAbsoluteBaseLocation with BaseNavigableLocation with NavigableLocation { self =>
  //TODO review these
  override protected def repr: self.type = toRepr2(self)
  implicit protected def toRepr2[T <: NavigableFileLocation](location: T): self.type = location.asInstanceOf[self.type]
  def isEmptyFolder = list.isEmpty

  def list: Iterable[self.type] = Option(existing).map { x =>
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(buildFromFile).asInstanceOf[Iterable[self.type]]
  override def descendantsWithOptions(includeDirs:Boolean): Iterable[self.type] = {
    val all: Iterable[File] = Option(existing).map { x =>
      traverse(includeDirs).map(_._1.toFile).toIterable
    }.getOrElse(Iterable[File]())
    all.map(buildFromFile).asInstanceOf[Iterable[self.type]]
  }
  import java.nio.file.Files
  import java.nio.file.Path
  import java.nio.file.attribute.BasicFileAttributes
  def traverse(includeDirs:Boolean): Traversable[(Path, BasicFileAttributes)] = if (raw contains "*")
    Locations.file(pathInRaw).parent.traverse(includeDirs)
  else
    new TraversePath(toPath,includeDirs)
  //def traverseFiles: Traversable[Path] = if (exists) traverse.map { case (file, attr) => file } else Traversable()
  //def traverseWithDir = new TraversePath(toPath, true)

  override def build(path: String): self.type = Locations.file(path)
  protected def buildFromFile(x: File): self.type = build(x.getAbsolutePath)
  def renamedIfExists: self.type = renamedIfExists(true)
  def renamedIfExists(renameIfEmptyToo: Boolean = false): self.type = {
    @tailrec
    def findUniqueName(destFile: self.type, counter: Int): self.type = {
      val renamed = destFile.withBaseName { baseName: String => (baseName + "-" + counter) }
      if (renamed.existsWithoutResolving)
        if (renameIfEmptyToo && exists && list.isEmpty)
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