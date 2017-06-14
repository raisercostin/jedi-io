package org.raisercostin.jedi.impl

import scala.util.{Try,Failure,Success}

object ProcessUtils extends SlfLogger{
    /**Inspired from here: http://winaero.com/blog/symbolic-link-in-windows-10 */
  def executeWindows(command: Seq[String]):Try[Unit] = {
    SlfLogger.logger.info("Execute on windows shell: [{}]", command.mkString("\"", "\" \"", "\""))
    import sys.process._
    var reason = new StringBuilder()
    val processLogger = ProcessLogger(out => logger.info(out), {err => reason.append(err).append("\n")})
    val result = Seq("cmd", "/C") ++ command ! (processLogger)
    if(result != 0 ){
      Failure(new RuntimeException(s"Running ${command} failed with responseCode=${result}. Reason: ${reason}"))
    }else
      Success({})
  }
}