package org.raisercostin.jedi

import java.io.InputStream

trait CachedLocation extends InputLocation with VersionedLocation {
  def origin: InputLocation with VersionedLocation
  override def unsafeToInputStream: InputStream = {
    val originVersion = origin.version
    val currentVersion = version
    if (currentVersion != originVersion)
      origin.unsafeToInputStream
    else
      super.unsafeToInputStream
  }
}