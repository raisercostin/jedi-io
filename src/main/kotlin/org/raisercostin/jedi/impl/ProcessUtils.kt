package org.raisercostin.jedi.impl

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.util.Properties

object ProcessUtils : SlfLogger{
    /**Inspired from here: http://winaero.com/blog/symbolic-link-in-windows-10 */
  fun executeWindows(command: List<String>):Try<Unit> {
    require(Properties.isWin)
    SlfLogger.logger.info("Execute on windows shell: <{}>", command.mkString("\"", "\" \"", "\""))
    import sys.process._
    var reason = StringBuilder()
    val processLogger = ProcessLogger(out -> logger.info(out), {err -> reason.append(err).append("\n")})
    val result = Seq("cmd", "/C") ++ command ! (processLogger)
    if(result != 0 ){
      Failure(new RuntimeException(s"Running ${command} failed , responseCode=${result}. Reason: ${reason}"))
    }else
      Success({})
  }
}