# 测试相关说明

本文件包含整体测试相关说明。

## 整体测试的执行流程

### 1. 编译

先进行编译。

```bash
mvn -DskipTests package
```

### 2. 导入数据

运行命令里增加```--import```参数，按照```config-example.json```中```jdbc```内容连接数据，并将测试数据导入到指定测试模式（mysql为库），若测试模式（mysql为库）已经存在了先删除。

```bash
java -jar target/saltscan-1.0.0-all.jar --config ./config/config-mysql-example.json --import
```

### 3. 执行识别

执行识别。

```bash
java -jar target/saltscan-1.0.0-all.jar --config ./config/config-mysql-example.json
```

### 4. 验证结果

对输出的报告进行验证，是否正确。

```bash
java -jar target/saltscan-1.0.0-all.jar --config ./config/config-mysql-example.json --report ./report.html
```

## 测试数据内容

一定要保证以下生成的数据每次执行生成的内容都是一样的。

### 1 测试模式

创建测试模式（mysql为库），名称为 test_schema 。

### 2. 数据编码识别

生成一张表，表名为 encode_test_data ；
该表包含如下字段：
- idcard: 生成随机身份证号数据1000条（要求统计重复值，前20均值在5到10之间）；
- idcard_hex: idcard 内容转 hex；
- idcard_base64: idcard 内容转 base64。

### 3. 伪加密测试数据

生成一张表，表名为 pseudo_encryption_test_data ；
该表包含如下字段：
- address: 生成随机长度中文地址数据1000条，地址长度最短4字节，最长100字节（要求统计重复值，前20均值在5到10之间）；
- address_hex: address 内容转 hex ；
- address_base64: address 内容转 base64；
- address_xor_hex: address 内容按字节异或 0xAA 后转 hex ；
- address_xor_base64: address 内容按字节异或 0xAA 后转 base64 。

### 4. 弱加密测试数据

生成一张表，表名为 weak_encryption_test_data ；
该表包含如下字段：
- name: 生成随机中文姓名数据1000条（要求统计重复值，前20均值在5到10之间）；
- name_encrypted: 对 name 内容进行 AES128 加密，密钥使用"1234567890ABCDEF"，不要加盐, 二进制存储；
- name_encrypted_hex: name_encrypted 转 hex ；
- name_encrypted_base64: name_encrypted 转 base64 。

### 5. 强加密测试数据

生成一张表，表名为 strong_encryption_test_data ；
该表包含如下字段：
- phone: 生成随机手机号数据1000条，字符串方式存储（要求统计重复值，前20均值在5到10之间）；
- phone_encrypted: 对 phone 内容前追加8给字节随机内容，然后进行 AES128 加密，密钥使用"1234567890ABCDEF"，二进制存储；
- phone_encrypted_hex: phone_encrypted 转 hex ;
- phone_encrypted_base64: phone_encrypted 转 base64 。

## 测试用例说明

### 用例 1: 编码检测
- **目标**: 验证系统能否识别RAW数据、HEX数据/Base64编码数据
- **数据特征**: 分别为"未检测到编码"、"十六进制编码"、"Base64编码"
- **预期结果**: 报告应分别标记为"未检测到编码"、"十六进制编码"、"Base64编码"

### 用例 2: 伪加密检测
- **目标**: 验证系统能否识别RAW数据、HEX/Base64编码的伪加密数据、XOR异或处理的伪加密数据
- **数据特征**: 可还原且能识别原始模式
- **预期结果**: 报告应标记为"伪加密"

### 用例 3: 弱加密检测
- **目标**: 验证系统能否识别相同明文对应相同密文
- **数据特征**: 高重复率、模式明显
- **预期结果**: 报告应标记为"弱加密"

### 用例 4: 强加密数据
- **目标**: 验证系统不误判强加密数据
- **数据特征**: 随机分布、低重复率
- **预期结果**: 报告应标记为"安全加密"

## 预期输出

### 伪加密场景的报告内容

```
检测详情：
检测时间：XXXX年XX月XX日
检测数据库：jdbc中相关描述
检测内容：
    1. 模式（如果有）.表名.列名 （未检测到编码、⚠️检测到伪加密、⚠️检测到弱加密）
    2. 模式（如果有）.表名.列名 （十六进制编码、✅ 未检测到伪加密、⚠️检测到弱加密）
    3. 模式（如果有）.表名.列名 （Base64编码、⚠️检测到伪加密、✅ 未检测到弱加密）
    ...

1. 模式（如果有）.表名.列名 （要能折叠起来，在看的时候点击展开，默认是折叠的）
    - 总体报告
    - 编码方式报告
    - 伪加密检测报告
    - 弱加密检测报告

2. 模式（如果有）.表名.列名 
    ...

...
```

### 总体报告格式

```
编码方式：未检测到编码、十六进制编码、Base64编码 （三选一）；
伪加密检测：⚠️ 检测到伪加密、✅ 未检测到伪加密 （二选一）；
弱加密检测：⚠️ 检测到弱加密、✅ 未检测到弱加密 （二选一）；
（用小字在下面标注：根据检测算法，明文数据也可能被检测成“伪加密”或“弱加密”）
```

### 伪加密报告内容

```
1. 数据分析：
    - 数据长度分布表
    - 数据分布热力图

2. 伪加密分析:
    - 对于一些常用的算法，真正加密数据的长度通常为阶跃性的，两种不同长度的数据长度间隔应该大于等于<config.json中scan.data_length_interval_min，默认16>字节。并且数据的最小长度也应该大于等于<config.json中scan.data_length_min，默认16>。
    最小长度间隔为 2，长度间隔偏低，为密文的可能性较低。（最小长度间隔2为例）
    最小长度间隔为 16，长度间隔正常，为密文的可能性较高。（最小长度间隔16为例）
    所有长度都为 16，长度间隔正常，为密文的可能性较高。（长度都是16为例）
    最小长度为 9，长度偏低，为密文的可能性较低。（最小长度9为例）
    最小长度为 16，长度正常，为密文的可能性较高。（最小长度16为例）

    - 数据分布热力图代表数据分布情况，计算这个256个数字的归一化方差。以<config.json中scan.data_variance_max，默认1.0>为判断阈值，归一化方差大于等于<config.json中scan.data_variance_max，默认1.0>认为是伪加密。
    归一化方差为 2.0，归一化方差较大，为密文的可能性较低。（归一化方差2.0为例）
    归一化方差为 0.05，归一化方差较小，为密文的可能性较大。（归一化方差0.05为例）

3. 结论：
    ⚠️ 检测到伪加密（最小长度小于 scan.data_length_min 或长度不等且最小长度间隔小于 scan.data_length_interval_min 或归一化方差大于等于 scan.data_variance_max）
    ✅ 未检测到伪加密（长度不等切最小长度大于等于 scan.data_length_min 并且最小长度间隔大于等于 scan.data_length_interval_min 并且归一化方差小于 scan.data_variance_max）
```

### 弱加密场景的报告内容

```
1. 数据分析：
    前20个重复值统计（抽取总数1000）:
    1. e10adc3949ba59abbe56e057f20f883e - 125次 (12.50%)
    2. 21232f297a57a5a743894a0e4a801fc3 - 95次 (9.50%)
    3. e99a18c428cb38d5f260853678922e03 - 80次 (8.00%)
    ...

2. 弱加密分析：
    真正加盐的加密数据重复率往往是很低的，出现相同数据可以断言密文数据没有加盐。重复次数大于等于<config.json中scan.data_repetition_count_max，默认5>次则认为数据没有加盐。
    重复值统计最高值为 2，重复率较低，为加盐密文的可能性较高。（重复值统计最高值2为例）
    重复值统计最高值为 125，重复率较高，为加盐密文的可能性较低。（重复值统计最高值125为例）

3. 结论：
    ⚠️ 检测到弱加密（重复值统计最高值大于等于 scan.data_repetition_count_max）
    ✅ 未检测到弱加密（重复值统计最高值小于 scan.data_repetition_count_max）
```

## 参考结果

encode_test_data: 
- idcard: 未检测到编码、检测到伪加密、检测到弱加密；
- idcard_hex: 未检测到编码、检测到伪加密、检测到弱加密；
- idcard_base64: Base64编码、检测到伪加密、检测到弱加密。

pseudo_encryption_test_data:
- address: 未检测到编码、检测到伪加密、检测到弱加密；
- address_hex: 十六进制编码、检测到伪加密、检测到弱加密；
- address_base64: Base64编码、检测到伪加密、检测到弱加密；
- address_xor_hex: 十六进制编码、检测到伪加密、检测到弱加密；
- address_xor_base64: Base64编码、检测到伪加密、检测到弱加密。

weak_encryption_test_data:
- name: 未检测到编码、检测到伪加密、检测到弱加密；
- name_encrypted: 未检测到编码、未检测到伪加密、检测到弱加密；
- name_encrypted_hex: 十六进制编码、未检测到伪加密、检测到弱加密；
- name_encrypted_base64: Base64编码、未检测到伪加密、检测到弱加密。

strong_encryption_test_data:
- phone: 未检测到编码、检测到伪加密、检测到弱加密；
- phone_encrypted: 未检测到编码、未检测到伪加密、未检测到弱加密；
- phone_encrypted_hex: 十六进制编码、未检测到伪加密、未检测到弱加密;
- phone_encrypted_base64: Base64编码、未检测到伪加密、未检测到弱加密。
