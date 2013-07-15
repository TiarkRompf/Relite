import r._
import r.data._
import r.builtins.{CallFactory,Primitives}
import r.nodes._
import r.nodes.truffle.{BaseR, RNode}
import com.oracle.truffle.api.frame._;

import org.antlr.runtime._

import java.io._

import scala.collection.JavaConversions._

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
        new ANTLRInputStream(new ByteArrayInputStream("5+5; foobar(Vector.rand(100))".getBytes))))

    println(res.pretty)
    
  }
}


object Test3 {

  trait Eval extends ppl.dsl.optiml.OptiMLApplication {
    type Env = Map[RSymbol,Rep[Any]]
    var env: Env = Map.empty

    def infix_tpe[T](x:Rep[T]): Manifest[_]

    def eval(e: ASTNode, frame: Frame): Rep[Any] = e match {
      case e: Constant => e.getValue match {
        case v: RString => unit(v.getString(0))
        case v: RInt    => unit(v.getInt(0))
        case v: RDouble => unit(v.getDouble(0))
      }
      case e: SimpleAssignVariable => 
        val lhs = e.getSymbol
        val rhs = eval(e.getExpr,frame)
        env = env.updated(lhs,rhs)
        unit(())
      case e: SimpleAccessVariable => 
        val lhs = e.getSymbol
        env(lhs)
      case e: Sequence => 
        e.getExprs.map(g => eval(g,frame)).last
      case e: FunctionCall => 
        val args = e.getArgs.map(g => eval(g.getValue,frame)).toList
        (e.getName.toString,args) match {
          case ("Vector.rand",(n:Rep[Double])::Nil) => 
            assert(n.tpe == manifest[Double])
            Vector.rand(n.toInt)
          case ("pprint",(v:Rep[DenseVector[Double]])::Nil) => 
            assert(v.tpe == manifest[DenseVector[Double]])
            v.pprint
          case s => println("unknown f: " + s + " / " + args.mkString(",")); 
        }
      case _ => 
        println("unknown: "+e+"/"+e.getClass); 
        new RLanguage(e) //RInt.RIntFactory.getScalar(42)
        42
    }
  }

  def main(args: Array[String]): Unit = {

    val cf = new CallFactory("Delite", Array("e"), Array("e")) {
      def create(call: ASTNode, names: Array[RSymbol], exprs: Array[RNode]): RNode = {
        check(call, names, exprs)
        val expr = exprs(0)
        val ast = expr.getAST()

        val ast1:AnyRef = ast // apparently ASTNode member fields are reassigned -- don't make it look like one!
        new BaseR(call) { 
          def execute(frame: Frame): AnyRef = {
            val ast = ast1.asInstanceOf[ASTNode]
            println("dyn "+ast + "/"+System.identityHashCode(ast1))

            val runner = new MainDeliteRunner with Eval {
              def infix_tpe[T](x:Rep[T]): Manifest[_] = x.tp
            }
            runner.program = (x => runner.eval(ast, null))
            DeliteRunner.compileAndTest(runner)
            RInt.RIntFactory.getScalar(0)
          }
        } 
      }
    }

    Primitives.add(cf)

    val prog = """res <- Delite({
        v <- Vector.rand(5)
        pprint(v)
    })
    res"""


    val res = RContext.eval(RContext.parseFile(
        new ANTLRInputStream(new ByteArrayInputStream(prog.getBytes))))

    println(res.pretty)
    
  }
}
