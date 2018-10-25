package org.raisercostin.jedi.impl

object Predef2 {
  def requireArgNotNull(arg: =>AnyRef, name: =>String=""):Unit = require(arg!=null, "Parameter "+name+" should not be null!") 
  def requireNotNull(arg: =>AnyRef, message: =>String=""):Unit = require(arg!=null, message) 
}