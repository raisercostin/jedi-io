package org.raisercostin.jedi.impl

interface SlfLogger {self ->
    lazy val logger:org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(self.getClass);
}

object SlfLogger : SlfLogger