package org.raisercostin.jedi

import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import scala.util.Try
import java.util.regex.Pattern.Loop
import org.raisercostin.jedi.impl.MimeTypeDetectors

package test {
  object Logger{
    val LOG = org.slf4j.LoggerFactory.getLogger(MimeTypeDetectors.getClass)
  }
  //How can I define an inherited operation that returns the current type (and not current type instance) in scala?
  //I already tried to return this.type but in case I need to create a new instance will not work.
  //object Test {
  //  def main(args: Array[String]) {
  trait A { self =>
    type Return //= A
    //return current instance type
    def op: this.type = { log("opA"); this }
    //return current type (but must be overriden)
    def op2: A = { log("op2A"); this }
    //return current type (without the need to override)
    def op3[T >: A]: T = { log("opA"); this }
    //def op4[T <: A]: T = { log("opA"); this }
    //def op5: Return = { log("opA"); this }
    def op6: self.type = { log("opA"); self }

    def log(msg: String) = Logger.LOG.debug(msg)
  }
  case class B() extends A { self =>
    def doB: Unit = log("doB")

    override def op: this.type = {
      log("opB");
      this
      //new B() - compilation error: type mismatch;  found: B  required: B.this.type
    }
    override def op2: B = {
      log("op2B");
      new B()
      //here it works
    }
    override def op3[T >: B]: B = {
      log("op3B");
      //here it works
      new B()
    }
    type Return = B
    def op5: Return = { log("opA"); this }
    override def op6: self.type = { log("op6B"); self }
  }
  //Inherits both op2 and op3 from A but op2 and op3 need to return a C type
  case class C() extends A {
    type Return = C
    def doC: Unit = log("doC")
    def op5: Return = { log("opA"); this }
  }
  //  case class DCase[Self <: D[Self]]() extends A { self: Self =>
  //  }
  trait D[Self <: D[Self]] extends A { self: Self =>
    def doD: Unit = log("doD")
    override def op6: self.type = { log("D.op6"); self }
  }
  case class DCase2() extends D[DCase2]
  case class E() extends A { self =>
    def doE: Unit = log("doE")
    override def op6: self.type = { log("E.op6"); self }
  }
  /**@see TraversableLike and Traversable*/
  trait FLike[+Repr] extends A {
    def op7: Repr = { log("FLike.op7"); this.asInstanceOf[Repr] }
  }
  case class F() extends FLike[F] {
    def doF: Unit = log("F.doF")
  }
}

@RunWith(classOf[JUnitRunner])
class InheritanceStudyTest extends FunSuite {
  import org.scalatest.Matchers._
  import org.raisercostin.jedi.test._
  def log(msg: String) = Logger.LOG.debug(msg)
  test("inheritance") {
    log("---op")
    B().op.doB
    C().op.doC

    log("---op2")
    B().op2.doB
    //C().op2.doC //compilation error => value doC is not a member of A

    log("---op3")
    B().op3.doB
    //C().op3.doC //compilation error => value doC is not a member of type parameter T

    log("---op5")
    B().op5.doB
    C().op5.doC

    log("---op6")
    B().op6.doB
    C().op6.doC
    DCase2().op6.doD
    E().op6.doE

    F().op7.doF
    List(1, 2, 3).filter(_ > 1)

    /**
     * Output:
     * opB
     * doB
     * opA
     * doC
     * op2B
     * doB
     * op3B
     * doB
     * opA
     * doB
     * opA
     * doC
     * op6
     * op6B
     * doB
     * opA
     * doC
     */
    //  }
    //}
    //Test.main(Array())
  }
}
