# K-Means clustering

`threads` package contains implementations of the
problem with plain Java threads.
`objects` is fully local. Threads share memory.
`objectsCr` is the same, but shared objects are
decoupled in CRUCIAL.

`aws` package contains the same implementation
with `objectsCr` but the threads now are serverless functions.
It includes the code to make and call Lambdas as
threads

```java
new CloudThread(runnable);
```

## Run

`Main` class is the main of each implementation.
Crucial client is configured there (for svr IP).
Problem dimensions are also there.

Lambdas are deployed with

```bash
mvn package shade:shade lambda:deploy-lambda -DskipTests -f pom.xml
```

Configuration is directly in the `pom.xml`. It is
configured to run in the VPC.
VPC can be disabled by commenting out the parameters in the plugin.

Since this example contains user-defined shared objects, the packaged `.jar`
must be uploaded to the CRUCIAL server so that it can be imported.
