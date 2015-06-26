package org.raisercostin.util.io
import org.scalatest._
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import scala.util.Try
import java.util.regex.Pattern.Loop

@RunWith(classOf[JUnitRunner])
class InheritanceStudyTest extends FunSuite {
  import org.scalatest.Matchers._

  test("inheritance") {
    //How can I define an inherited operation that returns the current type (and not current type instance) in scala?
    //I already tried to return this.type but in case I need to create a new instance will not work.
    //object Test {
    //  def main(args: Array[String]) {
    trait A {
      type Return //= A
      //return current instance type
      def op: this.type = { println("opA"); this }
      //return current type (but must be overriden)
      def op2: A = { println("op2A"); this }
      //return current type (without the need to override)
      def op3[T >: A]: T = { println("opA"); this }
      //def op4[T <: A]: T = { println("opA"); this }
      //def op5: Return = { println("opA"); this }
    }
    case class B() extends A {
      def doB: Unit = println("doB")

      override def op: this.type = {
        println("opB");
        this
        //new B() - compilation error: type mismatch;  found: B  required: B.this.type
      }
      override def op2: B = {
        println("op2B");
        new B()
        //here it works
      }
      override def op3[T >: B]: B = {
        println("op3B");
        //here it works
        new B()
      }
      type Return = B
      def op5: Return = { println("opA"); this }
    }
    //Inherits both op2 and op3 from A but op2 and op3 need to return a C type
    case class C() extends A {
      type Return = C
      def doC: Unit = println("doC")
      def op5: Return = { println("opA"); this }
    }
    B().op.doB
    C().op.doC

    B().op2.doB
    //C().op2.doC //compilation error => value doC is not a member of A

    B().op3.doB
    //C().op3.doC //compilation error => value doC is not a member of type parameter T

    B().op5.doB
    C().op5.doC

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
     */
    //  }
    //}
    //Test.main(Array())
  }
}
