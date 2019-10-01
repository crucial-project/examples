# AWS Executor

This project includes the abstraction of `CloudThread` that allows
running Java runnables in the cloud thanks to AWS Lambda.

The `CloudThread` class requires some configuration depending on your
AWS Lambda deployment. See the examples for more information.

The basic usage is that of Java threads:

```java
Thread t = new CloudThread(new MyRunnable());
t.start();
t.join();
```
