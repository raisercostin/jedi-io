package org.raisercostin.jedi
import scala.util.Try
import scala.util.Success

/**
 * Should take into consideration several composable/ortogonal aspects:
 * - end of line: win,linux,osx - internal standard: \n
 * - file separator: win,linux,osx - internal standard: / (like linux, fewer colisions with string escaping in java)
 * - file name case sensitivity - internal standard: case sensible (in windows there will not be any problem)
 * - win 8.3 file names vs. full file names - internal standard: utf-8
 *
 * In principle should be agnostic to these aspects and only at runtime will depend on the local environment.
 */
trait InOutLocation extends InputLocation with OutputLocation
trait NavigableInputLocation extends InputLocation with NavigableLocation { self =>
  //override type MetaRepr = self.type
  def copyToFolder(to: NavigableOutputLocation): self.type = {
    to.copyFromFolder(self)
  }
  def metaLocation: Try[NavigableInOutLocation/*MetaRepr*/] = Try { withName(_ + ".meta").asInstanceOf[NavigableInOutLocation/*MetaRepr*/] }
}
trait NavigableOutputLocation extends OutputLocation with NavigableLocation { self =>
  //type InputPairType = NavigableInputLocation
  def asInput: NavigableInputLocation
  def mkdirIfNecessary: self.type
  def mkdirOnParentIfNecessary: self.type
  def copyFromFolder(src: NavigableInputLocation)(implicit option:CopyOptions=CopyOptions.simpleCopy): self.type = {
    if (!src.isFolder)
      throw new RuntimeException(s"Src $src is not a folder")
    if(option.checkCopyToSame(src,this))
      src.descendants.map { x =>
        val rel = x.extractPrefix(src).get
        val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
        println(f"""copy ${rel.raw}%-40s $x -> $y""")
      }
    this
  }
  def copyFromFileToFileOrFolder(from: InputLocation)(implicit option:CopyOptions=CopyOptions.simpleCopy): self.type = {
    def copyMeta(meta:Try[NavigableInOutLocation/*MetaRepr*/]):Unit={
      if (option.copyMeta){
        if(!option.optionalMeta || meta.isSuccess && meta.get.exists)
          meta.get.copyFromInputLocation(from.metaLocation.get)
        else
          option.monitor.warn("Optional meta "+meta+" doesn't exists. Ignored.")
      }
    }

    mkdirOnParentIfNecessary
    if (isFolder) {
      copyMeta(child(from.name).metaLocation)
      child(from.name).copyFromInputLocation(from)
    } else {
      copyMeta(metaLocation)
      copyFromInputLocation(from)
    }
  }
}
trait NavigableInOutLocation extends InOutLocation with NavigableInputLocation with NavigableOutputLocation {
}

trait NavigableFileInputLocation extends InputLocation with NavigableFileLocation with NavigableInputLocation
trait NavigableFileInOutLocation extends InOutLocation with NavigableFileInputLocation with NavigableFileOutputLocation with NavigableInOutLocation
