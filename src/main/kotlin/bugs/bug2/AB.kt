package bugs.bug2

interface AB { self ->
  type Repr = self.type
  fun foo:self.type = ???
}