#!/bin/bash

addr="localhost"
wait=20s
numwaits=5

# NOTA: é preciso implementar Notify

declare -A map

# $1 = my server port
function print {
    java -cp bin dbs.App print "$addr" "$1"
}

for port in $(seq 29500 29530); do
    map["$port"]=$(print "$port")
    echo "$port: "${map["$port"]}""
done

# $1 = server port
function create {
    echo "Creating first chord peer $1"
    java -cp bin dbs.App create "$addr" "$1" > "log/$1" 2>&1 &
}

# $1 = my server port
# $2 = some else's server port
function join {
    echo "Joining new chord peer $1 to $2"
    java -cp bin dbs.App join "$addr" "$1" "${map["$2"]}" "$addr" "$2" > "log/$1" 2>&1 &
}

trap 'jobs -p | xargs kill' EXIT

create 29500
sleep 3.2s
join 29501 29500
sleep 3.2s
join 29502 29500
sleep 3.5s
join 29503 29500
sleep 3.3s
join 29504 29500
sleep 1.5s
join 29505 29501
: '
sleep 2s
join 29506 29503
join 29507 29500
join 29508 29504
join 29509 29502
join 29510 29500
join 29511 29501
sleep 2s
join 29512 29506
join 29513 29502
join 29514 29505
join 29515 29506
join 29516 29500
sleep 0.3s
join 29517 29501
join 29518 29514
sleep 0.3s
join 29519 29514
'
echo -n "waiting($wait, x$numwaits)"

for port in $(seq 1 $numwaits); do
    sleep $wait
    echo -n .
done
