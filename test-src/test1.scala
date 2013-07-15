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
