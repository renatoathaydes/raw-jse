#!/usr/bin/env bash

set -e

echo "Cleaning up"
rm -rf dist/

echo "Compiling annotations"
javac annotations/src/raw/jse/http/*.java -d dist/annotations

echo "Compiling annotation processors"
javac -proc:none -cp dist/annotations/ $(find ./processors/src -name "*.java") -d dist/processors
cp -r processors/resources/ dist/processors/

echo "Compiling framework"
mkdir -p dist/framework
javac -cp "framework/libs/*" $(find ./framework/src -name "*.java") -d dist/framework

./compile-app.sh

echo "Done"
