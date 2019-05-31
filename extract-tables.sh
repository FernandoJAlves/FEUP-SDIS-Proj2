#!/bin/bash

source config.sh

rm -f tables

after_grep=$(( $m + 2 ))
catch_tail=$(( $m + 4 ))

echo "tables for:"

for port in $(seq $minport $maxport); do
    if [ -f "$logdir/$port" ]; then
        echo "      $port"
        grep -Ee "^Table of node" "$logdir/$port" -A $after_grep -B 1 \
            | tail -n $catch_tail >> tables
    fi
done
