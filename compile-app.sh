#!/usr/bin/env bash

set -e

echo "Cleaning up application"
rm -rf dist/app/

echo "Compiling application"
mkdir -p dist/app/http/api/
cp -r dist/framework/http/api/ dist/app/http/api/
cp dist/framework/http/HttpServer.class dist/app/http/

javac -cp "dist/annotations/:dist/processors/:dist/app/:framework/libs/*" \
    $(find ./app/src -name "*.java") \
    -d dist/app
