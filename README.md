Relite
======

R + Delite

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
    - Create a `delite.properties` file in your local lancet clone 
      (contents as described [here](http://github.com/stanford-ppl/delite)).

3. Use `sbt test` and `sbt test:run` to run tests.