#!/bin/bash
rm -rf bin
mkdir -p bin
find src | grep "\.java$" | xargs javac -d bin -target 1.4 -source 1.4
