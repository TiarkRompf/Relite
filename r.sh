#! /bin/bash
java -classpath \
fastr/bin:\
fastr/lib/antlr-runtime-3.5.jar:\
fastr/lib/arpack_combined_all.jar:\
fastr/lib/junit-4.8.jar:\
fastr/lib/netlib-java-0.9.3.jar:\
fastr/lib/truffle-api-03-16-2013.jar:\
fastr/lib/jline-2.12.jar:\
target/scala-2.10/classes:\
lib_managed/jars/EPFL/lms_2.10/lms_2.10-0.3-SNAPSHOT.jar:\
lib_managed/jars/com.google.protobuf/protobuf-java/protobuf-java-2.4.1.jar:\
lib_managed/jars/org.apache.commons/commons-math/commons-math-2.2.jar:\
lib_managed/jars/org.apache.mesos/mesos/mesos-0.9.0-incubating.jar:\
lib_managed/jars/org.scala-lang/scala-actors/scala-actors-2.10.2-RC1.jar:\
lib_managed/jars/org.scala-lang.virtualized/scala-compiler/scala-compiler-2.10.2-RC1.jar:\
lib_managed/jars/org.scala-lang.virtualized/scala-library/scala-library-2.10.2-RC1.jar:\
lib_managed/jars/org.scala-lang.virtualized/scala-reflect/scala-reflect-2.10.2-RC1.jar:\
lib_managed/jars/org.scalatest/scalatest_2.10/scalatest_2.10-2.0.M5b.jar:\
lib_managed/jars/stanford-ppl/framework_2.10/framework_2.10-0.1-SNAPSHOT.jar:\
lib_managed/jars/stanford-ppl/optila_2.10/optila_2.10-0.1-SNAPSHOT.jar:\
lib_managed/jars/stanford-ppl/optiml_2.10/optiml_2.10-0.1-SNAPSHOT.jar:\
lib_managed/jars/stanford-ppl/runtime_2.10/runtime_2.10-0.1-SNAPSHOT.jar:\
-ea -esa relite.Main $*
