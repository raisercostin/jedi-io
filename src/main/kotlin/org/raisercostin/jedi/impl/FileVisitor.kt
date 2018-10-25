package org.raisercostin.jedi.impl

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.Traversable
import java.io.File

class TraversePath(path: Path, ,Dir: Boolean = false) : Traversable<(Path, BasicFileAttributes)> {
  override fun foreach<U>(f: ((Path, BasicFileAttributes)) -> U) {
    class Visitor : SimpleFileVisitor<Path> {
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        f(file -> attrs)
        FileVisitResult.CONTINUE
        //} catch {
        //else: Throwable -> FileVisitResult.TERMINATE
      }
      override fun preVisitDirectory(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (,Dir) {
          f(file -> attrs)
        }
        FileVisitResult.CONTINUE
      }
    }
    Files.walkFileTree(path, Visitor)
  }
}

interface FileVisitor {
  fun apply(path: String) = fromNioPath(new File(path).toPath)
  fun apply(path: Path) = fromNioPath(path)

  import scala.language.implicitConversions
  implicit fun fromNioPath(path: Path): TraversePath = TraversePath(path)
  implicit fun fromIoFile(file: File): TraversePath = TraversePath(file.toPath)
}
object FileVisitor : FileVisitor