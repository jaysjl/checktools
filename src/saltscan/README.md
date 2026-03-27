## 密文数据强度分析工具

> **核心目的**：分析加密字段内容，精准识别伪加密与弱加密问题。

- **实现方式**
    1.  连接数据库，读取指定的密文字段数据。
    2.  对加密内容进行统计分析，评估其加密强度与合规性。

- **权限要求**
    - 具备目标数据库的连接权限以及查询所需数据的权限。

#### 关键词语解释

- **伪加密**
    - 指通过转换成16进制、base64编码、异或以及其他不需要密钥就可以进行的转码操作，这种转码看起来与原始数据不同，但原始数据的特征还保留其中。

- **弱加密**
    - 指加密中未加盐，也就是同一明文一定得到相同的密文。

## 实现方式

- 通过配置文件配置有效的数据库访问凭据（JDBC URL、用户名、密码或受支持的凭证方式）；
- 通过配置文件配置扫描目标列表（格式为[schema.]table）；
- 验证数据库访问账号是否具备读取目标的权限；
- 输出HTML格式的扫描报告；
- 支持 MySQL、PostgreSQL、SQLServer、Oracle 数据库；
- 输入配置文件json格式；
- 支持数据格式分析支持hex、base64、raw；
- 支持伪加密分析；
- 支持弱加密分析；

#### 数据格式分析
分析数据存储方式：1）hex；2）base64；3）raw，将数据还原之后给伪加密分析和弱加密分析使用。

- 检测十六进制编码时，不要将纯数字误判为十六进制编码，不包含A-F、a-f仅包含0-9的字符串不是十六进制编码，应该是未检测到编码！

- hex 和 base64 类型，数据的内容一定是字符串。

#### 伪加密分析

1. 给出数据长度分布表和分布图；
2. 给出数据字节分布频率表和频率图，计算出归一化方差；

根据以上三点分析是否为伪加密，并给出原因，结果输出到html的报告中。

#### 弱加密分析

统计数据中重复值的数量，提取重复最高的前20个；
根据重复率判断数据是否加盐，给出判断结果到html的报告中。

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
- `jdbc.database`：数据库库名；
- `scan.targets.table`：限定要扫描的目标列表（非mysql格式为[schema.]table，mysql格式为[database.]table）；
- `scan.targets.columns`：限定要扫描的目标列列；
- `scan.limit`：最大采样行数（0 表示全量，默认 3000）；
- `scan.data_length_min`：伪加密分析中的数据最小长度;
- `scan.data_length_interval_min`：伪加密分析中的数据最小长度间隔;
- `scan.data_variance_max`：伪加密分析中的归一化方差阈值;
- `scan.data_repetition_count_max`：弱加密分析中的重复值统计最高值阈值;
- `output.path`：扫描报告输出目录或文件路径。

注意，jdbc下的内容仅作为连接使用，与下面的扫描内容无关。

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
            {
                "table": "target_schema_or_mysql_database.target_table1",
                "columns": [
                    "columns1", 
                    "columns2",
                    "columns3"
                ]
            }
        ],
        "limit": 3000,
        "data_length_min": 16,
        "data_length_interval_min": 16,
        "data_variance_max": 1.0,
        "data_repetition_count_max": 5
    }
    "output": {
	    "path": "./report.html"
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
java -jar target/saltscan-<version>.jar --config config.json
```

运行时建议先在开发或测试环境对单个 schema/表进行小范围扫描以验证配置，然后再对生产库进行全量扫描。

## 输出与报告

输出报告html格式，可以包含多个字段的结果，每个字段包含如下内容：
1. 该字段的详细信息，IP、端口、库、模式（如果有）、表
2. 分析目标列存储方式结果：hex、base64、raw，按照这个分析结果还原数据给下面分析使用，下面内容都是在还原后的数据上得出的；
3. 数据长度分布图，二维图表显示，数据长度为按照1方式还原数据后的长度；
5. 计算字节分布热力图，统计0~255每个字节出现的次数，绘制16×16统计表和热力图，计算出归一化方差；
6. 统计数据中重复值的数量，显示重复最高的前20个内容和数量，内容显示数据库中原始内容。

16×16统计表和热力图的具体显示形式：
1. 显示一个16×16的表格，表格的上面第1列到第16列显示00到0F，表格的左面第1行到第16行显示00到F0；
2. 表格内显示具体的统计次数；
3. 表格背景颜色显示[红、橙、黄、绿、蓝、灰]，数字为0的格背景为[灰]，设表格所有数的均值为m，(0,m/3]背景显示[蓝]，(m/3,m\*2/3]背景显示[绿]，(m\*2/3,m]背景显示[黄]，(m,m\*4/3]背景显示[橙]，(m\*4/3,+∞)背景显示[红]。 图例中将m/3、m\*2/3、m、m\*4/3要替换成具体的数值。

每个输出的结果都要给出指标解释、结论性输出、得出结论的依据。

报告输出内容也参考 OVERALL_TEST.md 中样例。

## 整体测试

参考 OVERALL_TEST.md 中描述编写整体测试代码，并进行测试。

## 使用示例

1. 编辑 `config.json`，填写目标数据库信息与规则；
2. 在开发环境运行（示例）：

```bash
mvn -DskipTests package
java -jar target/saltscan-1.0.0.jar --config config.json
```

3. 查看 `./report.html`。

## 代码编写要求

- 类名使用大驼峰式（CamelCase），方法名和变量名使用小驼峰式（camelCase）；
- 在关键位置添加注释，减少无用注释数量；
- 所有模块使用 JUnit 进行自动化测试；
- 日志直接打印在终端，不要保存到文件中，不要中文乱码。

## 注意事项与限制

- 对生产数据库执行全量扫描会带来显著 I/O 与 CPU 负担，务必在低峰时间或使用采样；
- 数据库账号应仅授予必要权限，避免使用过高权限的账户直接在生产环境运行未经审计的扫描；
- 规则需不断迭代以减少误报与漏报；
- 对加密或脱敏字段的识别依赖于规则与采样，可能会有漏检。

## 贡献与联系

如需改进或贡献规则集、修复 bug，请提交 PR 或在 issue 中描述复现步骤与配置示例。出现运行问题时，可附上最小可复现配置与日志帮助排查。

## 许可证

项目遵循仓库根目录 `LICENSE` 中指定的开源协议。
