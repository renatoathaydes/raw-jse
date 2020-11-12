#!/usr/bin/env bash

set -e

echo "Cleaning up"
rm -rf dist/

echo "Compiling framework"
mkdir -p dist/framework
javac framework/src/*.java -d dist/framework

echo "Compiling application"
mkdir -p dist/app
javac app/src/*.java -d dist/app

echo "Done"
