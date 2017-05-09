package org.raisercostin.jedi

import java.io.InputStream
import java.time.Clock
import java.time.LocalDate

abstract case class CacheEntry(cache: NavigableInOutLocation) {
  def cacheIt: Unit
}
trait CacheConfig {
  def cacheFor(src: InputLocation): CacheEntry
}
object DefaultCacheConfig extends TimeSensitiveEtagCachedEntry(Locations.temp.child("default-cache"))

case class EtagCacheConfig(cacheFolder: NavigableInOutLocation) extends CacheConfig {
  def cacheFor(origin: InputLocation): CacheEntry = new CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).withBaseName(x => x + "--etag-" + origin.etag)) {
    def cacheIt: Unit = {
      //since the name is computed based on etag is enough to check the existence of file
      //TODO maybe we should start to delete equivalent files that are older
      if (!cache.exists)
        cache.copyFrom(origin)
    }
  }
}
case class TimeSensitiveCachedEntry(cacheFolder: NavigableInOutLocation) extends CacheConfig {
  def cacheFor(origin: InputLocation): CacheEntry = new CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).withBaseName(x => x + "--date-" + LocalDate.now())) {
    def cacheIt: Unit = {
      if (!cache.exists)
        cache.copyFrom(origin)
    }
  }
}
/**Use etag if UrlLocation returns one non empty otherwise use date.*/
case class TimeSensitiveEtagCachedEntry(cacheFolder: NavigableInOutLocation) extends CacheConfig {
  def cacheFor(origin: InputLocation): CacheEntry = new CacheEntry(cacheFolder.mkdirIfNecessary.child(origin.slug).withBaseName { x =>
    x +
      (if (origin.etag.isEmpty())
        "--date-" + LocalDate.now()
      else
        "--etag-" + origin.etag)
  }) {
    def cacheIt: Unit = {
      if (!cache.exists)
        cache.copyFrom(origin)
    }
  }
}

//TODO CachedLocation when printed should show the temporary file 
case class CachedLocation[O <: InputLocation](cacheConfig: CacheConfig, origin: O) extends FileLocationLike { self =>
  private lazy val cacheEntry: CacheEntry = cacheConfig.cacheFor(origin)
  def cache = cacheEntry.cache
  //def cache: InOutLocation = cacheConfig.cacheFor(origin)
  override type Repr = self.type
  def build(path: String): Repr = new FileLocation(path)
  override def childName(child: String): String = toPath.resolve(checkedChild(child)).toFile.getAbsolutePath
  //override def withAppend: Repr = self.copy(append = true)
  override def unsafeToInputStream: InputStream = {
    flush
    super.unsafeToInputStream
  }
  /**Force caching.*/
  //TODO as async
  def flush: this.type = {
    cacheEntry.cacheIt
    this
  }

  def append: Boolean = cache.append
  def fileFullPath: String = cache.nameAndBefore
  def withAppend = ??? //this.copy(cache = cache.withAppend)
}
