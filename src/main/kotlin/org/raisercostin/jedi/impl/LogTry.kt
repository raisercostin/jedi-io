package org.raisercostin.jedi.impl

import scala.util.Try
import scala.util.Success
import scala.util.Failure

object LogTry {
  implicit class LogTry<+A>(res: Try<A>) : SlfLogger{
    fun log():Try<A> = res when {
      Success(s) ->
        res
      Failure(f) ->
        logger.info("Failure via LogTry.",f)
        res
    }
  }
}