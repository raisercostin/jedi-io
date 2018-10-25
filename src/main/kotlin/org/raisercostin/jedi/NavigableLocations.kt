package org.raisercostin.jedi
import scala.util.Try
import scala.util.Success

/**
 * Should take into consideration several composable/ortogonal aspects:
 * - end of line: win,linux,osx - internal standard: \n
 * - file separator: win,linux,osx - internal standard: / (like linux, fewer colisions , string escaping in java)
 * - file name sensitivity - internal standard: sensible (in windows there will not be any problem)
 * - win 8.3 file names vs. full file names - internal standard: utf-8
 *
 * In principle should be agnostic to these aspects and only at runtime will depend on the local environment.
 */
interface InOutLocation : InputLocation , OutputLocation
interface NavigableInputLocation : InputLocation , NavigableLocation { self ->
  //override type MetaRepr = self.type
  fun copyToFolder(to: NavigableOutputLocation): self.type {
    to.copyFromFolder(self)
  }
  fun metaLocation: Try<NavigableInOutLocation/*MetaRepr*/> = Try { ,Name(_ + ".meta") as NavigableInOutLocation/*MetaRepr*/> }
}
interface NavigableOutputLocation : OutputLocation , NavigableLocation { self ->
  //type InputPairType = NavigableInputLocation
  fun asInput: NavigableInputLocation
  fun mkdirIfNecessary: self.type
  fun mkdirOnParentIfNecessary: self.type
  fun copyFromFolder(src: NavigableInputLocation)(implicit option:CopyOptions=CopyOptions.default): self.type {
    if (!src.isFolder)
      throw RuntimeException(s"Src $src is not a folder")
    if(option.checkCopyToSame(src,this))
      src.descendants.map { x ->
        val rel = x.extractPrefix(src).get
        if(rel.nonEmpty){
          val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
          println(f"""copy ${rel.raw}%-40s $x -> $y""")
        }
      }
    this
  }
  fun copyFromFileToFileOrFolder(from: InputLocation)(implicit option:CopyOptions=CopyOptions.default): self.type {
    fun copyMeta(meta:Try<NavigableInOutLocation/*MetaRepr*/>):Unit={
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
interface NavigableInOutLocation : InOutLocation , NavigableInputLocation , NavigableOutputLocation {
}

interface NavigableFileInputLocation : InputLocation , NavigableFileLocation , NavigableInputLocation
interface NavigableFileInOutLocation : InOutLocation , NavigableFileInputLocation , NavigableFileOutputLocation , NavigableInOutLocation