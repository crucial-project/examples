# CRUCIAL Examples

This repository contains different examples using the
[CRUCIAL](https://github.com/crucial-project/crucial) system.

All these example projects depend on the
[crucial-executor](https://github.com/crucial-project/executor).
It contains the abstraction of `CloudThread` and allows running threads at
AWS Lambda transparently.
It also includes a `ServerlessExecutorService` to easily manage the execution
of concurrent tasks.
This executor service offers the same API as the Java one, but it adds the
`invokeIterativeTask` functionality to easily parallelize **for** structures
(see Mandelbrot example).

The examples also need the [DSO datastore](https://github.com/crucial-project/dso)
running.

Examples:

- [_k_-means](./kmeans)
- [logistic regression](./logistic-regression)
- [Santa Claus problem](./santa-claus)
- [Mandelbrot](./mandelbrot)
