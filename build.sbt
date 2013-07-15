name := "relite"

version := "0.1"

scalaVersion := "2.10.2-RC1"

//scalaBinaryVersion := "2.10.0"

scalaOrganization := "org.scala-lang.virtualized"

scalacOptions += "-Yvirtualize"

// tests are not thread safe
parallelExecution in Test := false

libraryDependencies += "org.scala-lang.virtualized" % "scala-compiler" % "2.10.0"

libraryDependencies += "EPFL" %% "lms" % "0.3-SNAPSHOT"

libraryDependencies += "stanford-ppl" %% "framework" % "0.1-SNAPSHOT"

libraryDependencies += "stanford-ppl" %% "runtime" % "0.1-SNAPSHOT"

libraryDependencies += "stanford-ppl" %% "optiml" % "0.1-SNAPSHOT"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "2.4.1"


scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "test-src")


retrieveManaged := true

unmanagedBase <<= baseDirectory { base => base / "fastr" / "lib" }

unmanagedClasspath in Compile <++= baseDirectory map { base =>
   Seq(base / "fastr" / "bin")
}

unmanagedClasspath in Test <++= (unmanagedClasspath in Compile)

// unmanagedClasspath in Run <++= (unmanagedClasspath in Compile)
