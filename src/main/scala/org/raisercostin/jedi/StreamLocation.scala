package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls


case class StreamLocation(val inputStream: InputStream) extends InputLocation with IsFile{
  def exists: Boolean = true
  def nameAndBefore: String = inputStream.toString()
  def raw = "inputStream[" + inputStream + "]"
  override def unsafeToInputStream: InputStream = inputStream
  override def size:Long = ???
}

case class StreamProviderLocation(inputStream: ()=>InputStream) extends InputLocation with IsFile{
  def exists: Boolean = true
  def nameAndBefore: String = inputStream.toString()
  def raw = "inputStream[" + inputStream + "]"
  override def unsafeToInputStream: InputStream = inputStream.apply()
  override def size:Long = ???
}
