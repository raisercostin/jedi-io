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
interface ClassPathInputLocation : NavigableFileInputLocation { self ->
  fun initialResourcePath: String
  fun raw ()= initialResourcePath
  import ClassPathInputLocation._
  val resourcePath = initialResourcePath.stripPrefix("/")
  val resource: java.net.URL {
    val res = getSpecialClassLoader.getResource(resourcePath);
    Predef2.requireNotNull(res, s"Couldn't get a stream from $self");
    res
  }
  override fun exists ()= true //resource !()= null - resource is always resolved

  override fun toUrl: java.net.URL = resource
  override fun absolute: String =
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
  override fun isFolder: Boolean = exists && hasMultipleChildrenDifferentThanThis
  override fun isFile: Boolean = exists && !hasMultipleChildrenDifferentThanThis
  private fun hasMultipleChildrenDifferentThanThis: Boolean =
    if (resource.toURI.isOpaque)
      false
    else
      list when {
        x :: nil ->
          list.head.absolute != absolute.stripSuffix("/").stripSuffix("\\")
        else ->
          true
      }

  fun toFile: File = Try { File(resource.toURI()) }.recoverWith {
    e: Throwable -> Failure(new RuntimeException("Couldn't get file from " + self + " , url <" + resource.toURI() + ">. " + e.getMessage, e))
  }.get
  override fun unsafeToInputStream: InputStream = getSpecialClassLoader.getResourceAsStream(resourcePath)
  override fun parentName {
    val index = initialResourcePath.lastIndexOf("/")
    if (index == -1)
      ""
    else
      initialResourcePath.substring(0, index)
  }
  fun asFile: FileLocation = Locations.file(toFile)
  fun asUrl: UrlLocation = Locations.url("file:" + absolute)
  override fun build(path: String): self.type = ClassPathInputLocation(standard(path).stripPrefix(outerPrefix))
  fun outerPrefix: String = absolute.stripSuffix(initialResourcePath)
  fun innerPath: String = absolute.stripPrefix(outerPrefix)
}
object ClassPathInputLocation {
  fun apply(initialResourcePath: String) = ClassPathInputLocationImpl(initialResourcePath)
  private fun getDefaultClassLoader(): ClassLoader {
    Try { Thread.currentThread().getContextClassLoader }.toOption.getOrElse(classOf<System>.getClassLoader)
  }
  private fun getSpecialClassLoader(): ClassLoader =
    (Option(classOf<ClassPathInputLocation>.getClassLoader)).orElse(Option(classOf<ClassLoader>.getClassLoader)).get
}
data class ClassPathInputLocationImpl(initialResourcePath: String) : ClassPathInputLocation {
  Predef2.requireArgNotNull(initialResourcePath, "initialResourcePath")
  override fun toString ()= s"ClassPathInputLocation($initialResourcePath)"
}