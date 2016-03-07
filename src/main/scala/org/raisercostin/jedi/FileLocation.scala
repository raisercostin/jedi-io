package org.raisercostin.jedi

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Try
import org.apache.commons.io.FileUtils
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.slf4j.LoggerFactory
trait FileLocationLike extends NavigableInOutLocation { self =>
  override type Repr = self.type
  def fileFullPath: String
  def append: Boolean
  override def parentName: String = toFile.getParentFile.getAbsolutePath
  def raw = fileFullPath
  def asInput: NavigableInputLocation = self
  lazy val toFile: File = new File(fileFullPath)
  override def toPath: Path = Paths.get(fileFullPath)
  protected override def unsafeToInputStream: InputStream = new FileInputStream(toFile)
  //should not throw exception but return Try?
  def checkedChild(child: String): String = { require(!child.endsWith(" "), "Child [" + child + "] has trailing spaces"); child }
  //import org.raisercostin.util.MimeTypesUtils2
  def asFile: Repr = self
  def renamed(renamer: String => String):Try[Repr] = Try{
    val newName = renamer(baseName)
    if (newName == baseName) {
      //p rintln(s"ignore [${absolute}] to [${absolute}]")
      this
    } else {
      val dest = parent.child(withExtension2(newName, extension))
      //p rintln(s"move [${absolute}] to [${dest.absolute}]")
      FileUtils.moveFile(toFile, dest.toFile)
      dest
    }
  }
  //TODO replace by stream (future)
  def watch(listener: FileLocation => Unit):FileMonitor = {
        val observer = new FileAlterationObserver(toFile);
        val pollingInterval = 1000 //millis
        val monitor = new FileAlterationMonitor(pollingInterval);
        val fileListener = new FileAlterationListenerAdaptor() {
          override def onFileCreate(file:File) = {
            val location = Locations.file(file)
            try{
              listener(location)
            }catch{
              case e:Throwable =>
                LoggerFactory.getLogger(classOf[FileLocation]).error(s"Processing of [$location] failed.",e)
            }
          }
        }
        observer.addListener(fileListener)
        monitor.addObserver(observer)
        monitor.start()
        FileMonitor(monitor)
  }
}
case class FileMonitor(private val monitor:FileAlterationMonitor){
  def stop() = monitor.stop()
}
case class FileLocation(fileFullPath: String, append: Boolean = false) extends FileLocationLike {self=>
  override type Repr = self.type
  override def parent: Repr = new FileLocation(parentName)
  override def child(child: String): Repr = new FileLocation(toPath.resolve(checkedChild(child)).toFile.getAbsolutePath)
  override def withAppend: Repr = self.copy(append = true)
}