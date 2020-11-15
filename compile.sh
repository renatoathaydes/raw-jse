#!/usr/bin/env bash

set -e

echo "Cleaning up"
rm -rf dist/

echo "Compiling framework"
mkdir -p dist/framework
javac -cp "framework/libs/*" framework/src/{WatchDir.java,Main.java,HttpServer.java}  -d dist/framework

echo "Compiling application"
mkdir -p dist/app
javac $(find ./app -name "*.java") -d dist/app

echo "Done"
