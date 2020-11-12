#!/usr/bin/env bash

set -e

echo "Cleaning up"
rm -rf dist/

echo "Compiling framework"
mkdir -p dist/framework
javac $(find ./framework -name "*.java") -d dist/framework

echo "Compiling application"
mkdir -p dist/app
javac $(find ./app -name "*.java") -d dist/app

echo "Done"
