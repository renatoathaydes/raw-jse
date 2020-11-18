#!/usr/bin/env bash

native-image -cp "framework/libs/*:dist/app" http.HttpMain
