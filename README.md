Relite
======

R + Delite

Example
-------

    Delite({
        v <- Vector.rand(5)
        pprint(v)
    })

    STAGING...
    Delite Application Being Staged:[Test3$$anon$7$$anon$4$$anon$1]
    ******Generating the program******
    EXECUTING...
    test output for: Test3$$anon$7$$anon$4$$anon$1@2799b086
    Delite Runtime executing with the following arguments:
    Test3anon7anon4anon1-test.deg
    Delite Runtime executing with: 1 Scala thread(s), 0 Cpp thread(s), 0 Cuda(s), 0 OpenCL(s)
    Beginning Execution Run 1
    [ 0.7220096548596434 0.19497605734770518 0.6671595726539502 0.7784408674101491 0.6186076060240648 ]
    [METRICS]: Latest time for component all: 0.008117s



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