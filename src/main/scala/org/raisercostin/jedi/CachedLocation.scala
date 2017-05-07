package org.raisercostin.jedi

import java.io.InputStream

object CachedLocation{
  implicit val default = CacheParent(Locations.temp.child("default-cache"))
}

case class CacheParent(cache:NavigableInOutLocation){
  def cacheFor(src:InputLocation):NavigableInOutLocation = cache.child(src.slug)
}
case class CachedLocation[O<:InputLocation](cache:InOutLocation, origin: O) extends FileLocationLike { self =>
  override type Repr = self.type
  def build(path: String): Repr = new FileLocation(path)
  override def childName(child: String): String = toPath.resolve(checkedChild(child)).toFile.getAbsolutePath
  //override def withAppend: Repr = self.copy(append = true)
  override def unsafeToInputStream: InputStream = {
    val originVersion = origin.version
    val currentVersion = version
    if (currentVersion != originVersion){
      cache.copyFrom(origin)
      cache.unsafeToInputStream
    }else
      super.unsafeToInputStream
  }
  
  
  def append: Boolean = cache.append
  def fileFullPath: String = cache.nameAndBefore
  def withAppend = this.copy(cache = cache.withAppend)
}
