#!/bin/bash

source config.sh

clear
[ -d "$logdir" ] && rm -rf "$logdir"
mkdir -p "$logdir"

declare -A map

# $1 = my server port
function print {
    java -cp bin dbs.Dbs print "$addr" "$1"
}

for port in $(seq 29500 29530); do
    id=$(print "$port")
    map["$port"]="$id"
    percentage=$(wcalc -q "(100.0 * $id) / (2 ** $m)")
    width=$(( precision + 3 ))
    printf "%d: (%0*.*f%%) %d\n" "$port" "$width" "$precision" "$percentage" "$id"
done

# $1 = server port
function create {
    echo "Creating first chord peer $1"
    java -cp bin dbs.Dbs create "$addr" "$1" > "$logdir/$1" 2>&1 &
}

# $1 = my server port
# $2 = someone else's server port
function join {
    echo "Joining new chord peer $1 to $2"
    remote="${map["$2"]}"
    java -cp bin dbs.Dbs join "$addr" "$1" "$remote" "$addr" "$2" > "$logdir/$1" 2>&1 &
}

trap 'jobs -p | xargs kill' EXIT

create 29500
sleep 2.2s
join 29501 29500
sleep 0.2s
join 29502 29500 # same
join 29503 29500 # same
sleep 0.3s
join 29504 29500
sleep 0.5s
join 29505 29501
sleep 2s
join 29506 29503
join 29507 29500 # same
join 29508 29504
join 29509 29502
join 29510 29500 # same
join 29511 29501
sleep 0.4s
join 29512 29506 # same
join 29513 29502
join 29514 29505
join 29515 29506 # same
join 29516 29500
sleep 0.3s
join 29517 29501
join 29518 29514
sleep 0.3s
join 29519 29514
sleep 3s
join 29520 29510 # same
join 29521 29508
join 29522 29503
join 29523 29507
join 29524 29517
join 29525 29502
join 29526 29509
join 29527 29512
join 29528 29501
join 29529 29510 # same
sleep 0.5s
join 29530 29511

source countdown.sh

countdown $wait
