#!/usr/bin/env bash

set -e

./compile-app.sh

mkdir -p dist/test

echo "Compiling tests"
javac -cp "test/libs/*:dist/app" \
  $(find ./test/src -name "*.java" ) \
  -d dist/test

echo "Executing tests"
java -jar test/runtime-libs/junit-platform-console-standalone-1.7.0-all.jar \
  --fail-if-no-tests \
  -cp "dist/app:dist/annotations:dist/test" \
  --scan-classpath
