package b {
  
  //import org.raisercostin.jedi.Locations
  
  object SimpleScalaObject {
    //Locations.logger
    def foo:String = ???
  }
}

package eclipseScalaBug{
  import b.SimpleScalaObject
  
  object test{
    SimpleScalaObject.foo
  }
}