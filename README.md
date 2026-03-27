# Check Tools

面向数据安全检查场景的一组工具，覆盖以下几类工作：

- 数据库明文敏感信息扫描
- 文件可见字符提取与敏感信息检测
- 网络数据节点发现与数据访问发现
- 密文字段格式识别、伪加密分析、弱加密分析
- 生成可交付的 HTML 报告

当前仓库的可执行能力以 4 个 Java 模块为主，配合若干 Shell 脚本完成编排和落地执行。旧版说明中的“7 个独立工具”并不完全符合当前代码状态，例如“密文索引强度分析”在本仓库中尚未落地为可执行模块。

## 项目组成

### 4 个核心模块

| 模块 | 主类 | 打包产物 | 主要用途 |
|------|------|----------|----------|
| dbscan | `com.dbscan.Main` | `dbscan-1.0.0-all.jar` | 扫描数据库中的明文敏感数据 |
| filescan | `com.checktools.filescan.FileScanApp` | `filescan-1.0.0.jar` | 分析文件中的可见字符并识别敏感信息 |
| netscan | `com.netscan.App` | `netscan-1.0.0.jar` | 编排网络主动扫描与流量分析并输出统一报告 |
| saltscan | `com.checktools.saltscan.SaltScanApplication` | `saltscan-1.0.0-all.jar` | 分析密文字段的数据格式、伪加密和弱加密问题 |

### 顶层脚本入口

编译后，根目录会生成 `scripts/`，常用入口如下：

| 脚本 | 作用 |
|------|------|
| `scripts/personal_data_discovery_from_database.sh` | 调用 dbscan 扫描数据库并输出 HTML 报告 |
| `scripts/personal_data_discovery_from_file_report.sh` | 调用 filescan 分析单个文件并输出 HTML 报告 |
| `scripts/auto_analysis_datanode_and_data_accesss.sh` | 调用 netscan 串行执行网络扫描与流量分析 |
| `scripts/ciphertext_data_analysis.sh` | 调用 saltscan 分析密文字段 |
| `scripts/personal_data_discovery_from_file.sh` | 直接提取文件中的可见字符并做 grep/正则匹配 |
| `scripts/datanode_discovery_by_nmap.sh` | 直接执行 Nmap 主动扫描 |
| `scripts/datanode_discovery_by_tcpdump.sh` | 直接执行 tcpdump 数据节点发现 |
| `scripts/data_access_discovery_by_tcpdump.sh` | 直接执行 tcpdump 数据访问发现 |

## 目录结构

```text
checktools/
├── init.sh
├── report/
├── src/
│   ├── dbscan/
│   ├── filescan/
│   ├── netscan/
│   ├── saltscan/
│   ├── images/
│   └── scripts/
├── java/                 # 编译后生成
├── config/               # 编译后生成
└── scripts/              # 编译后生成
```

说明：

- `src/dbscan`、`src/filescan`、`src/netscan`、`src/saltscan` 是 4 个 Maven 子模块。
- `src/scripts` 存放调用各模块 jar 的顶层包装脚本。
- `src/netscan/scripts` 与 `src/filescan/scripts` 存放底层脚本。
- 执行 `./init.sh make` 后会在根目录生成 `java/`、`config/`、`scripts/`、`report/`。

## 环境要求

### 操作系统

`init.sh` 当前已内置多平台依赖安装逻辑，支持：

- Debian / Ubuntu：使用 `apt-get`
- CentOS / RHEL / Fedora：使用 `yum`
- openSUSE / SUSE：使用 `zypper`
- macOS：使用 `brew`

说明：

- 仓库当前没有提供原生 Windows 脚本支持。
- 如果在 Windows 使用，建议自行在 WSL 中运行，但该路径不是本仓库当前脚本的主要测试环境。
- 文件快速扫描脚本依赖 `grep -P`、`perl`、`sed`、`awk`、`fold` 等命令；在 macOS 上通常需要额外安装 GNU grep 或自行调整环境。

### 运行依赖

| 依赖 | 用途 |
|------|------|
| Java 8+ | 运行所有 Java 模块 |
| Maven 3+ | 编译 4 个 Maven 模块 |
| nmap | 主动发现数据节点 |
| tcpdump | 抓取网络流量、识别数据访问与数据节点 |
| Perl / grep / sed / awk / fold | 文件可见字符提取脚本 |

### 数据库支持

当前代码中 dbscan 与 saltscan 都包含以下 JDBC 驱动依赖：

- MySQL
- PostgreSQL
- SQL Server
- Oracle

## 快速开始

### 1. 克隆项目

```bash
git clone https://gitee.com/jay_sjl/checktools.git
cd checktools
```

### 2. 安装运行依赖

```bash
./init.sh
```

无参执行时，脚本会检查并按需安装：

- `nmap`
- `tcpdump`
- `java`

### 3. 编译项目

```bash
./init.sh make
```

该命令会：

1. 检查并安装 `javac` 与 `maven`
2. 依次编译 `dbscan`、`saltscan`、`netscan`、`filescan`
3. 将 jar 复制到根目录 `java/`
4. 将配置样例复制到根目录 `config/`
5. 生成根目录 `scripts/` 入口脚本与符号链接
6. 创建根目录 `report/`

### 4. 清理构建结果

```bash
./init.sh clean
```

该命令会清理：

- 根目录 `java/`
- 根目录 `config/`
- 根目录 `scripts/`
- 各模块 Maven 构建产物

## 常用命令

### 数据库明文扫描

```bash
./scripts/personal_data_discovery_from_database.sh ./config/dbscan/config-mysql-example.json
```

### 文件扫描并生成报告

```bash
./scripts/personal_data_discovery_from_file_report.sh /path/to/file.bin
```

### 文件快速提取与 grep 匹配

```bash
./scripts/personal_data_discovery_from_file.sh -P phone -m 20 /path/to/file.bin
```

### 自动执行网络扫描与流量分析

```bash
./scripts/auto_analysis_datanode_and_data_accesss.sh
```

### 密文数据强度分析

```bash
./scripts/ciphertext_data_analysis.sh ./config/saltscan/config-mysql-example.json
```

## 模块说明

### 1. dbscan：数据库明文敏感信息扫描

### 入口

- 包装脚本：`scripts/personal_data_discovery_from_database.sh`
- 主类：`com.dbscan.Main`
- 直接运行方式：

```bash
java -jar ./java/dbscan-1.0.0-all.jar --config ./config/dbscan/config-mysql-example.json
```

### 主要能力

- 读取 JSON 配置
- 连接数据库并校验连接
- 按目标表逐列扫描样本数据
- 基于正则规则识别敏感信息
- 输出带完整性校验的 HTML 报告

### 配置结构

配置文件位于 `src/dbscan/config/`，编译后会复制到 `config/dbscan/`。

核心字段如下：

```json
{
  "jdbc": {
    "type": "MySQL",
    "url": "jdbc:mysql://127.0.0.1:3306/test",
    "username": "root",
    "password": "password",
    "ip": "127.0.0.1",
    "port": 3306,
    "database": "test"
  },
  "scan": {
    "targets": [
      "schema.table1",
      "schema.table2"
    ],
    "limit": 10,
    "rules": [
      {
        "name": "phone",
        "description": "手机号",
        "regex": "^(?:0|86|\\+?86)?1[3-9]\\d{9}$"
      }
    ],
    "concurrency": 4
  },
  "output": {
    "path": "./report/dbscan-report.html",
    "sample": 5
  }
}
```

### 支持的规则与数据库

- 支持 MySQL、PostgreSQL、Oracle、SQL Server
- 示例规则覆盖手机号、身份证号、银行卡号、邮箱
- 扫描结果报告字段包括 IP、端口、数据库类型、库名、模式名、表名、列名、抽取总数、规则名称、规则描述、匹配率、采样

### 输出特点

- 报告格式为 HTML，不是 CSV
- HTML 内嵌 SHA-256 完整性校验逻辑
- 报告内容被篡改时页面会显示“内容不完整”

### 2. filescan：文件可见字符与敏感信息分析

### 两种使用方式

#### 方式 A：生成 HTML 报告

- 包装脚本：`scripts/personal_data_discovery_from_file_report.sh`
- 主类：`com.checktools.filescan.FileScanApp`

```bash
java -jar ./java/filescan-1.0.0.jar /path/to/file.bin [输出报告路径]
```

功能包括：

- 调用底层脚本提取文件中的可见字符
- 最多分析前 1000000 个可见字符
- 检测身份证号、手机号、邮箱
- 对敏感内容做脱敏展示
- 生成带防篡改校验的 HTML 报告

如果不手工指定输出路径，默认会在输入文件同目录下输出：

```text
report/filescan-report.html
```

#### 方式 B：直接做 grep/正则匹配

- 脚本：`scripts/personal_data_discovery_from_file.sh`

```bash
./scripts/personal_data_discovery_from_file.sh [选项] [文件]
```

支持参数：

- `-P [TYPE|PATTERN]`：`all`、`id`、`phone`、`chinese` 或自定义 PCRE
- `-m [NUM]`：匹配到指定次数后停止
- `-B [NUM]`：显示匹配前文
- `-A [NUM]`：显示匹配后文
- `-C [NUM]`：显示匹配上下文
- `-o`：仅输出匹配内容
- `-h`：帮助信息

这个脚本适合快速排查；如果需要交付报告，请使用 `personal_data_discovery_from_file_report.sh`。

### 3. netscan：网络发现与流量分析汇总

### 入口

- 包装脚本：`scripts/auto_analysis_datanode_and_data_accesss.sh`
- 主类：`com.netscan.App`

```bash
java -jar ./java/netscan-1.0.0.jar
```

### 执行流程

netscan 固定按以下顺序执行：

1. `datanode_discovery_by_nmap.sh`
2. `data_access_discovery_by_tcpdump.sh`
3. `datanode_discovery_by_tcpdump.sh`

处理逻辑如下：

- 第 1 步使用实时输出方式执行 Nmap 主动扫描
- 第 2 步执行 60 秒数据访问发现，达到 1000 条记录可提前停止
- 第 3 步执行 60 秒数据节点发现，按去掉源端口后的去重结果统计，达到 1000 条可提前停止
- 所有结果会统一写入 `report/netscan-report.html`

### 输出结果

netscan 会汇总三类结果：

- Nmap 发现的开放端口
- 抓包识别出的数据访问记录
- 抓包识别出的数据节点记录

程序结束时会直接在控制台打印报告路径。

### 4. 网络底层脚本

### datanode_discovery_by_nmap.sh

```bash
./scripts/datanode_discovery_by_nmap.sh [-p 端口范围] [-T 0-4] [目标]
```

说明：

- 默认端口包含 `80,8080,3306,5432,1433,1521,27017,6379,9200,5236,54321,2881,2883`
- 如果不传目标，脚本会尝试扫描本机与当前网段地址
- 依赖本机已安装 `nmap`

### datanode_discovery_by_tcpdump.sh

```bash
sudo ./scripts/datanode_discovery_by_tcpdump.sh [-p 端口范围]
```

说明：

- 使用 `tcpdump` 被动捕获目标端口流量
- 需要 root 权限
- 默认持续输出，适合作为 netscan 的底层组件或人工观察工具

### data_access_discovery_by_tcpdump.sh

```bash
sudo ./scripts/data_access_discovery_by_tcpdump.sh
```

说明：

- 通过抓包识别 SQL 关键字，例如 `SELECT`、`INSERT`、`UPDATE`、`DELETE`
- 需要 root 权限
- 输出中会包含连接对与 SQL 片段

### 5. saltscan：密文数据强度分析

### 入口

- 包装脚本：`scripts/ciphertext_data_analysis.sh`
- 主类：`com.checktools.saltscan.SaltScanApplication`

```bash
java -jar ./java/saltscan-1.0.0-all.jar --config ./config/saltscan/config-mysql-example.json
```

### 主要能力

- 连接数据库读取目标表列数据
- 自动识别数据格式：HEX、Base64、RAW
- 对解码后的数据做伪加密分析
- 基于重复值统计做弱加密分析
- 为每个目标列生成详细 HTML 报告

### 配置结构

配置文件位于 `src/saltscan/config/`，编译后会复制到 `config/saltscan/`。

核心字段如下：

```json
{
  "jdbc": {
    "type": "MySQL",
    "url": "jdbc:mysql://127.0.0.1:3306/test",
    "username": "root",
    "password": "password",
    "ip": "127.0.0.1",
    "port": 3306,
    "database": "test"
  },
  "scan": {
    "targets": [
      {
        "table": "schema.table",
        "columns": ["column1", "column2"]
      }
    ],
    "limit": 3000,
    "data_length_min": 16,
    "data_length_interval_min": 16,
    "data_variance_max": 1.0,
    "data_repetition_count_max": 5
  },
  "output": {
    "path": "./report/saltscan-report.html"
  }
}
```

### 报告内容

报告会按列输出以下内容：

- 检测到的数据格式
- 总体结论
- 数据长度分布表
- 字节分布热力图
- 归一化方差分析
- 重复值统计与弱加密判断

### 额外模式

主类还支持 `--import`，用于向目标数据库导入测试数据：

```bash
java -jar ./java/saltscan-1.0.0-all.jar --config ./config/saltscan/config-mysql-example.json --import
```

说明：当前代码虽然解析了 `--report` 参数，但最终报告输出仍以配置文件中的 `output.path` 为主。

## 测试情况

当前仓库已包含单元测试，覆盖情况如下：

- dbscan：5 个测试类
- filescan：3 个测试类
- netscan：5 个测试类
- saltscan：4 个测试类

如果需要运行测试，可分别进入子模块目录执行：

```bash
cd src/dbscan && mvn test
cd ../filescan && mvn test
cd ../netscan && mvn test
cd ../saltscan && mvn test
```

## 已知边界与使用建议

- 所有网络扫描与抓包行为都应在授权环境中执行。
- `tcpdump` 相关脚本通常需要 root 权限。
- dbscan 与 saltscan 会访问真实数据库，建议优先使用只读账号与测试环境验证配置。
- filescan 的快速脚本模式依赖系统命令行为；不同操作系统上的表现会受到 grep/perl 实现差异影响。
- netscan 的底层脚本偏向 Linux 环境，macOS 上可运行性取决于本机命令行为兼容程度。
- 当前仓库中没有完整实现“密文索引强度分析”模块，不建议在 README 中继续按已交付功能对外宣称。

## 许可证

本项目采用 GPL License，详见仓库根目录 `LICENSE`。

## 联系方式

- 项目主页：https://gitee.com/jay_sjl/checktools
- 问题反馈：https://gitee.com/jay_sjl/checktools/issues
