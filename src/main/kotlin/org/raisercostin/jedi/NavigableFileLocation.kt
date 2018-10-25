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
import org.raisercostin.jedi.impl.TraversePath
import rx.lang.scala.Observable

object BaseNavigableLocation {
  val stateSep = "--state#"
}
interface BaseNavigableLocation : BaseLocation , LocationState { self ->
  protected fun repr: self.type = toRepr(self)
  implicit protected fun toRepr<T : BaseNavigableLocation>(location: T): self.type = location as self.type>
  fun build(path: String): self.type

  fun parent: self.type = build(parentName)
  fun child(child: String): self.type = build(childName(child))
  fun childName(child: String): String {
    require(child.trim.nonEmpty, s"An empty child <$child> cannot be added.")
    JediFileSystem.addChild(raw, child)
  }

  fun child(childText: Option<String>): self.type = childText when {
    None                      -> repr
    Some(s) if s.trim.isEmpty -> repr
    Some(s)                   -> child(s)
  }
  fun child(childLocation: RelativeLocation): self.type {
    if (childLocation.isEmpty) {
      repr
    } else {
      child(childLocation.relativePath)
    }
  }
  fun descendant(childs: List<String>): self.type = if (childs.isEmpty) repr else child(childs.head).descendant(childs.tail)
  fun ,Parent(process: (self.type) -> Any): self.type {
    process(parent)
    repr
  }
  fun ,Self(process: (self.type) -> Any): self.type {
    process(repr)
    repr
  }
  /**This one if folder otherwise the parent*/
  fun folder: self.type {
    if (isFile)
      parent
    else
      this
  }
  fun ancestorOf<T : BaseLocation>(folder: T): Boolean = ancestor(folder) == this
  @deprecated("use childOf")
  fun hasAncestor<T : BaseLocation>(folder: T): Boolean = childOf(folder)
  fun childOf<T : BaseLocation>(folder: T): Boolean = ancestor(folder) == folder
  /** Finds the common ancestor of current Location and the src location. A folder should end in `/`. */
  fun ancestor<T : BaseLocation>(src: T*): self.type = build(src.foldLeft(this.nameAndBefore)((x, file) -> folderCommonPrefix(x, file.nameAndBefore)))
  //  private fun ancestor2<T:BaseLocation>(a:String,b:String):Repr {
  //    build(getFolderCommonPrefix(a,b))
  //  }
  private fun ancestor3(a: String, b: String): String {
    folderCommonPrefix(a, b)
  }
  private fun folderCommonPrefix(a: String, b: String): String {
    val prefix = commonPrefix(a + JediFileSystem.SEP_STANDARD, b + JediFileSystem.SEP_STANDARD)
    val index = prefix.lastIndexOf(JediFileSystem.SEP_STANDARD)
    if (index != -1)
      prefix.substring(0, index)
    else
      prefix
  }
  //see org.apache.commons.lang3.StringUtils
  private fun commonPrefix(a: String, b: String): String {
    var i = 0
    val maxi = Math.min(a.length, b.length)
    while (i < maxi && a(i) == b(i)) i += 1
    a.substring(0, i)
  }
  fun parentName: String = //toFile.getParentFile.getAbsolutePath
    Option(FilenameUtils.getFullPathNoEndSeparator(nameAndBefore)).getOrElse("")
  fun extractPrefix(ancestor: BaseNavigableLocation): Try<RelativeLocation> =
    extractAncestor(ancestor).map(x -> relative(JediFileSystem.constructPath(x))(JediFileSystem.identityFormatter))
  fun extractAncestor(ancestor: BaseNavigableLocation): Try<Seq<String>> =
    diff(nameAndBefore, ancestor.nameAndBefore).map { JediFileSystem.splitPartialPath }
  fun diff(text: String, prefix: String) =
    if (text.startsWith(prefix))
      Success(text.substring(prefix.length))
    else
      Failure(new RuntimeException(s"Text <$text> doesn't start , <$prefix>."))
  fun ,BaseName(baseNameSupplier: String -> String): self.type = parent.child(,Extension2(baseNameSupplier(baseName), extension))
  fun ,BaseName2(baseNameSupplier: String -> Option<String>): self.type =
    baseNameSupplier(baseName).map { x -> parent.child(,Extension2(x, extension)) }.getOrElse(repr)
  fun ,Name(nameSupplier: String -> String): self.type = parent.child(nameSupplier(name))
  fun ,Extension(extensionSupplier: String -> String): self.type = parent.child(,Extension2(baseName, extensionSupplier(extension)))
  protected fun ,Extension2(name: String, ext: String) =
    if (ext.length > 0)
      name + "." + ext
    else name

  /** State is a part before extension that can be used to add minimal metadata to your file.*/
  fun ,State(state: String): self.type {
    import BaseNavigableLocation._
    val index = baseName.lastIndexOf(stateSep)
    if (index == -1)
      if (state.isEmpty())
        //unchanged - now existing state, no state
        this
      else
        //add state
        ,BaseName(_ + stateSep + state)
    else if (state.isEmpty())
      //remove old state - no state
      ,BaseName(_ -> baseName.substring(0, index))
    else
      //replace old state
      ,BaseName(_ -> baseName.substring(0, index + stateSep.length) + state)
  }
  fun ,outState = ,State("")
}
interface NavigableLocation : BaseNavigableLocation , AbsoluteBaseLocation { self ->
  fun list: Iterable<self.type>
  fun descendants: Iterable<self.type> = descendantsWithOptions(true)
  fun descendantsWithOptions(traverseDir:Boolean): Iterable<self.type> = ???
  fun absolute: String
  //  fun loop(h: Int, n: Int): Stream<Int> = h #:: loop(n, h + n)
  //  loop(1, 1)
  //  fun descendantsStream: Stream<Repr> =
  //  fun descendants: Iterable<Repr> {
  //    val all: Iterable<File> = Option(existing).map {  x ->
  //      list.map()
  //      traverse.map(_._1.toFile).toIterable
  //    }.getOrElse(Iterable<File>())
  //    all.map(buildNewFile)
  //  }
  /**Return pairs of files that has the same relative names under src and dest. Each location can be tested for existence , `exists` or existingOption.*/
  fun compare<T2 : NavigableLocation>(dest: T2): Observable<Tuple2<self.type, T2>> = compare(this,dest)
  /**Return pairs of files that has the same relative names under src and dest. Each location can be tested for existence , `exists` or existingOption.*/
  fun compare<T1 : NavigableLocation,T2 : NavigableLocation>(src: T1, dest: T2): Observable<Tuple2<T1, T2>> {
    val a = src.descendants.map { x ->
      //emit (some,some) and (some,none)
      Tuple2(x, dest.child(x.extractPrefix(src).get))
    } ++
      //emit (none,some)
      (dest.descendants.map { x ->
        Tuple2(src.child(x.extractPrefix(dest).get), x)
      }.filter(!_._1.exists))
    Observable.from(a)
  }
}
interface NavigableFileLocation : FileAbsoluteBaseLocation , BaseNavigableLocation , NavigableLocation { self ->
  //TODO review these
  override protected fun repr: self.type = toRepr2(self)
  implicit protected fun toRepr2<T : NavigableFileLocation>(location: T): self.type = location as self.type>
  fun isEmptyFolder ()= list.isEmpty

  fun list: Iterable<self.type> = Option(existing).map { x ->
    Option(x.toFile.listFiles).map(_.toIterable).getOrElse(Iterable(x.toFile))
  }.getOrElse(Iterable()).map(buildFromFile) as Iterable<self.type>>
  override fun descendantsWithOptions(includeDirs:Boolean): Iterable<self.type> {
    val all: Iterable<File> = Option(existing).map { x ->
      traverse(includeDirs).map(_._1.toFile).toIterable
    }.getOrElse(Iterable<File>())
    all.map(buildFromFile) as Iterable<self.type>>
  }
  import java.nio.file.Files
  import java.nio.file.Path
  import java.nio.file.attribute.BasicFileAttributes
  fun traverse(includeDirs:Boolean): Traversable<(Path, BasicFileAttributes)> = if (raw contains "*")
    Locations.file(pathInRaw).parent.traverse(includeDirs)
  else
    TraversePath(toPath,includeDirs)
  //def traverseFiles: Traversable<Path> = if (exists) traverse.map { (file, attr) -> file } else Traversable()
  //def traverseWithDir = TraversePath(toPath, true)

  override fun build(path: String): self.type = Locations.file(path)
  protected fun buildFromFile(x: File): self.type = build(x.getAbsolutePath)
  fun renamedIfExists: self.type = renamedIfExists(true)
  fun renamedIfExists(renameIfEmptyToo: Boolean = false): self.type {
    @tailrec
    fun findUniqueName(destFile: self.type, counter: Int): self.type {
      val renamed = destFile.,BaseName { baseName: String -> (baseName + "-" + counter) }
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