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
    case v: ScalarIntImpl => unit(v.getInt(0))
    case v: ScalarDoubleImpl => unit(v.getDouble(0))
    case v: IntImpl => 
      val data = staticData(v.getContent).asInstanceOf[Rep[DeliteArray[Int]]]
      densevector_obj_fromarray(data, true)
    case v: DoubleImpl => 
      val data = staticData(v.getContent).asInstanceOf[Rep[DeliteArray[Double]]]
      densevector_obj_fromarray(data, true)
  }

  def convertBack(x: Any): AnyRef = x match {
    case x: String => RString.RStringFactory.getScalar(x)
    case x: Int => RInt.RIntFactory.getScalar(x)
    case x: Double => RDouble.RDoubleFactory.getScalar(x)
    // struct classes are generated on the fly. we cannot acces them yet.
    case x if x.getClass.getName == "generated.scala.DenseVectorInt" => RInt.RIntFactory.getFor(x.asInstanceOf[{val _data: Array[Int]}]._data)
    case x if x.getClass.getName == "generated.scala.DenseVectorDouble" => RDouble.RDoubleFactory.getFor(x.asInstanceOf[{val _data: Array[Double]}]._data)
//    case x: generated.scala.DenseVectorDouble => RDouble.RDoubleFactory.getFor(x._data)
    case () => RInt.RIntFactory.getScalar(0)
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
        liftValue(ex)
      })
    case e: Sequence => 
      e.getExprs.map(g => eval(g,frame)).last
    case e: Add => 
      val lhs = eval(e.getLHS,frame)
      val rhs = eval(e.getRHS,frame)
      val D = manifest[Double]
      val VD = manifest[DenseVector[Double]]
      (lhs.tpe,rhs.tpe) match {
        case (D,D) => lhs.asInstanceOf[Rep[Double]] + rhs.asInstanceOf[Rep[Double]]
        case (VD,VD) => lhs.asInstanceOf[Rep[DenseVector[Double]]] + rhs.asInstanceOf[Rep[DenseVector[Double]]]
        case (VD,D) => lhs.asInstanceOf[Rep[DenseVector[Double]]] + rhs.asInstanceOf[Rep[Double]]
      }
    case e: Mult => 
      val lhs = eval(e.getLHS,frame)
      val rhs = eval(e.getRHS,frame)
      val D = manifest[Double]
      val VD = manifest[DenseVector[Double]]
      (lhs.tpe,rhs.tpe) match {
        case (D,D) => lhs.asInstanceOf[Rep[Double]] * rhs.asInstanceOf[Rep[Double]]
        case (VD,VD) => lhs.asInstanceOf[Rep[DenseVector[Double]]] * rhs.asInstanceOf[Rep[DenseVector[Double]]]
        case (VD,D) => lhs.asInstanceOf[Rep[DenseVector[Double]]] * rhs.asInstanceOf[Rep[Double]]
      }
    case e: Div => 
      val lhs = eval(e.getLHS,frame)
      val rhs = eval(e.getRHS,frame)
      val D = manifest[Double]
      val VD = manifest[DenseVector[Double]]
      (lhs.tpe,rhs.tpe) match {
        case (D,D) => lhs.asInstanceOf[Rep[Double]] / rhs.asInstanceOf[Rep[Double]]
        case (VD,VD) => lhs.asInstanceOf[Rep[DenseVector[Double]]] / rhs.asInstanceOf[Rep[DenseVector[Double]]]
        case (VD,D) => lhs.asInstanceOf[Rep[DenseVector[Double]]] / rhs.asInstanceOf[Rep[Double]]
      }
    case e: Colon => 
      val lhs = eval(e.getLHS,frame)
      val rhs = eval(e.getRHS,frame)
      val D = manifest[Double]
      val VD = manifest[DenseVector[Double]]
      (lhs.tpe,rhs.tpe) match {
        case (D,D) => 
          indexvector_range(lhs.asInstanceOf[Rep[Double]].toInt,rhs.asInstanceOf[Rep[Double]].toInt+1).toDouble
      }
    case e: FunctionCall => 
      e.getName.toString match {
        case "map" | "sapply" =>
          val v = eval(e.getArgs.getNode(0), frame).asInstanceOf[Rep[DenseVector[Double]]]
          val f = evalFun[Double,Double](e.getArgs.getNode(1), frame)
          v.map(f)
        case "sum" =>
          val v = eval(e.getArgs.getNode(0), frame).asInstanceOf[Rep[DenseVector[Double]]]
          v.sum
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

class EvalRunner extends MainDeliteRunner with Eval {
  //case class Lam[A,B](f: Rep[A]=>Rep[B]) extends Def[A=>B]
  //override def fun[A:Manifest,B:Manifest](f: Rep[A]=>Rep[B]):Rep[A=>B] = Lam(f)
  def nuf[A,B](f: Rep[A=>B]):Rep[A]=>Rep[B] = f match { case Def(Lambda(f,_,_)) => f }

  def infix_tpe[T](x:Rep[T]): Manifest[_] = x.tp

  val transport = new Array[Any](1)
  def setResult(x: Rep[Any]) = staticData(transport).update(0,x)
  def getResult: AnyRef = convertBack(transport(0))
}



object DeliteBridge {

  def install(): Unit = {
    installDelite()
    installTime()
  }

  // todo: Delitec(function (x) { })

  def installDelite(): Unit = {
    val cf = new CallFactory("Delite", Array("e"), Array("e")) {
      def create(call: ASTNode, names: Array[RSymbol], exprs: Array[RNode]): RNode = {
        check(call, names, exprs)
        val expr = exprs(0)
        val ast = expr.getAST()

        val ast1:AnyRef = ast // apparently ASTNode member fields are reassigned -- don't make it look like one!
        new BaseR(call) { 
          def execute(frame: Frame): AnyRef = {
            val ast = ast1.asInstanceOf[ASTNode]
            println("delite input: "+ast)

            val runner = new EvalRunner {}
            runner.program = { x => 
              val res = runner.eval(ast, null)
              runner.setResult(res)
            }
            DeliteRunner.compileAndTest(runner)
            runner.getResult
          }
        } 
      }
    }

    Primitives.add(cf)
  }
  def installTime(): Unit = {
    val cf = new CallFactory("system.time", Array("e"), Array("e")) {
      def create(call: ASTNode, names: Array[RSymbol], exprs: Array[RNode]): RNode = {
        check(call, names, exprs)
        val expr = exprs(0)
        //val ast = expr.getAST()

        //val ast1:AnyRef = ast // apparently ASTNode member fields are reassigned -- don't make it look like one!
        new BaseR(call) { 
          def execute(frame: Frame): AnyRef = {
            val t0 = System.currentTimeMillis()
            val res = expr.execute(frame)
            val t1 = System.currentTimeMillis()
            println("elapsed: " + ((t1-t0)/1000.0) + "s")
            res
          }
        }
      }
    }

    Primitives.add(cf)
  }
}
