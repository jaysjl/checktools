网络中发现主机节点

# 项目要求
1. Java实现、maven管理项目、使用java-1.8.0；
2. 关键位置加上注释。

# 需求描述

1. 软件执行 java -jar netscan.jar 启动，不要有参数；
2. 启动之后在控制台输出软件描述，做的好看一些；
3. 依次调用以下脚本：
    - datanode_discovery_by_nmap.sh
    - data_access_discovery_by_tcpdump.sh
    - datanode_discovery_by_tcpdump.sh
4. 调用 datanode_discovery_by_nmap.sh 的时候控制台要有输出，让用户知道软件没有卡死；
5. 调用 data_access_discovery_by_tcpdump.sh 执行一分钟，执行中候控制台显示进度；
6. data_access_discovery_by_tcpdump.sh 执行结果达到1000条则提前停止；
7. 调用 datanode_discovery_by_tcpdump.sh 执行一分钟，执行中候控制台显示进度；
8. datanode_discovery_by_tcpdump.sh 执行结果中忽略“源端口”，然后去除重复，去除重复后总条数达到1000条则提前停止；
9. 执行结束给用户提示执行结束，请下载报告，并给出报告地址；
10. 报告是html格式，内容是三个脚本的输出。

# 脚本结果示例

## datanode_discovery_by_nmap.sh
```
Discovered open port 8080/tcp on 127.0.0.1
8080/tcp  open   http-proxy
Discovered open port 3306/tcp on 192.168.48.204
3306/tcp  open   mysql
Discovered open port 8080/tcp on 192.168.48.219
8080/tcp  open   http-proxy
```

## data_access_discovery_by_tcpdump.sh
```
[1;31m=== 192.168.48.204.3306 > 192.168.48.1.52544 ===[0m
[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===
.....SHOW VARIABLES LIKE 'lower_case_%'; SHOW VARIABLES LIKE 'sql_mode'; SELECT COUNT(*) AS support_ndb FROM information_schema.ENGINES WHERE Engine = 'ndbcluster'
--
[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===[0m
h....SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA
--
[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===[0m
.....SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'information_schema' UNION SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'information_schema' UNION SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = 'information_schema'
--
[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===[0m
.....SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'information_schema' ORDER BY TABLE_SCHEMA, TABLE_TYPE
--
[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===[0m
.....SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'information_schema' ORDER BY TABLE_SCHEMA, TABLE_NAME
--
[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===[0m
CLIENT_STATISTICS.ROWS_UPDATED
--
```

## datanode_discovery_by_tcpdump.sh
```
192.168.48.1.50016 > 192.168.48.204.3306
192.168.48.1.50017 > 192.168.48.204.3306
192.168.48.1.57788 > 192.168.48.204.3306
192.168.48.1.57788 > 192.168.48.204.3306
192.168.48.1.57789 > 192.168.48.204.3306
```