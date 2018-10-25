package org.raisercostin.jedi.impl

import scala.util.Try
import scala.util.Success
import scala.util.Failure

object ResourceUtil {
  val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(ResourceUtil.getClass);

  //from https://www.phdata.io/try-,-resources-in-scala
  fun cleanly<A, B>(resource: A)(cleanup: A -> Unit)(doWork: A -> B): Try<B> {
    try {
      Success(doWork(resource))
    } catch {
      e: Exception -> Failure(e)
    } finally {
      try {
        if (resource != null) {
          cleanup(resource)
        }
      } catch {
        e: Exception -> 
          log.error("Error on closing resource "+resource,e)
      }
    }
  }
}