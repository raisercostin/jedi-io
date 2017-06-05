package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import org.raisercostin.jedi.impl.ResourceUtil
import sun.net.www.protocol.file.FileURLConnection
import org.raisercostin.jedi.impl.SlfLogger
import org.raisercostin.jedi.impl.Http2
import org.raisercostin.jedi.impl.HttpRequest
import org.raisercostin.jedi.impl.DefaultConnectFunc
import scalaj.http.HttpConstants
import org.raisercostin.jedi.impl.QueryStringUrlFunc
import java.net.HttpURLConnection
import scala.util.Failure
import java.io.IOException
case class EmailLocation(host:String, port:Int, username:String, password:String) extends NavigableFileInOutLocation { self =>
  override type Repr = self.type

/** As seen from class EmailLocation, the missing signatures are as follows.
 *  For convenience, these are usable as stub implementations.
 */
  // Members declared in org.raisercostin.jedi.BaseLocation
  def raw: String = ???

  // Members declared in org.raisercostin.jedi.BaseNavigableLocation
  def build(path: String): Repr = ???

  // Members declared in org.raisercostin.jedi.FileAbsoluteBaseLocation
  def toFile: java.io.File = ???

  // Members declared in org.raisercostin.jedi.InputLocation
  def unsafeToInputStream: java.io.InputStream = ???

  // Members declared in org.raisercostin.jedi.NavigableOutputLocation
  def asInput: org.raisercostin.jedi.NavigableFileInputLocation = ???

  // Members declared in org.raisercostin.jedi.OutputLocation
  def append: Boolean = ???
  def unsafeToOutputStream: java.io.OutputStream = ???
  def withAppend: Repr = ???
}