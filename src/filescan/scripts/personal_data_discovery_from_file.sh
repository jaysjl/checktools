#!/bin/bash

PATTERN=''
ONLY_MATCHING=
BEFORE_CONTEXT=
AFTER_CONTEXT=
CONTEXT=
MAX_COUNT=

function usage {
echo "用法： personal_data_discovery_from_file.sh [选项] [文件]
    打印 [文件] 中可打印的字符串
    选项为：
        -P [TYPE|PATTERN]       TYPE查找类型
                                    all    : 全部可见字符（默认）
                                    id     : 身份证
                                    phone  : 手机号
                                    chinese: 中文
                                PATTERN是一个Perl正则表达式
        -m [NUM]                NUM 次匹配后停止
        -B [NUM]                打印以文本起始的NUM 行
        -A [NUM]                打印以文本结尾的NUM 行
        -C [NUM]                打印输出文本NUM 行
        -o                      仅显示匹配上的内容（默认显示整行）
        -h                      显示帮助信息
" 
}

while getopts ":hP:m:B:A:C:o" opt 
do 
    case $opt in 
        P) 
            PATTERN="$OPTARG"
            ;; 
        m)
            MAX_COUNT="-m $OPTARG"
            ;;
        B)
            BEFORE_CONTEXT="-B $OPTARG"
            ;;
        A)
            AFTER_CONTEXT="-A $OPTARG"
            ;;
        C)
            CONTEXT="-C $OPTARG"
            ;;
        o)
            ONLY_MATCHING="-o"
            ;;
        h|?)
            usage
            exit 1
            ;; 
    esac 
done 

shift $[$OPTIND-1] 
FILE=$1 
if [ "$FILE" == "" ]; then
    usage
    exit 1
fi

if [ "$PATTERN" == "all" ]; then
    PATTERN=''
elif [ "$PATTERN" == "id" ]; then
    PATTERN='^[1-9]\d{16}[0-9Xx]$'
elif [ "$PATTERN" == "phone" ]; then
    PATTERN='[1][3-9]{1}[0-9]{9}'
elif [ "$PATTERN" == "chinese" ]; then
    PATTERN='[\x{4e00}-\x{9fa5}][\x{4e00}-\x{9fa5}]+'
fi

cat $FILE \
    | perl -C0 -pe 's/[\x00-\x1F\x7F]+/\n/g' \
    | sed -e 's/\xff/\n/g' \
    | awk 'length > 3' \
    | fold -bw 128 \
    | grep -P "$PATTERN" \
        $ONLY_MATCHING \
        $BEFORE_CONTEXT \
        $AFTER_CONTEXT \
        $CONTEXT \
        $MAX_COUNT

