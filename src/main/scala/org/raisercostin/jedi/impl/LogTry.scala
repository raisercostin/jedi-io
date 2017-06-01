package org.raisercostin.jedi.impl

import scala.util.{ Try, Success, Failure }

object LogTry {
  implicit class LogTry[A](res: Try[A]) {
    def log() = res match {
      case Success(s) =>
        res
      case Failure(f) =>
        println(res); res
    }
  }
}