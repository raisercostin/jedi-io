package org.raisercostin.jedi.impl

object Predef2 {
  fun requireArgNotNull(arg: ->AnyRef, name: ->String=""):Unit = require(arg!=null, "Parameter "+name+" should not be null!") 
  fun requireNotNull(arg: ->AnyRef, message: ->String=""):Unit = require(arg!=null, message) 
}