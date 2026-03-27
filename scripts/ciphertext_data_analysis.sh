#!/bin/bash

DIR=$(dirname "$0")

java -jar $DIR/../java/saltscan-1.0.0-all.jar --config $1

