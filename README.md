Relite
======

R + Delite

Example
-------

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



Setup
-----

1. Get FastR (install and build in a subdir called `fastr`; latest commit tested: 41183615b2dead89f47afd69c31ff2e6e84fc21e):

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