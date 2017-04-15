package org.raisercostin.jedi

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.Traversable
import java.io.File

trait FileVisitor {
  def apply(path: String) = fromNioPath(new File(path).toPath)
  def apply(path: Path) = fromNioPath(path)

  import scala.language.implicitConversions
  implicit def fromNioPath(path: Path): TraversePath = new TraversePath(path)
  implicit def fromIoFile(file: File): TraversePath = new TraversePath(file.toPath)

  class TraversePath(path: Path, withDir: Boolean = false) extends Traversable[(Path, BasicFileAttributes)] {
    override def foreach[U](f: ((Path, BasicFileAttributes)) => U) {
      class Visitor extends SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          f(file -> attrs)
          FileVisitResult.CONTINUE
          //} catch {
          //case _: Throwable => FileVisitResult.TERMINATE
        }
        override def preVisitDirectory(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (withDir) {
            f(file -> attrs)
          }
          FileVisitResult.CONTINUE
        }
      }
      Files.walkFileTree(path, new Visitor)
    }
  }
}
object FileVisitor extends FileVisitor