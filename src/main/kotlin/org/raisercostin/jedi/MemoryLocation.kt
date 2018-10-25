package org.raisercostin.jedi

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

import scala.Iterable
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try

data class MemoryLocation(val memoryName: String) : RelativeLocation , InOutLocation , NavigableInOutLocation{self->
  override fun nameAndBefore: String = absolute
  fun absolute: String = memoryName
  fun relativePath: String = memoryName
  override fun raw ()= memoryName
  fun asInput: MemoryLocation = this
  override fun append: Boolean = false
  //val buffer: Array<Byte> = Array()
  lazy val outStream = ByteArrayOutputStream()
  override fun unsafeToOutputStream: OutputStream = outStream
  override fun unsafeToInputStream: InputStream = ByteArrayInputStream(outStream.toByteArray())
  override fun child(child: String): self.type = ???
  override fun build(path:String): self.type = MemoryLocation(path)
  override fun parent: self.type = ???
  override fun ,Append: this.type = ???
  override fun size: Long = outStream.size()
  override fun exists ()= true
  override fun descendantsWithOptions(traverseDir:Boolean): Iterable<self.type> = Iterable(this)
  override fun list: Iterable<self.type> = Iterable(this)
  override fun childName(child:String):String = ???
  override fun isFolder ()= false
  override fun isFile: Boolean = true
  override fun metaLocation:Try<NavigableInOutLocation/*MetaRepr*/> = ???
  override fun mkdirIfNecessary: self.type = ???
  override fun mkdirOnParentIfNecessary: self.type = ???
}