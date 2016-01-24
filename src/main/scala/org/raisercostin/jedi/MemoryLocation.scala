package org.raisercostin.jedi

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

import scala.Iterable
import scala.language.implicitConversions
import scala.language.reflectiveCalls

case class MemoryLocation(val memoryName: String) extends RelativeLocationLike with NavigableInOutLocation {self=>
  override type Repr = self.type
  override def nameAndBefore: String = absolute
  override def absolutePlatformDependent: String = memoryName
  def relativePath: String = memoryName
  override def raw = memoryName
  def asInput: MemoryLocation = this
  def append: Boolean = false
  //val buffer: Array[Byte] = Array()
  lazy val outStream = new ByteArrayOutputStream()
  override def toFile: File = ???
  protected override def unsafeToOutputStream: OutputStream = outStream
  protected override def unsafeToInputStream: InputStream = new ByteArrayInputStream(outStream.toByteArray())
  override def child(child: String): Repr = ???
  override def parent: Repr = ???
  override def withAppend: this.type = ???
  override def length: Long = outStream.size()
  override def mkdirOnParentIfNecessary: this.type = this
  override def exists = true
  override def descendants: Iterable[Repr] = Iterable(this)
  override def size = outStream.size()
}