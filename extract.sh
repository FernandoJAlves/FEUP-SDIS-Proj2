#!/bin/bash

what="$1"
shift
where="$1"
shift

source config.sh

if [ -z "$what" ] || [ -z "$where" ]; then
    echo "usage: ./extract.sh WHAT WHERE"
    echo "Extract messages of type 'WHAT' into directory 'WHERE' from all logfiles."
    exit 0
fi

for port in $(seq $minport $maxport); do
    if [ -f "$logdir/$port" ]; then
        echo "      $port"
        echo "grep -Ee "\[$what\]" "$logdir/$port" > "$where/$port""
        grep -Ee "$what" "$logdir/$port" > "$where/$port"
    fi
done
