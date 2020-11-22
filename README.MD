# Vanilla Java SE Framework

This is a repository demonstrating how you can use Vanilla Java SE to get advanced features
typically associated with heavy frameworks.

In this branch, `java-build`, I've added a Java build script to replace bash scripts for building the application.

To build everything, run the following command:

```bash
$ java -cp "build/libs/*" build/src/Build.java
```

To re-compile only the app for hot reloading it:

```bash
javac -cp "dist/annotations/:dist/processors/:dist/app/:framework/libs/*" \
    $(find ./app/src -name "*.java") \
    -d dist/app
```

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
