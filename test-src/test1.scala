import r._
import r.data._
import r.data.internal._
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

  import ppl.dsl.optiml.OptiMLApplication
  import scala.virtualization.lms.common.StaticData
  import ppl.delite.framework.datastructures.DeliteArray

  trait Eval extends OptiMLApplication with StaticData {
    type Env = Map[RSymbol,Rep[Any]]
    var env: Env = Map.empty

    def infix_tpe[T](x:Rep[T]): Manifest[_]

    //def fun[A,B](f: Rep[A]=>Rep[B]):Rep[A=>B]
    def nuf[A,B](f: Rep[A=>B]):Rep[A]=>Rep[B]

    def liftValue(v: Any): Rep[Any] = v match {
      case v: RString => unit(v.getString(0))
      case v: RInt    => unit(v.getInt(0))
      case v: DoubleImpl => densevector_obj_fromarray[Double](
        staticData(v.getContent).asInstanceOf[Rep[DeliteArray[Double]]], true)
      case v: ScalarDoubleImpl => unit(v.getDouble(0))
    }

    def evalFun[A:Manifest,B:Manifest](e: ASTNode, frame: Frame): Rep[A] => Rep[B] = e match {
      case e: Function => 
        { x: Rep[A] => 
          val ex = RContext.createRootNode(e,null).execute(frame)
          ex match {
            case ex: ClosureImpl =>
              val env0 = env
              // TODO: closure env?
              env = env.updated(ex.function.paramNames()(0),x)
              val res = eval(ex.function.body.getAST, ex.enclosingFrame)
              env = env0
              res.asInstanceOf[Rep[B]]
          }
        }
    }

    def eval(e: ASTNode, frame: Frame): Rep[Any] = e match {
      case e: Constant => liftValue(e.getValue )
      case e: SimpleAssignVariable => 
        val lhs = e.getSymbol
        val rhs = eval(e.getExpr,frame)
        env = env.updated(lhs,rhs)
      case e: SimpleAccessVariable => 
        val lhs = e.getSymbol
        env.getOrElse(lhs, {
          val ex = RContext.createRootNode(e,null).execute(frame)
          scala.Console.println(ex)
          liftValue(ex)
        })
      case e: Sequence => 
        e.getExprs.map(g => eval(g,frame)).last
      case e: FunctionCall => 
        e.getName.toString match {
          case "map" =>
            val v = eval(e.getArgs.getNode(0), frame).asInstanceOf[Rep[DenseVector[Double]]]
            val f = evalFun[Double,Double](e.getArgs.getNode(1), frame)
            v.map(f)
          case _ =>
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

            class Foo extends MainDeliteRunner with Eval {
              //case class Lam[A,B](f: Rep[A]=>Rep[B]) extends Def[A=>B]
              //override def fun[A:Manifest,B:Manifest](f: Rep[A]=>Rep[B]):Rep[A=>B] = Lam(f)
              def nuf[A,B](f: Rep[A=>B]):Rep[A]=>Rep[B] = f match { case Def(Lambda(f,_,_)) => f}

              def infix_tpe[T](x:Rep[T]): Manifest[_] = x.tp              
            }

            val runner = new Foo
            runner.program = (x => runner.eval(ast, null))
            DeliteRunner.compileAndTest(runner)
            RInt.RIntFactory.getScalar(0)
          }
        } 
      }
    }

    Primitives.add(cf)

    val prog = """
    v0 <- c(1,2,3,4)
    res <- Delite({
        pprint(v0)
        v1 <- map(v0,function(x) 2*x)
        pprint(v1)
        v2 <- Vector.rand(4)
        pprint(v2)
    })
    res
    """


    val res = RContext.eval(RContext.parseFile(
        new ANTLRInputStream(new ByteArrayInputStream(prog.getBytes))))

    println(res.pretty)
    
  }
}
