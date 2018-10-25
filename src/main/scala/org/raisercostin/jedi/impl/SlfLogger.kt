package org.raisercostin.jedi.impl

trait SlfLogger {self =>
    lazy val logger:org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(self.getClass);
}

object SlfLogger extends SlfLogger