package org.raisercostin.jedi

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

import scala.Iterable
import scala.language.implicitConversions
import scala.language.reflectiveCalls

case class MemoryLocation(val memoryName: String) extends RelativeLocation with NavigableFileInOutLocation {self=>
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
  override def unsafeToOutputStream: OutputStream = outStream
  override def unsafeToInputStream: InputStream = new ByteArrayInputStream(outStream.toByteArray())
  override def child(child: String): Repr = ???
  override def build(path:String): Repr = new MemoryLocation(path)
  override def parent: Repr = ???
  override def withAppend: this.type = ???
  override def length: Long = outStream.size()
  override def mkdirOnParentIfNecessary: this.type = this
  override def exists = true
  override def descendants: Iterable[Repr] = Iterable(this)
  override def size = outStream.size()
  override def childName(child:String):String = ???
  override def isFolder = false
}