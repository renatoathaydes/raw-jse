# Vanilla Java SE Framework

This is a repository demonstrating how you can use Vanilla Java SE to get advanced features
typically associated with heavy frameworks.

In this branch, `step-1-hot-reloading`, I am demonstrating how the JVM can easily hot reload classes at runtime.

To try it out:

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

```
