#!/bin/bash

cd $(dirname "$0")

# 检测操作系统类型
function detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if [ -f /etc/os-release ]; then
            . /etc/os-release
            if [[ "$ID" == "ubuntu" || "$ID" == "debian" ]]; then
                echo "ubuntu"
            elif [[ "$ID" == "centos" || "$ID" == "rhel" || "$ID" == "fedora" ]]; then
                echo "centos"
            elif [[ "$ID" == "opensuse" || "$ID" == "suse" ]]; then
                echo "suse"
            else
                echo "linux"
            fi
        else
            echo "linux"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    else
        echo "unknown"
    fi
}

# 获取包管理器命令
function get_package_manager() {
    local os=$1
    case "$os" in
        ubuntu)
            echo "apt-get"
            ;;
        centos)
            echo "yum"
            ;;
        suse)
            echo "zypper"
            ;;
        macos)
            echo "brew"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

# 安装软件包
function install_package() {
    local os=$1
    local package=$2
    local pm=$(get_package_manager "$os")
    
    if [ "$pm" == "unknown" ]; then
        echo "无法识别操作系统，请手动安装 $package"
        return 1
    fi
    
    echo "正在使用 $pm 安装 $package..."
    
    case "$pm" in
        apt-get)
            sudo apt-get update
            sudo apt-get install -y "$package"
            ;;
        yum)
            sudo yum install -y "$package"
            ;;
        zypper)
            sudo zypper install -y "$package"
            ;;
        brew)
            brew install "$package"
            ;;
    esac
}

# 获取Java开发工具包包名
function get_java_devel_package() {
    local os=$1
    case "$os" in
        ubuntu)
            echo "default-jdk"
            ;;
        centos)
            echo "java-1.8.0-openjdk-devel"
            ;;
        suse)
            echo "java-1_8_0-openjdk-devel"
            ;;
        macos)
            echo "openjdk@8"
            ;;
        *)
            echo "java"
            ;;
    esac
}

# 获取Java运行环境包名
function get_java_runtime_package() {
    local os=$1
    case "$os" in
        ubuntu)
            echo "default-jre"
            ;;
        centos)
            echo "java-1.8.0-openjdk"
            ;;
        suse)
            echo "java-1_8_0-openjdk"
            ;;
        macos)
            echo "openjdk@8"
            ;;
        *)
            echo "java"
            ;;
    esac
}

function usage() {
    echo "Usage: $0 [make|clean]"
    echo "$0        安装运行依赖"
    echo "$0 make   安装编译依赖并编译"
    echo "$0 clean  清理编译生成的文件"
    exit 1
}   

if [[ $# -gt 1 ]]; then
    usage
    exit 1
fi

if [[ $# -eq 1 ]]; then
    if [[ "$1" != "make" && "$1" != "clean" ]]; then
        usage
        exit 1
    fi
fi

OS_TYPE=$(detect_os)

if [ $# -eq 0 ]; then
    NMAP_EXISTS=`which nmap | wc -l`
    if [ "$NMAP_EXISTS" == "0" ]; then
        install_package "$OS_TYPE" "nmap"
    fi

    TCPDUMP_EXISTS=`which tcpdump | wc -l`
    if [ "$TCPDUMP_EXISTS" == "0" ]; then
        install_package "$OS_TYPE" "tcpdump"
    fi

    JAVA_EXISTS=`which java | wc -l`
    if [ "$JAVA_EXISTS" == "0" ]; then
        JAVA_PACKAGE=$(get_java_runtime_package "$OS_TYPE")
        install_package "$OS_TYPE" "$JAVA_PACKAGE"
    fi

    exit 0
fi

if [ "$1" == "clean" ]; then
    rm -rf ./java
    rm -rf ./config
    rm -rf ./scripts
    pushd src/dbscan/
        mvn clean
    popd
    pushd src/saltscan/
        mvn clean
    popd
    pushd src/netscan/
        mvn clean
    popd
    pushd src/filescan/
        mvn clean
    popd
    exit 0
fi

JAVAC_EXISTS=`which javac | wc -l`
if [ "$JAVAC_EXISTS" == "0" ]; then
    JAVA_DEVEL_PACKAGE=$(get_java_devel_package "$OS_TYPE")
    install_package "$OS_TYPE" "$JAVA_DEVEL_PACKAGE"
fi
 
MAVEN_EXISTS=`which mvn | wc -l`
if [ "$MAVEN_EXISTS" == "0" ]; then
    install_package "$OS_TYPE" "maven"
fi

pushd src/dbscan/
    mvn clean package
popd

pushd src/saltscan/
    mvn clean package
popd

pushd src/netscan/
    mvn clean package
popd

pushd src/filescan/
    mvn clean package
popd

rm -rf ./java
rm -rf ./config
rm -rf ./scripts

mkdir -p report/
mkdir -p java/
mkdir -p config/dbscan/
mkdir -p config/saltscan/
mkdir -p scripts/
cp src/scripts/*.sh scripts/

cp src/dbscan/target/dbscan-*-all.jar java/
cp src/dbscan/config/*.json config/dbscan/

cp src/saltscan/target/saltscan-*-all.jar java/
cp src/saltscan/config/*.json config/saltscan/

cp src/netscan/target/netscan-*.jar java/
cp -r src/netscan/scripts java/.

cp src/filescan/target/filescan-*.jar java/
cp -r src/filescan/scripts java/.

ln -s java/scripts/data_access_discovery_by_tcpdump.sh  scripts/data_access_discovery_by_tcpdump.sh
ln -s java/scripts/datanode_discovery_by_nmap.sh        scripts/datanode_discovery_by_nmap.sh
ln -s java/scripts/datanode_discovery_by_tcpdump.sh     scripts/datanode_discovery_by_tcpdump.sh
ln -s java/scripts/personal_data_discovery_from_file.sh scripts/personal_data_discovery_from_file.sh

