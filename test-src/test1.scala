import r._
import r.builtins.{Array=>_,_}
import r.nodes._
import r.nodes.truffle.{BaseR, RNode}
import com.oracle.truffle.api.frame._;

import org.antlr.runtime._

import java.io._

import r.data._

object Test1 {

  def main(args: Array[String]): Unit = {

    val cf = new CallFactory("foobar", Array("e"), Array("e")) { 
      def create(call: ASTNode, names: Array[RSymbol], exprs: Array[RNode]): RNode = {
        new BaseR(call) { 
          def execute(frame: Frame): AnyRef = RInt.RIntFactory.getScalar(42)
        } 
      }
    }

    Primitives.add(cf)

    val res = RContext.eval(RContext.parseFile(new ANTLRInputStream(new ByteArrayInputStream("foobar(x+2)".getBytes))))

    println(res.pretty)
    
  }
}


object Test2 {

  def eval(e: ASTNode, frame: Frame) = e match {
    case e: FunctionCall => 
      println("unknown f: "+e.getName + " / " + e); 
      println("unknown f: "+e.getArgs.first.getValue) //foreach(_.getValue)); 
      new RLanguage(e)
    case _ => println("unknown: "+e); new RLanguage(e) //RInt.RIntFactory.getScalar(42)
  }


  def main(args: Array[String]): Unit = {

    val cf = new CallFactory("foobar", Array("e"), Array("e")) { 
      def create(call: ASTNode, names: Array[RSymbol], exprs: Array[RNode]): RNode = {
        check(call, names, exprs)
        val expr = exprs(0)
        val ast = expr.getAST()

        val ast1:AnyRef = ast // apparently ASTNode member fields are reassigned -- don't make it look like one!
        new BaseR(call) { 
          def execute(frame: Frame): AnyRef = {
            val ast = ast1.asInstanceOf[ASTNode]
            println("dyn "+ast1 + "/"+System.identityHashCode(ast1))
            eval(ast, null)
          }
        } 
      }
    }

    Primitives.add(cf)

    val res = RContext.eval(RContext.parseFile(
        new ANTLRInputStream(new ByteArrayInputStream("5+5; foobar(x+2)".getBytes))))

    println(res.pretty)
    
  }
}
