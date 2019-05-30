#!/bin/bash

countdown() {
    secs=$1
    while [ $secs -gt 0 ]; do
        printf "\r\033[K\033[1;37mWaiting... %ds\033[0m" $((secs--))
        sleep 1s
    done
    printf "\r\033[K\033[1;32mDone!\033[0m\n"
}
