# CRUCIAL Examples

This repository contains different examples using the
[CRUCIAL](http://github.com/danielBCN/crucial-dso) system.

All these example projects depend on the [aws-executor](./aws-executor).
It contains the abstraction of `CloudThread` and allows to run threads at
AWS Lambda transparently.
It also includes a `ServerlessExecutorService` to easily manage the execution
of concurrent tasks.
This executor service offers the same API as the Java one, but it adds the
`invokeIterativeTask` functionality to easily parallelize **for** structures
(see Mandelbrot example).


Examples:

- [_k_-means](./kmeans)
- [logistic regression](./logistic-regression)
- [Santa Claus problem](./santa-claus)
- [Mandelbrot](./mandelbrot)
