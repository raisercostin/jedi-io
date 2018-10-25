package org.raisercostin.jedi

import java.io.InputStream
import java.time.Clock
import java.time.LocalDate

abstract data class CacheEntry(cache: NavigableFileInOutLocation) {
  fun cacheIt: Unit
}
interface CacheConfig {
  fun cacheFor(src: InputLocation): CacheEntry
}
object DefaultCacheConfig : TimeSensitiveEtagCachedEntry(Locations.temp.child("default-cache"))

data class EtagCacheConfig(cacheFolder: NavigableFileInOutLocation) : CacheConfig {
  fun cacheFor(origin: InputLocation): CacheEntry = CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).,BaseName(x -> x + "--etag-" + origin.etag)) {
    fun cacheIt: Unit {
      //since the name is computed based on etag is enough to check the existence of file
      //TODO maybe we should start to delete equivalent files that are older
      if (!cache.exists)
        cache.copyFrom(origin)
    }
  }
}
data class TimeSensitiveCachedEntry(cacheFolder: NavigableFileInOutLocation) : CacheConfig {
  fun cacheFor(origin: InputLocation): CacheEntry = CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).,BaseName(x -> x + "--date-" + LocalDate.now())) {
    fun cacheIt: Unit {
      if (!cache.exists)
        cache.copyFrom(origin)
    }
  }
}
/**Use etag if UrlLocation returns one non empty otherwise use date.*/
data class TimeSensitiveEtagCachedEntry(cacheFolder: NavigableFileInOutLocation) : CacheConfig {
  fun cacheFor(origin: InputLocation): CacheEntry = CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).,BaseName { x ->
    x +
      (if (origin.etag.isEmpty())
        "--date-" + LocalDate.now()
      else
        "--etag-" + origin.etag)
  }) {
    fun cacheIt: Unit {
      if (!cache.exists)
        cache.copyFrom(origin)
    }
  }
}

//TODO CachedLocation when printed should show the temporary file 
data class CachedLocation<O : InputLocation>(cacheConfig: CacheConfig, origin: O) : FileLocation { self ->

  private lazy val cacheEntry: CacheEntry = cacheConfig.cacheFor(origin)
  fun cache ()= cacheEntry.cache
  //def cache: InOutLocation = cacheConfig.cacheFor(origin)
  override fun build(path: String): self.type = origin when {
    n: NavigableLocation ->
      CachedLocation(cacheConfig, n.build(path))
    else ->
      //TODO bug since origin is not used?
      FileLocation(path)
  }
  override fun childName(child: String): String = toPath.resolve(checkedChild(child)).toFile.getAbsolutePath
  //override fun ,Append: Repr = self.copy(append = true)
  override fun unsafeToInputStream: InputStream {
    flush
    super.unsafeToInputStream
  }
  /**Force caching.*/
  //TODO as async
  fun flush: this.type {
    cacheEntry.cacheIt
    this
  }

  override fun append: Boolean = cache.append
  fun fileFullPath: String = cache.nameAndBefore
  fun ,Append = ??? //this.copy(cache = cache.,Append)
}