#!/usr/bin/env bash

set -e

echo "Cleaning up"
rm -rf dist/

echo "Compiling annotations"
javac annotations/src/raw/jse/http/*.java -d dist/annotations

echo "Compiling framework"
mkdir -p dist/framework
mkdir -p dist/app/http/api/
javac -cp "framework/libs/*" $(find ./framework/src -name "*.java") -d dist/framework

echo "Compiling annotation processors"
javac -proc:none -cp dist/annotations/:dist/framework $(find ./processors/src -name "*.java") -d dist/processors
cp -r processors/resources/ dist/processors/

./compile-app.sh

echo "Done"
