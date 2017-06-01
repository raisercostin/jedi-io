package org.raisercostin.jedi.impl

import scala.util.{ Try, Success, Failure }

object LogTry {
  implicit class LogTry[A](res: Try[A]) extends SlfLogger{
    def log() = res match {
      case Success(s) =>
        res
      case Failure(f) =>
        logger.info("Failure via LogTry.",f) 
        res
    }
  }
}