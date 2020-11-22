# Vanilla Java SE Framework

This is a repository demonstrating how you can use Vanilla Java SE to get advanced features
typically associated with heavy frameworks.

In this branch, `junit-tests`, I demonstrate how to compile and run Java JUnit tests without a build tool.

To build everything, run the [`compile.sh`](compile.sh) script.

To compile the tests only, run the following command:

```bash
$ javac -cp "test/libs/*:dist/app" \
      $(find ./test/src -name "*.java" ) \
      -d dist/test
```

To execute the tests, run:

```bash
java -jar test/runtime-libs/junit-platform-console-standalone-1.7.0-all.jar \
    --fail-if-no-tests \
    -cp "dist/app:dist/annotations:dist/test" \
    --scan-classpath
```
