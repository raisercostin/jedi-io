package org.raisercostin.jedi

import java.io.File
import java.io.InputStream

import scala.util.Failure
import scala.util.Try
import org.raisercostin.jedi.impl.JediFileSystem
import org.raisercostin.jedi.impl.Predef2
import scala.util.Properties

/**
 * @see http://www.thinkplexx.com/learn/howto/java/system/java-resource-loading-explained-absolute-and-relative-names-difference-between-classloader-and-class-resource-loading
 */
trait ClassPathInputLocation extends NavigableFileInputLocation { self =>
  def initialResourcePath: String
  def raw = initialResourcePath
  import ClassPathInputLocation._
  val resourcePath = initialResourcePath.stripPrefix("/")
  val resource: java.net.URL = {
    val res = getSpecialClassLoader.getResource(resourcePath);
    Predef2.requireNotNull(res, s"Couldn't get a stream from $self");
    res
  }
  override def exists = true //resource != null - resource is always resolved

  override def toUrl: java.net.URL = resource
  override def absolute: String =
    {
      val x = toUrl.toURI().getPath()
      if (x == null)
        toUrl.toURI().toString()
      else if (Properties.isWin)
        x.stripPrefix("/")
      else
        x
    }
  //Cannot detect if is file or folder. See https://stackoverflow.com/questions/20105554/is-there-a-way-to-tell-if-a-classpath-resource-is-a-file-or-a-directory
  override def isFolder: Boolean = exists && hasMultipleChildrenDifferentThanThis
  override def isFile: Boolean = exists && !hasMultipleChildrenDifferentThanThis
  private def hasMultipleChildrenDifferentThanThis: Boolean =
    if (resource.toURI.isOpaque)
      false
    else
      list match {
        case x :: nil =>
          list.head.absolute != absolute.stripSuffix("/").stripSuffix("\\")
        case _ =>
          true
      }

  def toFile: File = Try { new File(resource.toURI()) }.recoverWith {
    case e: Throwable => Failure(new RuntimeException("Couldn't get file from " + self + " with url [" + resource.toURI() + "]. " + e.getMessage, e))
  }.get
  override def unsafeToInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  override def parentName = {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  def asFile: FileLocation = Locations.file(toFile)
  def asUrl: UrlLocation = Locations.url("file:" + absolute)
  override def build(path: String): self.type = ClassPathInputLocation(standard(path).stripPrefix(outerPrefix))
  def outerPrefix: String = absolute.stripSuffix(initialResourcePath)
  def innerPath: String = absolute.stripPrefix(outerPrefix)
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
