package relite

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

object Test3 {

  def main(args: Array[String]): Unit = {

    DeliteBridge.install()

    def test(prog: String): Unit = {
      val res = RContext.eval(RContext.parseFile(
          new ANTLRInputStream(new ByteArrayInputStream(prog.getBytes))))

      println(res.pretty)
    }

    test("""
    v0 <- c(1,2,3,4)
    Delite({
        pprint(v0)
        v1 <- map(v0,function(x) 2*x)
        v2 <- Vector.rand(4)
        v3 <- v1 + v2
        pprint(v1)
        pprint(v2)
        pprint(v3)
    })
    """)

    
  }
}
