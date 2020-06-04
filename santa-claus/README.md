# Santa Claus problem

The Santa Claus problem is a traditional exercise for concurrency.
See the details [here](https://crsr.net/files/ANewExerciseInConcurrency.pdf).
In this repository we solve the problem using
[CRUCIAL](http://github.com/danielBCN/crucial-dso) and present different
stages of the implementation.

`threads` package contains implementations of the problem with plain
Java threads.
`crucial.examples.mandelbrot.objects` is fully local. Threads share memory.
`objectsCr` is the same, but shared crucial.examples.mandelbrot.objects are decoupled in CRUCIAL.

`aws` package contains the same implementation with `objectsCr` but the
threads now run in AWS Lambda by using the `CloudThread` abstraction in
[aws-executor](../aws-executor).

### Prerequisites

To build this example you will need the Crucial DSO client installed to the
local Maven repository. 
Follow the instructions to [install and run Crucial DSO](https://github.com/danielBCN/crucial-dso#usage).

You will also need to set up some extra configurations in AWS:
* This example is configured to run in a Virtual Private Cloud (VPC).
 You need to create a VPC with several subnets (e.g. 10.0.128.0/24, 10.0.129.0/24, ...).
 For more information on configuring a Lambda function to access resources in a VPC,
 check out [AWS Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/configuration-vpc.html).
* Assign an S3 endpoint to the route table of the VPC,
 to allow lambda functions to access S3.
* Create an IAM role with at least the following policies:
 AWSLambdaFullAccess, AmazonS3FullAccess, CloudWatchLogsFullAccess,
 and AWSLambdaVPCAccessExecutionRole.

To run this example you need one virtual machine for Crucial DSO server
(e.g. `r5.2xlarge`) and another one for the client node.
Both machines must be in the previously configured VPC and the same subnet, and must have Java 8 Runtime installed.

You will also need to configure the [AWS credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) in the client node in order to be able to invoke lambdas.

### Build  

Before building the example, you have to apply some configurations:
* Edit `aws-executor/src/main/java/crucial/execution/aws/CloudThread.java` 
  and configure the `region` and the `functionName`.
  This function name must include the suffix (see following points).
* Edit `crucial.examples.santa.aws.objectsCr.SantaClaus` and configure
  the IP and port of the Crucial DSO server (same for `threads.objectsCr`).
* Edit `santa-claus/pom.xml` and configure the following fields:
  * `lambda.awsAccountId`
  * `lambda.roleName`: the previously created IAM role 
     (this is the role for the AWS Lambda functions)
  * `lambda.functionName`: without suffix (e.g. `CloudThread`)
  * `lambda.timeout`
  * `lambda.memorySize`
  * `lambda.functionNameSuffix`: e.g. `-daniel`.
     The final name of the function then will be `CloudThread-daniel`.
  * `lambda.s3Bucket`: an S3 bucket used by the lambda-maven-plugin to
     temporary upload the function code before deploying to Lambda.
  * `lambda.region`:  The AWS region to use for the Lambda function.
  * `lambda.vpcsecurityGroup`: The Group ID of the VPC security group.
  * Under the configuration of the lambda-maven-plugin, you have to list
    the VPC subnets that will be assigned to lambda functions. E.g.:
    
```
<vpcSubnetIds>
  <vpcSubnetId>subnet-00000000000000000</vpcSubnetId>
  <vpcSubnetId>subnet-11111111111111111</vpcSubnetId>
  <vpcSubnetId>subnet-22222222222222222</vpcSubnetId>
  <vpcSubnetId>subnet-33333333333333333</vpcSubnetId>
</vpcSubnetIds>
```

Package and deploy the functions code to AWS Lambda with:

```bash
mvn package shade:shade lambda:deploy-lambda -DskipTests -f pom.xml
```


### Run

You have to copy `santa-claus-1.0.jar` to the client node. 
Since this example contains user-defined shared crucial.examples.mandelbrot.objects, you also have to copy
this jar file to the `/tmp` directory of the Crucial DSO server node/s, so that
it can be imported.

Start the Crucial DSO server with VPC support:

```bash
./server.sh -vpc
```

Make sure that the Crucial DSO server is loading the jar file with the shared
crucial.examples.mandelbrot.objects classes. The logs should show a line like this:

```
[Server] Loading santa-claus-1.0.jar
```

Start the client:

```bash
java -cp santa-claus-1.0.jar crucial.examples.santa.aws.objectsCr.SantaClaus
```

`SantaClaus` class is the main of each implementation.
Crucial client is configured there (for server IP).
Problem dimensions are also there.

#### Further notes
Commenting out VPC configuration will run the Lambda functions outside of it.
Then, the DSO server should be accessible from outside.

For local testing (`threads` package), run the DSO server locally and connect
through `localhost`.

The AWS version can be tested locally with the local feature in
[aws-executor](../aws-executor) (see line 56 at `aws.objectsCr.SantaClaus`).
