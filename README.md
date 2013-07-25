Relite
======

R + Delite = Relite. This project is a proof-of-concept accelerator for [R](http://www.r-project.org) 
using [Delite](http://github.com/stanford-ppl/delite), based on the [FastR](https://github.com/allr/fastr) 
implementation on the JVM.


Some Quick Performance Numbers
------------------------------

Let's run a quick one-liner first in GNU R:

      > system.time(sapply(1:50000,function(x) { sum(1:x) }))
         user  system elapsed 
        3.224   0.900   4.124 

Then the same in FastR:

      > v <- system.time(sapply(1:50000,function(x) { sum(1:x) }))
      elapsed: 1.579s

And with Delite, parallelized using 8 threads:

      > v <- Delite(sapply(1:50000,function(x) { sum(1:x) }))
      [METRICS]: Latest time for component all: 0.413440s

As we can see, we get about a 10x speedup over GNU R (and about 4x over FastR).


Let's change the program slightly and introduce some additional computation in the inner loop. 

GNU R:

    > system.time(sapply(1:50000,function(x) { sum((1:x)*0.1) }))
       user  system elapsed 
      7.703   1.362   9.066 

FastR:

    > v <- system.time(sapply(1:50000,function(x) { sum((1:x)*0.1) }))
    elapsed: 2.228s

Delite (8 threads):

    > v <- Delite(sapply(1:50000,function(x) { sum((1:x)*0.1) }))
    [METRICS]: Latest time for component all: 0.560602s

We're approaching 20x over GNU R, and remain at 4x over FastR.


Both GNU R and FastR have optimized operations on vectors like `(1:x)*0.1`, but performance 
completely breaks if we switch to scalar mode. Not so Delite:


    > v <- Delite(sapply(1:50000,function(x) { sum(sapply(1:x, function(y) y * 0.1)) }))
    [METRICS]: Latest time for component all: 0.536216s

We can see that performance stays exactly the same. In fact, the very same code
is executed internally!

Let try FastR:

    > v <- system.time(sapply(1:50000,function(x) { sum(sapply(1:x, function(y) y * 0.1)) }))
    elapsed: 104.255s
    

And GNU R:

    > system.time(sapply(1:50000,function(x) { sum(sapply(1:x, function(y) y * 0.1)) }))
        user   system  elapsed 
    2383.707   10.775 2395.798 

Yes, that's right, 40 (!) minutes.

Delite speedup: 4500x over GNU R, 200x over FastR. All numbers taken on a Mid 2012 MacBook Pro Retina.




How Does it Work?
-----------------

Delite is a compiler framework that performs aggressive operations like loop fusion,
code motion, struct flattening, etc. You can read more about it [here](http://stanford-ppl.github.io/Delite). 
This project uses Delite to compile and optimize R code at runtime.
Here is a slightly larger example:

      relite me$ ./r.sh 
      Using LAPACK: org.netlib.lapack.JLAPACK
      Using BLAS: org.netlib.blas.JBLAS
      Using GNUR: not available
      > v0 <- c(1,2,3,4)
      > res <- Delite({
      +     pprint(v0)
      +     v1 <- map(v0,function(x) 2*x)
      +     v2 <- Vector.rand(4)
      +     v3 <- v1 + v2
      +     pprint(v1)
      +     pprint(v2)
      +     pprint(v3)
      +     v3
      + })

      STAGING...
      Delite Application Being Staged:[relite.DeliteBridge$$anon$3$$anon$2$$anon$1]
      ******Generating the program******
      EXECUTING...
      test output for: relite.DeliteBridge$$anon$3$$anon$2$$anon$1@7e09e56a
      Delite Runtime executing with the following arguments:
      relite.DeliteBridgeanon3anon2anon1-test.deg
      Delite Runtime executing with: 8 Scala thread(s), 0 Cpp thread(s), 0 Cuda(s), 0 OpenCL(s)
      Beginning Execution Run 1
      [ 1.0 2.0 3.0 4.0 ]
      [ 2.0 4.0 6.0 8.0 ]
      [ 0.7220096548596434 0.19497605734770518 0.6671595726539502 0.7784408674101491 ]
      [ 2.7220096548596433 4.194976057347705 6.66715957265395 8.778440867410149 ]
      [METRICS]: Latest time for component all: 0.009590s

      > res
      2.7220096548596433, 4.194976057347705, 6.66715957265395, 8.778440867410149
      > 



Relite adds a new builtin for expressions of the form `Delite(…)` that takes the
AST of its argument and interprets it by executing the corresponding Delite operations.

Since the Delite API uses [staging (LMS)](http://scala-lms.github.io), this little interpreter 
is actually a translator from R ASTs to Delite IR, from which the Delite backend generates low-level, 
parallelized code (either Scala, C, CUDA, ...), after performing a bunch of optimizations on the 
Delite IR level like loop fusion, code motion etc.

Let's go through the example snippet step by step:

First we create some data vector in the R world, which we later operate on using Delite:

    v0 <- c(1,2,3,4)

Now we enter the Delite block, which will be compiled and executed as one piece of code.
The result is again available as a regular data object in R:

    res <- Delite({

First, we use Delite's `pprint` operator to print v0 to the console. On the R side, v0 is a `DoubleImpl`,
and in Delite it is mapped to a `DenseVector[Double]`. The underlying data array is re-wrapped, not
copied:

      pprint(v0)

Now we perform some actual compution. The `map` call is interpreted as `v0.map { x => ... }` on the
Delite side, passing a Scala closure that interprets the body of the R [closure expression]
(https://github.com/TiarkRompf/Relite/blob/master/src/relite/DeliteBridge.scala#L128).

      v1 <- map(v0,function(x) 2*x)
      v2 <- Vector.rand(4)
      v3 <- v1 + v2

One interesting aspect here is that the `v1+v2` vector operation is internally also a map operation 
in Delite. This means that both will actually be fused together and computed at the same time,
instead of first building v1 and traversing it again to compute v3.

Finally we print the results:

      pprint(v1)
      pprint(v2)
      pprint(v3)

And return v3 to the R world, again without copying, just by rewrapping the underlying data array:

      v3
    })

As a result of executing this `Delite({...})` block, we can see a bunch of stuff happening in 
the console. This is just debug output from Delite telling us what it is doing:

    STAGING...
    Delite Application Being Staged:[relite.DeliteBridge$$anon$3$$anon$2$$anon$1]
    ******Generating the program******
    EXECUTING...
    test output for: relite.DeliteBridge$$anon$3$$anon$2$$anon$1@7e09e56a
    Delite Runtime executing with the following arguments:
    relite.DeliteBridgeanon3anon2anon1-test.deg
    Delite Runtime executing with: 8 Scala thread(s), 0 Cpp thread(s), 0 Cuda(s), 0 OpenCL(s)
    Beginning Execution Run 1

Then comes the actual application output from Delite (printing v0 ... v3):

    [ 1.0 2.0 3.0 4.0 ]
    [ 2.0 4.0 6.0 8.0 ]
    [ 0.7220096548596434 0.19497605734770518 0.6671595726539502 0.7784408674101491 ]
    [ 2.7220096548596433 4.194976057347705 6.66715957265395 8.778440867410149 ]

    [METRICS]: Latest time for component all: 0.009590s

And finally we're back in the R console, where we can print the result of the Delite block (v3) again,
or perform additional operations on it (plotting, ...):

    > res
    2.7220096548596433, 4.194976057347705, 6.66715957265395, 8.778440867410149


Being a proof-of-concept, the implementation is quite simple and just a small prototype at the 
moment. Most of it is contained in the ~200 lines 
[here](https://github.com/TiarkRompf/Relite/blob/master/src/relite/DeliteBridge.scala).





If You Want to Try it Out
-------------------------

1. Get [FastR](https://github.com/allr/fastr) (install and build in a subdir called `fastr`; latest commit tested: 41183615b2dead89f47afd69c31ff2e6e84fc21e):

        (git clone https://github.com/allr/fastr; cd fastr; ant)

2. Install dependencies: LMS-Core and Delite
    - [LMS-Core](http://github.com/tiarkrompf/virtualization-lms-core): 
      branch `delite-develop` (latest commit tested: b80693ddb6408c47116666f5253b777c48966509). 
      Run `sbt publish-local` inside your local clone dir.
    - [Delite](http://github.com/stanford-ppl/delite): 
      branch `wip-clusters-lancet` (latest commit tested: 32f6c245c15abe676f2251b895489b3aee2aad68).
      Run `sbt publish-local` and `sbt delite-test/publish-local` inside your local clone dir. There might
      be compile errors but that is OK.
    - Create a `delite.properties` file in your local checkout 
      (contents as described [here](http://github.com/stanford-ppl/delite)).

3. Use `sbt test` and `sbt test:run` to run tests.

### License

For the time being, Project Lancet is licensed under the [AGPLv3](http://www.gnu.org/licenses/agpl.html). More
permissive licensing may be available in the future.