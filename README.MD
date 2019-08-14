# Pool Usage

This is repository aims to show some good and bad pattern regarding thread pool usage. 
In particular, I would like to show the difference between:
*   single global pool (fork-join) ala Scala Future
*   2-3 pools: compute, blocking, dispatch

