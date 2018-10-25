package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try


data class StreamLocation(val inputStream: InputStream) : InputLocation , IsFile{
  fun exists: Boolean = true
  fun nameAndBefore: String = inputStream.toString()
  fun raw ()= "inputStream<" + inputStream + ">"
  override fun unsafeToInputStream: InputStream = inputStream
  override fun size:Long = ???
  fun metaLocation:Try<NavigableInOutLocation/*MetaRepr*/> = ???
}

data class StreamProviderLocation(inputStream: ()->InputStream) : InputLocation , IsFile{
  fun exists: Boolean = true
  fun nameAndBefore: String = inputStream.toString()
  fun raw ()= "inputStream<" + inputStream + ">"
  override fun unsafeToInputStream: InputStream = inputStream.apply()
  override fun size:Long = ???
  fun metaLocation:Try<NavigableInOutLocation/*MetaRepr*/> = ???
}