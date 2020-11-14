#!/usr/bin/env bash

set -e

echo "Cleaning up application"
rm -rf dist/app/

echo "Compiling application"
mkdir -p dist/app
javac -cp "dist/annotations/:dist/processors/:dist/app/:framework/libs/*" $(find ./app/src -name "*.java") -d dist/app
