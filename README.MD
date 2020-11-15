# Vanilla Java SE Framework

This is a repository demonstrating how you can use Vanilla Java SE to get advanced features
typically associated with heavy frameworks.

In this branch, `http-server-function-handler`, I am demonstrating how the JVM can easily hot reload a request
handler that is used by a running HTTP server.

I've also created a separate class, [NativeMain](framework/src/NativeMain.java), to launch the application without
reflection or hot-reloading, so that we can compile it to run as a native image.

### Hot reloading

To compile and run the application with hot-reload:

1. run [compile.sh](compile.sh).
2. run the [Main](framework/src/Main.java) class as shown below.
3. change [Starter.java](app/src/Starter.java).
4. re-compile the app in another shell.
5. observe how the [Starter] class was swapped and re-run.

To run the `Main` class and let it reload the `Starter`, run this command:

```bash
java -cp dist/framework Main dist/app Starter
```

To re-compile only the app for hot reloading it:

```bash
javac app/src/Starter.java -d dist/app/
```

### Native image

To compile the app as a native-image, run the [compile-native.sh](compile-native.sh) script.

This requires that you have GraalVM installed, as well as the [native compiler toolchain](https://www.graalvm.org/reference-manual/native-image/).

The script creates a binary called `nativemain` which can be executed directly:

```bash
./nativemain
```

### Measuring startup time

There is a script called [measure-startup-time.sh](measure-startup-time.sh) which can be used to check how fast the HTTP
Server starts responding to HTTP requests.

To change the method used to run the server, edit the line below `# start the server`.

Here are sample results when running on a MacBook Air:

```bash
# Running the app with hot reloading on a standard JVM
$ ./measure-startup-time.sh
classpath=dist/app, runnableClass=Starter
Watching directory dist/app
Server connected in 274 ms

# Running the app as a native binary
$ ./measure-startup-time.sh
Starting server on port 8080
Server connected in 38 ms
```
