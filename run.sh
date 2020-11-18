#!/usr/bin/env bash

java -cp "framework/libs/*:dist/framework" Main \
  "framework/libs/rawhttp-core-2.4.1.jar:dist/app:dist/annotations" \
  http.AppRequestHandlers
