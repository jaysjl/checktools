#!/bin/bash

PORTS='80,8080,3306,5432,1433,1521,27017,6379,9200,5236,54321,2881,2883'
TIMING_TEMPLATE='4'

NMAP_EXISTS=`which nmap | wc -l`
if [ "$NMAP_EXISTS" == "0" ]; then
    echo "需要预先安装nmap！"
    exit 1
fi

function usage {
echo "用法： datanode_discovery_by_nmap.sh [选项] [探测目标]
    打印 [探测目标] 中开放的服务
        例如: scanme.nmap.org, microsoft.com/24, 192.168.0.1; 10.0.0-255.1-254
        若不填写默认同网段IP
    选项为：
        -p [端口范围]           例如: -p 22; -p 1-65535; -p 21-25,80,139,8080
                                默认: -p $PORTS
        -T [0-4]                设置计时模板（越高越快）默认$TIMING_TEMPLATE
        -h                      显示帮助信息
    注意: 需要预先安装nmap
" 
}

while getopts ":hp:T:" opt 
do 
    case $opt in 
        p)
            PORTS="$OPTARG"
            ;;
        T)
            TIMING_TEMPLATE="$OPTARG"
            ;;
        h|?)
            usage
            exit 1
            ;; 
    esac 
done 

shift $[$OPTIND-1] 
IPS=$@
if [ "$IPS" == "" ]; then
    IPS=`ip addr show | grep -v 'inet6' | grep -v '127.0' | grep 'inet' | grep brd | awk '{print $2}'| tr '\n' ' '`
    IPS="127.0.0.1 $IPS"
fi

nmap -T $TIMING_TEMPLATE -v -sS -p $PORTS $IPS 2>/dev/null | grep -E 'open' 
