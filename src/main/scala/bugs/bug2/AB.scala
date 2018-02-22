package bugs.bug2

trait AB { self =>
  type Repr = self.type
  def foo:self.type = ???
}
