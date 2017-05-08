package org.raisercostin.jedi

import java.io.InputStream

object CachedLocation {
  implicit val default = CacheParent(Locations.temp.child("default-cache"))
}

case class CacheParent(cache: NavigableInOutLocation) {
  def cacheFor(src: InputLocation): NavigableInOutLocation = cache.mkdirIfNecessary.child(src.slug).withBaseName(x => x + "--etag-" + src.etag)
}
case class CachedLocation[O <: InputLocation](cache: InOutLocation, origin: O) extends FileLocationLike { self =>
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
    //since the name is computed based on etag is enough to check the existence of file
    //val originVersion = origin.version
    if (!cache.exists) {
      //    val currentVersion = existingOption.map{_.version}.getOrElse("")
      //    if (currentVersion != originVersion) {
      cache.copyFrom(origin)
    }
    this
  }

  def append: Boolean = cache.append
  def fileFullPath: String = cache.nameAndBefore
  def withAppend = this.copy(cache = cache.withAppend)
}
