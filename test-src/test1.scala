/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/agpl.html.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
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
