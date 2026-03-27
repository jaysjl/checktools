#!/bin/bash

PORTS='3306,5432,1433,1521,27017,6379,9200,5236,54321,2881,2883'

TCPDUMP_EXISTS=`which tcpdump | wc -l`
if [ "$TCPDUMP_EXISTS" == "0" ]; then
    echo "需要预先安装tcpdump！"
    exit 1
fi

if [ "`whoami`" != "root" ]; then
    echo "需要使用root权限！"
    exit 1
fi

function usage {
echo "用法： datanode_discovery_by_tcpdump.sh [选项]
    选项为：
        -p [端口范围]           例如: -p 3306,5432,1521,1433
                                默认: -p $PORTS
        -h                      显示帮助信息
    注意: 需要预先安装tcpdump、需要使用root权限
" 
}

while getopts ":hp:" opt 
do 
    case $opt in 
        p)
            PORTS="$OPTARG"
            ;;
        h|?)
            usage
            exit 1
            ;; 
    esac 
done 

FILTER="${PORTS//,/ or dst port }"
FILTER="dst port $FILTER"
#echo $FILTER

trap 'exit' INT

for iface in /sys/class/net/*; do
    iface_name=$(basename "$iface")
    if [ "$iface_name" != "lo" ]; then
        ip link set dev "$iface_name" promisc on
    fi
done

while true
do
    timeout 2 tcpdump -i any -l -nnt $FILTER &> /tmp/discovered_datanodes_by_tcpdump.tmp
    cat /tmp/discovered_datanodes_by_tcpdump.tmp | grep ">" | awk '{print substr($0, index($0, "IP"))}' | awk -F ':' '{print $1}' | awk '{print $2 " "$3" " $4}' | sort -u | uniq | grep -v '^$'
done
