package org.raisercostin.jedi

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import scala.annotation.tailrec
import java.nio.file.Files
import java.nio.file.CopyOption

trait NavigableFileOutputLocation extends OutputLocation with NavigableFileLocation with NavigableOutputLocation{ self =>
  def mkdirOnParentIfNecessary: self.type = {
    parent.mkdirIfNecessary
    this
  }
  def deleteOrRenameIfExists: self.type = {
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
  private def backupExistingOneAndReturnBackup(backupEmptyFolderToo:Boolean = true): self.type = {
    //new name should never exists
    val newName:NavigableFileLocation = renamedIfExists(false)
    if (!newName.equals(this)){
      if(!backupEmptyFolderToo && newName.exists && newName.isEmptyFolder)
        newName.delete
      renameTo(newName)
    }else
      this
  }
  def backupExistingOne(onBackup: self.type => Unit, backupIfEmpty:Boolean = true): self.type = {
    onBackup(backupExistingOneAndReturnBackup(backupIfEmpty))
    this
  }
  def backupExistingOne(backupEmptyFolderToo:Boolean): self.type = {
    backupExistingOneAndReturnBackup(backupEmptyFolderToo)
    this
  }
  def backupExistingOne: self.type = backupExistingOne(true)

  def renameTo[T <: FileAbsoluteBaseLocation](newName: T): T = {
    if(isSymlink)
      Files.move(toPath,newName.toPath)
    else
      FileUtils.moveFile(toFile, newName.toFile)
    newName
  }
}