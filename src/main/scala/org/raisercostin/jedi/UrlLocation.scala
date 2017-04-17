package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import org.raisercostin.jedi.impl.ResourceUtil
import sun.net.www.protocol.file.FileURLConnection

case class UrlLocation(url: java.net.URL, agent:Option[String] = None) extends InputLocation { self =>
  def raw = url.toExternalForm()
  override def toUrl: java.net.URL = url
  override def nameAndBefore: String = url.getPath
  def toFile: File = ???
  import java.net._
  override def length: Long = lengthTry.get
  def lengthTry: Try[Long] = ResourceUtil.cleanly(url.openConnection()) {
    case c: HttpURLConnection =>
      c.disconnect()
    case f: FileURLConnection =>
      f.close()
  } {
    case conn: HttpURLConnection =>
      //User-Agent:Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36
      agent.foreach(agent=>conn.setRequestProperty("User-Agent", agent))
      conn.setRequestMethod("HEAD")
      conn.getInputStream
      val len = conn.getContentLengthLong()
      if (len < 0) throw new RuntimeException("Invalid length " + len + " received!")
      len
    case conn: FileURLConnection =>
      //conn.getInputStream
      val len = conn.getContentLengthLong()
      if (len < 0) throw new RuntimeException("Invalid length " + len + " received!")
      len
  }
  protected override def unsafeToInputStream: InputStream = {
    val conn = url.openConnection()
    agent.foreach(agent=>conn.setRequestProperty("User-Agent", agent))
    conn.getInputStream
  }

  def withAgent(newAgent: String) = this.copy(agent = Some(newAgent))
  def withoutAgent = this.copy(agent = None)
}