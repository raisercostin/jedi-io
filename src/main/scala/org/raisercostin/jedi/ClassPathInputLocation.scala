package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.util.Failure
import scala.util.Try
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.Predef2

/**
 * @see http://www.thinkplexx.com/learn/howto/java/system/java-resource-loading-explained-absolute-and-relative-names-difference-between-classloader-and-class-resource-loading
 */
trait ClassPathInputLocation extends NavigableFileInputLocation { self =>
  def initialResourcePath: String
  def raw = initialResourcePath
  import ClassPathInputLocation._
  val resourcePath = initialResourcePath.stripPrefix("/")
  val resource = {
    val res = getSpecialClassLoader.getResource(resourcePath);
    Predef2.requireNotNull(res, s"Couldn't get a stream from $self");
    res
  }
  override def toUrl: java.net.URL = resource
  override def exists = true //resource != null from constructor
  override def absolute: String = toUrl.toURI().getPath().stripPrefix("/")
  //Try{toFile.getAbsolutePath()}.recover{case e:Throwable => Option(toUrl).map(_.toExternalForm).getOrElse("unfound classpath://" + resourcePath) }.get
  def toFile: File = Try { new File(toUrl.toURI()) }.recoverWith { case e: Throwable => Failure(new RuntimeException("Couldn't get file from " + self, e)) }.get
  override def unsafeToInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  ///def toWrite = Locations.file(toFile.getAbsolutePath)
  override def parentName = {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  def asFile: FileLocation = Locations.file(toFile)
  def asUrl: UrlLocation = Locations.url("file:" + absolute)
  def build(path: String): Repr = ClassPathInputLocation(path)
}
object ClassPathInputLocation {
  def apply(initialResourcePath: String) = ClassPathInputLocationImpl(initialResourcePath)
  private def getDefaultClassLoader(): ClassLoader = {
    Try { Thread.currentThread().getContextClassLoader }.toOption.getOrElse(classOf[System].getClassLoader)
  }
  private def getSpecialClassLoader(): ClassLoader =
    (Option(classOf[ClassPathInputLocation].getClassLoader)).orElse(Option(classOf[ClassLoader].getClassLoader)).get
}
case class ClassPathInputLocationImpl(initialResourcePath: String) extends ClassPathInputLocation {
  Predef2.requireArgNotNull(initialResourcePath, "initialResourcePath")
  override def toString = s"ClassPathInputLocation($initialResourcePath)"
}