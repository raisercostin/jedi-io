package org.raisercostin.jedi

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try
import scala.annotation.tailrec
import java.nio.file.Files
import java.nio.file.CopyOption

interface NavigableFileOutputLocation : OutputLocation , NavigableFileLocation , NavigableOutputLocation{ self ->
  fun mkdirOnParentIfNecessary: self.type {
    parent.mkdirIfNecessary
    this
  }
  fun deleteOrRenameIfExists: self.type {
    Try { deleteIfExists }.recover { else -> renamedIfExists }.get
  }
  fun asInput: NavigableFileInputLocation
  @tailrec fun moveToRenamedIfExists(dest: NavigableFileOutputLocation): this.type {
    try{
      FileUtils.moveFile(toFile, dest.toFile)
      this
    }catch{
      else : Exception ->
        moveToRenamedIfExists(dest.renamedIfExists)
    }
  }
  private fun backupExistingOneAndReturnBackup(backupEmptyFolderToo:Boolean = true): self.type {
    //new name should never exists
    val newName:NavigableFileLocation = renamedIfExists(false)
    if (!newName.equals(this)){
      if(!backupEmptyFolderToo && newName.exists && newName.isEmptyFolder)
        newName.delete
      renameTo(newName)
    }else
      this
  }
  fun backupExistingOne(onBackup: self.type -> Unit, backupIfEmpty:Boolean = true): self.type {
    onBackup(backupExistingOneAndReturnBackup(backupIfEmpty))
    this
  }
  fun backupExistingOne(backupEmptyFolderToo:Boolean): self.type {
    backupExistingOneAndReturnBackup(backupEmptyFolderToo)
    this
  }
  fun backupExistingOne: self.type = backupExistingOne(true)

  fun renameTo<T : FileAbsoluteBaseLocation>(newName: T): T {
    if(isSymlink)
      Files.move(toPath,newName.toPath)
    else
      FileUtils.moveFile(toFile, newName.toFile)
    newName
  }
}