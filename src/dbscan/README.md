通过接口扫描明文个人数据

## 工具目的

基于 JDBC 数据访问接口，实现高效、可审计的个人数据（明文）扫描，帮助团队发现未加密或未脱敏的敏感字段，降低合规与泄露风险。

### 个人信息

个人信息包括但不限于以下类型，如个人姓名、生日、年龄等个人基本资料，身份证、军官证、护照等个人身份信息，人脸、基因、声纹、虹膜、指纹等生物识别信息，用户账号、用户标识符（用户ID）等网络身份标识信息，教育经历、职业、职位等个人教育工作信息，金融账户、消费记录、收入状况、借款信息等个人财产信息，账号口令、数字证书等身份鉴别信息，通信记录、短信、电子邮件等个人通信信息，通讯录、好友列表等联系人信息，网页浏览记录、软件使用记录等个人上网记录，国际移动设备识别码（IMEI）等个人设备信息，交通出行信息等个人位置信息，用户标签、画像信息等个人标签信息，步数、步频等个人运动信息，以及其他与已识别或者可识别的自然人有关的各种信息。

### 敏感个人信息

敏感个人信息是一旦泄露或者非法使用，容易导致自然人的人格尊严受到侵害或者人身、财产安全受到危害的个人信息，包括生物识别、宗教信仰、特定身份、医疗健康、金融账户、行踪轨迹等信息，以及不满十四周岁未成年人的个人信息。

国家标准GB/T 45574-2025《数据安全技术 敏感个人信息处理安全要求》明确了敏感个人信息识别和界定方法，并在附录A中给出常见的敏感个人信息类别，如人脸、基因、声纹等生物识别信息，个人信仰的宗教、加入的宗教组织等宗教信仰信息，残障人士身份信息、不适宜公开的职业身份信息等特定身份信息，病症、既往病史、医疗就诊记录、检验检查数据等医疗健康信息，银行、证券、基金、保险账户的账号及密码等金融账户信息，连续精准定位轨迹信息、车辆行驶轨迹信息等行踪轨迹信息，居民身份证照片、征信信息、犯罪记录信息等其他敏感个人信息。


## 实现方式

- 通过配置文件配置有效的数据库访问凭据（JDBC URL、用户名、密码或受支持的凭证方式）；
- 通过配置文件配置扫描目标列表（格式为[schema.]table）；
- 自动筛选满足目标列表的表；
- 验证数据库访问账号是否具备读取目标的权限；
- 逐表、逐列扫描数据样本部分数据或全量数据（根据配置），识别可能包含个人敏感信息的字段；
- 使用可配置的规则（关键词、正则、数据模式）判断是否为明文个人数据，匹配率超过50%认为命中；
- 输出可供人工复核的扫描报告（包含模式名、表名、列名、命中示例、命中规则）；
- 支持 MySQL、PostgreSQL、SQLServer、Oracle 数据库；
- 明文个人数据包括：身份证、手机号；
- 输入配置文件json格式，输出报告html格式。


## 运行要求

- 有效的数据库访问凭据（JDBC URL、用户名、密码或受支持的凭证方式）；
- 运行扫描程序的网络/权限能访问目标数据库；
- Java 运行时环境（推荐使用与项目构建兼容的 JDK）。

## 安装与构建

本项目使用 Maven 构建。常见步骤：

1. 在项目根目录运行（跳过测试以加快构建）：

```bash
mvn -T1C -DskipTests package
```

2. 打包成功后，生成的 jar 通常在 `target/` 目录下（如有可执行 jar，可按下文运行）。

如果你使用 IDE（如 IntelliJ IDEA），可直接导入 `pom.xml` 并运行/调试主类。

## 配置文件

将数据库连接和扫描规则写入一个配置文件（示例名 `config.json`）。常见配置项：

- `jdbc.type`：数据库的类型：MySQL、PostgreSQL、SQLServer、Oracle；
- `jdbc.url`：JDBC 连接字符串；
- `jdbc.username`：数据库用户名；
- `jdbc.password`：数据库密码；
- `jdbc.ip`：数据库IP；
- `jdbc.port`：数据库端口；
- `jdbc.database`：数据库库名（仅用来登录）；
- `scan.targets`：限定要扫描的目标列表（非mysql格式为[schema.]table，mysql格式为[database.]table）；
- `scan.limit`：每列采样行数（0 表示全量）；
- `scan.rules`：匹配规则集合（关键字、正则、最小/最大长度等）；
- `scan.concurrency`：并发表/分片数以控制扫描吞吐与数据库压力；
- `output.path`：扫描报告输出目录或文件路径；
- `output.sample`：扫描报告采样数量。

示例（JSON 片段）：

```json
{
    "jdbc": {
        "type" : "MySQL",
        "url": "jdbc:mysql://127.0.0.1:3306/db_for_connect",
        "username": "username_for_connect",
        "password": "password_for_connect",
        "ip": "127.0.0.1",
        "port": 3306,
        "database": "db_for_connect"
    },
    "scan" {
        "targets": [
            "target1_schema_or_mysql_database.target_table1",
            "target1_schema_or_mysql_database.target_table2",
            "target2_schema_or_mysql_database.target_table1",
            "target2_schema_or_mysql_database.target_table2"
        ],
        "limit": 10,
        "rules": [
            {
                "name": "phone",
                "description": "手机号",
                "regex": "^(?:0|86|\\+?86)?1[3-9]\\d{9}$"
            },
            {
                "name": "id",
                "description": "身份证号",
                "regex": "\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9] \\d|3)\\d{3}[0-9Xx]$"
            },
            {
                "name": "bank_card",
                "description": "银行卡号",
                "regex": "\\d{16,19}"
            },
            {
                "name": "email",
                "description": "邮箱地址",
                "regex": "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
            }
        ],
        "concurrency": 4
    }
    "output": {
	    "path": "./reports/dbscan-report.html",
        "sample": 5
    }
}
```

## 运行

运行方式取决于项目是否产出可执行 jar：

- 使用 Maven 直接运行（需在 `pom.xml` 配置 `exec-maven-plugin` 或知道主类）：

```bash
mvn exec:java -Dexec.mainClass="com.example.Main" -Dexec.args="--config config.json"
```

- 运行打包好的 jar（若构建为可执行 jar）：

```bash
java -jar target/dbscan-<version>-all.jar --config config.json
```

运行时建议先在开发或测试环境对单个 schema/表进行小范围扫描以验证配置，然后再对生产库进行全量扫描。

## 输出与报告

扫描完成后会生成带完整性校验的 HTML 报告：

- 报告字段示例：
    `IP`，
    `端口`，
    `数据库类型`，
    `库名`， 
    `模式名`， 
    `表名`， 
    `列名`， 
    `抽取总数`，
    `规则名称`，
    `规则描述`，
    `匹配率`，
    `采样`；
- “抽取总数”是 config 中 scan.limit 对应的数量，执行select时使用此数量作为最大抽取行数；
- “规则名称”是 config 中 scan.rules.name ；
- “规则描述”是 config 中 scan.rules.description ；
- “匹配率”是抽取的所有数据中匹配上规则的百分比；
- “采样”的数量是 config 中 scan.output.sample 对应的数量，“采样”数量不会多余“抽取总数”，“采样”的结果是抽取的所有数据中匹配上规则的前 scan.output.sample 条；
- HTML 报告内置 `SHA-256` 校验和，页面打开时自动校验；如报告被篡改，背景变红并提示"内容不完整"；
- 建议将报告保存到受控位置以便审计与后续修复跟踪。

## 使用示例

1. 编辑 `config.json`，填写目标数据库信息与规则；
2. 在开发环境运行（示例）：

```bash
mvn -DskipTests package
java -jar target/dbscan-1.0.0-all.jar --config config.json
```

3. 查看 `./dbscan-report.html`。

## 代码编写要求

- 类名使用大驼峰式（CamelCase），方法名和变量名使用小驼峰式（camelCase）；
- 在关键位置添加注释，减少无用注释数量；
- 所有模块使用 JUnit 进行自动化测试。

## 注意事项与限制

- 对生产数据库执行全量扫描会带来显著 I/O 与 CPU 负担，务必在低峰时间或使用采样；
- 数据库账号应仅授予必要权限，避免使用过高权限的账户直接在生产环境运行未经审计的扫描；
- 规则需不断迭代以减少误报与漏报；
- 对加密或脱敏字段的识别依赖于规则与采样，可能会有漏检。

## 贡献与联系

如需改进或贡献规则集、修复 bug，请提交 PR 或在 issue 中描述复现步骤与配置示例。出现运行问题时，可附上最小可复现配置与日志帮助排查。

## 许可证

项目遵循仓库根目录 `LICENSE` 中指定的开源协议。
