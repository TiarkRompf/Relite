package relite

import ppl.dsl.optiml.{OptiMLApplication, OptiMLApplicationRunner}

import ppl.delite.framework.DeliteApplication
import ppl.delite.framework.Config

import ppl.dsl.optiml.{OptiMLCodeGenScala,OptiMLCodeGenCuda,OptiMLExp}
import ppl.delite.framework.codegen.delite.{DeliteCodeGenPkg, TargetDelite}
import ppl.delite.framework.codegen.{Target}
import ppl.delite.framework.codegen.scala.{TargetScala}
import ppl.delite.framework.codegen.cuda.{TargetCuda}
import scala.virtualization.lms.internal.{GenericFatCodegen}

import scala.virtualization.lms.common._
import scala.collection.mutable
import scala.collection.mutable.{ ArrayBuffer, SynchronizedBuffer }

import java.io.{ Console => _, _ }
import java.io.{File,FileSystem}

class MainDeliteRunner extends DeliteTestRunner with OptiMLApplicationRunner /*with VariablesExpOpt*/ { self =>

  var program: Rep[Int] => Rep[Any] = { x => x } // crashes if we refer to myprog directly!! GRRR ...
  override def main(): Unit = {
    //val (arg,block) = reify0[Int,Int](program)
    // discard arg; hacked it to be a const ...
    //reflect[Unit](block) // ok??
    program(0)
  }

/*
  // mix in delite and lancet generators
  val scalaGen = new ScalaCodegen { val IR: self.type = self }//; Console.println("SCG"); allClass(this.getClass) }
  override def createCodegen() = new ScalaCodegen { val IR: self.type = self }
  override def getCodeGenPkg(t: Target{val IR: self.type}) : 
    GenericFatCodegen{val IR: self.type} = t match {
      case _:TargetScala => createCodegen()
      case _:TargetCuda => new CudaCodegen { val IR: self.type = self }
      case _ => super.getCodeGenPkg(t)
    }
  override lazy val deliteGenerator = new DeliteCodegen { 
    val IR : self.type = self;
    val generators = self.generators; 
    //Console.println("DCG")
    //allClass(this.getClass)
  }
  

  def remap[A](x: TypeRep[A]): String = scalaGen.remap(x.manif)
*/
}




// *** from delite test runner. call compileAndTest to run an app

object DeliteRunner {

  val MAGICDELIMETER = "!~x02$758209"

  val propFile = new File("delite.properties")
  if (!propFile.exists) throw new TestFailedException("could not find delite.properties", 3)
  val props = new java.util.Properties()
  props.load(new FileReader(propFile))

  // test parameters
  var verbose = props.getProperty("tests.verbose", "false").toBoolean
  var verboseDefs = props.getProperty("tests.verboseDefs", "false").toBoolean
  var threads = props.getProperty("tests.threads", "1")
  var cacheSyms = false /* NNOOOOOOOOOO!!!!!!!!!!!*/   //props.getProperty("tests.cacheSyms", "true").toBoolean
  var javaHome = new File(props.getProperty("java.home", ""))
  var scalaHome = new File(props.getProperty("scala.vanilla.home", ""))
  var runtimeClasses = new File(props.getProperty("runtime.classes", ""))
  var runtimeExternalProc = false // scalaHome and runtimeClasses only required if runtimeExternalProc is true. should this be configurable? or should we just remove execTestExternal?

  var javaBin = new File(javaHome, "bin/java")
  var scalaCompiler = new File(scalaHome, "lib/scala-compiler.jar")
  var scalaLibrary = new File(scalaHome, "lib/scala-library.jar")

  def validateParameters() {
    if (!javaHome.exists) throw new TestFailedException("java.home must be a valid path in delite.properties", 3)
    else if (!javaBin.exists) throw new TestFailedException("Could not find valid java installation in " + javaHome, 3)
    else if (runtimeExternalProc && !scalaHome.exists) throw new TestFailedException("scala.vanilla.home must be a valid path in delite.proeprties", 3)
    else if (runtimeExternalProc && (!scalaCompiler.exists || !scalaLibrary.exists)) throw new TestFailedException("Could not find valid scala installation in " + scalaHome, 3)
    else if (runtimeExternalProc && !runtimeClasses.exists) throw new TestFailedException("runtime.classes must be a valid path in delite.properties", 3)
  }


  def compileAndTest(app: DeliteTestRunner) {
    compileAndTest2(app, app.getClass.getName.replaceAll("\\$", ""))
  }

  def compileAndTest2(app: DeliteTestRunner, uniqueTestName: String) {
    println("=================================================================================================")
    println("TEST: " + app.toString)
    println("=================================================================================================")

    validateParameters()
    val args = Array(uniqueTestName + "-test.deg")
    app.resultBuffer = new ArrayBuffer[Boolean] with SynchronizedBuffer[Boolean]
    stageTest(app, args(0), uniqueTestName)
    val outStr = execTest(app, args, uniqueTestName) // if (runtimeExternalProc..)?
    checkTest(app, outStr)
  }

  def stageTest(app: DeliteTestRunner, degName: String, uniqueTestName: String) = {
    println("STAGING...")
    val save = Config.degFilename
    val buildDir = Config.buildDir
    val saveCacheSyms = Config.cacheSyms
    val generatedDir = (new File("generated")).getAbsolutePath + /*protobuf wants absolute path*/
      java.io.File.separator + uniqueTestName
    try {
      Config.degFilename = degName 
      Config.buildDir = generatedDir
      Config.cacheSyms = cacheSyms
      //Config.generateCUDA = true
      val screenOrVoid = if (verbose) System.out else new PrintStream(new ByteArrayOutputStream())
      Console.withOut(screenOrVoid) {
        app.main(Array())
        if (verboseDefs) app.globalDefs.foreach { d => //TR print all defs
          println(d)
          //val s = d match { case app.TP(sym,_) => sym; case app.TTP(syms,_,_) => syms(0); case _ => sys.error("unknown Stm type: " + d) }
          //val info = s.sourceInfo.drop(3).takeWhile(_.getMethodName != "main")
          //println(info.map(s => s.getFileName + ":" + s.getLineNumber).distinct.mkString(","))
        }
        //assert(!app.hadErrors) //TR should enable this check at some time ...
      }
    } finally { 
      // concurrent access check 
      assert(Config.buildDir == generatedDir)
      Config.degFilename = save
      Config.buildDir = buildDir
      Config.cacheSyms = saveCacheSyms
    }
  }

  def execTest(app: DeliteTestRunner, args: Array[String], uniqueTestName: String) = {
    println("EXECUTING...")
    ppl.delite.runtime.profiler.PerformanceTimer.times.clear // don't print running time (messes up check file)
    val name = "test.tmp"
    System.setProperty("delite.runs", 1.toString)
    System.setProperty("delite.threads", threads.toString)
    System.setProperty("delite.home", Config.homeDir)
    //System.setProperty("delite.cuda", 1.toString)
    System.setProperty("delite.code.cache.home", System.getProperty("user.dir") + java.io.File.separator + "generatedCache" + java.io.File.separator + uniqueTestName)
    //Console.withOut(new PrintStream(new FileOutputStream(name))) {
      println("test output for: " + app.toString)
      // NOTE: DeliteCodegen (which computes app.staticDataMap) does not know about VConstantPool!!!
      val staticDataMap = app match {
        //case app: Base_LMS => app.VConstantPool.map(kv=>kv._1.toString->kv._2).toMap
        case app => app.staticDataMap
      }
      ppl.delite.runtime.Delite.embeddedMain(args, staticDataMap) // was: app.staticDataMap
    //}
    /*val buf = new Array[Byte](new File(name).length().toInt)
    val fis = new FileInputStream(name)
    fis.read(buf)
    fis.close()
    val r = new String(buf)
    if (verbose) System.out.println(r)
    r*/""
  }


  def checkTest(app: DeliteTestRunner, outStr: String) {
    println("CHECKING...")
    /*val resultStr = outStr substring (outStr.indexOf(MAGICDELIMETER) + MAGICDELIMETER.length, outStr.lastIndexOf(MAGICDELIMETER))
    val results = resultStr split ","
    for (i <- 0 until results.length) {
      if (verbose) print("  condition " + i + ": ")
      val passed = results(i).toLowerCase() == "true"
      if (verbose)
        if (passed) println("PASSED") else println("FAILED")
      assert(passed)
    }*/
  }
}


class TestFailedException(s: String, i: Int) extends Exception(s)

trait DeliteTestRunner extends DeliteTestModule with DeliteApplication
  with MiscOpsExp with SynchronizedArrayBufferOpsExp with StringOpsExp {

  var resultBuffer: ArrayBuffer[Boolean] = _

  def collector: Rep[ArrayBuffer[Boolean]] = staticData(resultBuffer)
}

trait DeliteTestModule extends Object
  with MiscOps with SynchronizedArrayBufferOps with StringOps {

  def main(): Unit

  def collector: Rep[ArrayBuffer[Boolean]]

  def collect(s: Rep[Boolean]) { collector += s; println(s) }

  def mkReport(): Rep[Unit] = {
    println(unit(DeliteRunner.MAGICDELIMETER) + (collector mkString unit(",")) + unit(DeliteRunner.MAGICDELIMETER))
  }
}
