# Logistic Regression

This example uses Crucial DSO and AWS Lambda to compute the logistic regression
on some data stored in S3.
The code assumes the dataset contains comma-separated-values (CSV) partitioned
in several files (part-00000, part-00001, ...) and the last value of each row is
the label while the other values are the features.

Each lambda function will compute one partition of the dataset.

### Prerequisites

To build this example you will need the Crucial DSO client installed to the
local Maven repository. 
Follow the instructions to [install and run Crucial DSO](https://github.com/crucial-project/dso).

You will need also to set up some extra configurations in AWS:
* This example is configured to run in a Virtual Private Cloud (VPC).
  You need to create a VPC with several subnets
  (e.g. 10.0.128.0/24, 10.0.129.0/24, ...).
   For more information on configuring a Lambda function to access resources in
   a VPC, check out [AWS Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/configuration-vpc.html).
* Assign an S3 endpoint to the route table of the VPC, to allow lambda functions
  to access S3.
* Create an IAM role with at least the following policies:
  AWSLambdaFullAccess, AmazonS3FullAccess, CloudWatchLogsFullAccess,
  and AWSLambdaVPCAccessExecutionRole.

To run this example you need one virtual machine for Crucial DSO server
(e.g. `r5.2xlarge`) and another one for the client node.
Both machines must be in the previously configured VPC and the same subnet,
and must have Java 8 Runtime installed.

You will also need to configure the
[AWS credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
in the client node in order to be able to invoke lambdas.

### Build  

Before building the example, you have to apply some configurations:
* Configure [Crucial executor](https://github.com/crucial-project/executor) at
  logistic-regression/src/main/resources/config.properties.
* Edit `crucial.examples.logisticregression.aws.objectsCr.Main` and configure
  the IP and port of the Crucial DSO server.
* Edit `crucial.examples.logisticregression.aws.objectsCr.S3Reader` and specify
  the `S3_BUCKET` that contains the data.  
* Edit `logistic-regression/pom.xml` and configure the following fields:
  * `lambda.awsAccountId`
  * `lambda.roleName`: the previously created IAM role
  * `lambda.functionName`: without a suffix (e.g. `CloudThread`)
  * `lambda.timeout`
  * `lambda.memorySize`
  * `lambda.functionNameSuffix`: e.g. `-example`.
    The final name of the function then will be `CloudThread-example`.
  * `lambda.s3Bucket`: an S3 bucket used by the lambda-maven-plugin to temporary
    upload the function code before deploying to Lambda.
  * `lambda.region`:  The AWS region to use for the Lambda function.
  * `lambda.vpcsecurityGroup`: The Group ID of the VPC security group.
  * Under the configuration of the lambda-maven-plugin, you have to list the VPC
    subnets that will be assigned to lambda functions. E.g.:
    
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

You have to copy `logistic-regression-1.0.jar` to the client node. 
Since this example contains user-defined shared objects, you also have to copy
this jar file to the `/tmp` directory of the Crucial DSO server node/s,
so that it can be imported.

Start the Crucial DSO server with VPC support:

```bash
./server.sh -vpc
```

Make sure the Crucial DSO server is loading the jar file with the shared
objects classes. The logs should show a line like this:

```
[Server] Loading logistic-regression-1.0.jar
```

Start the client:

```bash
java -cp logistic-regression-1.0.jar crucial.examples.logisticregression.aws.objectsCr.Main 50 80 100 dataset-100GB-100d
```

This command runs the logistic regression during 50 iterations using 80 lambdas.
The dataset contains 100 features per element, and the prefix of the S3 files
is `dataset-100GB-100d`.
