#!/bin/bash

cd $(dirname "$0")

for iface in /sys/class/net/*; do
    iface_name=$(basename "$iface")
    if [ "$iface_name" != "lo" ]; then
        ip link set dev "$iface_name" promisc on
    fi
done

tcpdump -i any -A -nntql -s 0 tcp 2>&1 \
    | grep 'IP ' -A 1 \
    | awk '/IP/ {start=index($0,"IP")+3; end=index($0,":"); print substr($0,start,end-start)} !/IP/ {print $0}' \
    | awk '/^E/ {print substr($0, 41)} !/^E/ {print "\033[1;31m=== " $0 " ===\033[0m" }' \
    | grep -E "(SELECT|select|INSERT|insert|UPDATE|update|DELETE|delete)" -B 1
