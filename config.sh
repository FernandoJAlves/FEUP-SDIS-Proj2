#!/bin/bash

# To be included with 'source config.sh' in other scripts.

# The m parameter for the Chord network. Must match that in Chord.java
m=32

# The precision with which to print chord id locations. Should match that in Chord.java.
precision=2

# Which address to use for the nodes. All will use the same network.
addr="localhost"

# How long should the launch.sh wait before killing everyone.
wait=600 # seconds

# Log directory
logdir="log"

# Do not change these, they are hardcoded in launch.sh for readability.
minport=29500
maxport=29530
