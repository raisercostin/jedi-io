package org.raisercostin.jedi

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import scala.annotation.tailrec
import java.nio.file.Files
import java.nio.file.CopyOption

trait NavigableFileOutputLocation extends OutputLocation with NavigableFileLocation with NavigableOutputLocation{ self =>
  override type Repr = self.type

  def mkdirOnParentIfNecessary: Repr = {
    parent.mkdirIfNecessary
    this
  }
  def deleteOrRenameIfExists: Repr = {
    Try { deleteIfExists }.recover { case _ => renamedIfExists }.get
  }
  def asInput: NavigableFileInputLocation
  @tailrec final def moveToRenamedIfExists(dest: NavigableFileOutputLocation): this.type = {
    try{
      FileUtils.moveFile(toFile, dest.toFile)
      this
    }catch{
      case _ : Exception =>
        moveToRenamedIfExists(dest.renamedIfExists)
    }
  }
  private def backupExistingOneAndReturnBackup(backupEmptyFolderToo:Boolean = true): Repr = {
    val newName:NavigableFileLocation = renamedIfExists(backupEmptyFolderToo)
    if (!newName.equals(this)){
      if(!backupEmptyFolderToo && newName.exists && newName.isEmptyFolder)
        newName.delete
      renameTo(newName)
    }else
      this
  }
  def backupExistingOne(onBackup: Repr => Unit, backupIfEmpty:Boolean = true): Repr = {
    onBackup(backupExistingOneAndReturnBackup(backupIfEmpty))
    this
  }
  def backupExistingOne(backupEmptyFolderToo:Boolean): Repr = {
    backupExistingOneAndReturnBackup(backupEmptyFolderToo)
    this
  }
  def backupExistingOne: Repr = backupExistingOne(true)

  def renameTo[T <: FileAbsoluteBaseLocation](newName: T): T = {
    if(isSymlink)
      Files.move(toPath,newName.toPath)
    else
      FileUtils.moveFile(toFile, newName.toFile)
    newName
  }
}