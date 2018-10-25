package org.raisercostin.jedi

import java.io.FileInputStream
import java.io.InputStream

import scala.io.Codec.decoder2codec
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try

import org.apache.commons.io.IOUtils
import scala.util.Success


/**Location orthogonal dimension: Resolved/Unresolved: Can reach content/cannot.*/
interface LocationState
/**
 * Trait to mark if a location is not resolved to a file system. For example Relative locations or offline urls that
 * are not available in offline mode.
 */
interface UnresolvedLocationState : LocationState
/**If a location has access to its content and metadata is said to be resolved.*/
interface ResolvedLocationState : LocationState , IsFileOrFolder {
  //type MetaRepr : InputLocation

  /**The meta seen as another location.*/
  fun metaLocation: Try<NavigableInOutLocation/*MetaRepr*/>
  fun meta: Try<HierarchicalMultimap> = metaLocation.flatMap(_.existingOption.map(_.readContentAsText.map(x -> HierarchicalMultimap(x))).getOrElse(Success(HierarchicalMultimap())))
}

interface InputLocation : AbsoluteBaseLocation , ResolvedLocationState , VersionedLocation { self ->
  fun unsafeToInputStream: InputStream
  fun unsafeToInputStreamIfFile: InputStream {
    //Return the InputStream only if this is a file. Classpath folder is returning an InputStream , the list of the files.
    if (!isFile)
      throw RuntimeException("Cannot create inputStream since <" + this + "> is not a file!")
    unsafeToInputStream
  }
  fun unsafeToReader: java.io.Reader = java.io.InputStreamReader(unsafeToInputStreamIfFile, decoder)
  fun unsafeToSource: scala.io.BufferedSource = scala.io.Source.fromInputStream(unsafeToInputStreamIfFile)(decoder)
  fun bytes: Array<Byte> {
    //TODO implement
    ??? //IOUtils.readFully(x$1, x$2)
  }

  fun usingInputStream<T>(op: InputStream -> T): T = using(unsafeToInputStreamIfFile)(op)
  fun usingReader<T>(reader: java.io.Reader -> T): T = using(unsafeToReader)(reader)
  fun usingSource<T>(processor: scala.io.BufferedSource -> T): T = using(unsafeToSource)(processor)

  fun usingInputStreamAndContinue(op: InputStream -> Any): self.type {using(unsafeToInputStreamIfFile)(op);this}
  fun usingReaderAndContinue(reader: java.io.Reader -> Any): self.type {using(unsafeToReader)(reader);this}
  fun usingSourceAndContinue(processor: scala.io.BufferedSource -> Any): self.type {using(unsafeToSource)(processor);this}

  fun readLines: Iterable<String> = traverseLines.toIterable
  fun traverseLines: Traversable<String> = Traversable<String> {
    fun foreach<U>(f: String -> U): Unit {
      usingSource { x -> x.getLines().foreach(f) }
    }
  }

  fun copyToIfNotExists(dest: OutputLocation): self.type { dest.nonExistingOption.map(_.copyFrom(this)); this }
  fun copyTo(dest: OutputLocation)(implicit option: CopyOptions = CopyOptions.default): self.type { dest.copyFrom(self); this}

  fun readContent {
    // Read a file into a string
    //    import rapture._
    //    import core._, io._, net._, uri._, json._, codec._
    //    import encodings.`UTF-8`
    //    val src = uri"http://rapture.io/sample.json".slurp<Char>
    //existing(toSource).getLines mkString ("\n")
    usingReader { reader ->
      try { IOUtils.toString(reader) } catch { x: Throwable -> throw RuntimeException("While reading " + this, x) }
    }
  }
  fun readContentAsText: Try<String> =
    Try(readContent)
  //Try(existing(toSource).getLines mkString ("\n"))
  //def unzip: ZipInputLocation = ???
  fun unzip: ZipInputLocation = ZipInputLocation(this, None)
  fun cached(implicit cacheConfig: CacheConfig = DefaultCacheConfig): CachedLocation<this.type> = CachedLocation(cacheConfig, this)
  /**Sometimes we want the content to be available locally in the filesystem.*/
  fun asFileInputLocation: FileInputLocation = cached.flush
}