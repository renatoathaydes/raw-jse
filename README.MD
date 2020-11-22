# Vanilla Java SE Framework

This is a repository demonstrating how you can use Vanilla Java SE to get advanced features
typically associated with heavy frameworks.

In this branch, `annotation-processor`, I demonstrate how to use annotation processors to generate Java code.

Annotations are defined in the [annotations](annotations) module.

The annotation processor, [HttpAnnotationProcessor](processors/src/processors/HttpAnnotationProcessor.java), in the
[processors](processors) module.

The application code, which uses the annotations, is in the [app](app) module.

To build everything, run the [`compile.sh`](compile.sh) script.

To compile the app to a native binary:

```bash
$ native-image -cp "framework/libs/*:dist/app" http.HttpMain
```

To run the application with hot-reloading:

```bash
java -cp "framework/libs/*:dist/framework" Main \
  "framework/libs/rawhttp-core-2.4.1.jar:dist/app:dist/annotations" \
  http.AppRequestHandlers
```

To run the application with a simple main (no hot-reloading):

```bash
$ java -cp "framework/libs/*:dist/app" http.HttpMain
```

To run the native binary:

```bash
$ ./http.httpmain
```
