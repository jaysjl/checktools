package com.checktools.saltscan.testdata;

import com.checktools.saltscan.config.ConfigManager;
import com.checktools.saltscan.db.DatabaseConnector;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/**
 * 测试数据生成器 - 生成测试数据表和数据
 */
public class TestDataGenerator {
    private static final String AES_KEY = "1234567890ABCDEF";
    private static final Random RANDOM = new Random(42);  // 使用固定种子保证数据一致性
    private static final String TEST_SCHEMA = "test_schema";

    private final DatabaseConnector connector;
    private final ConfigManager configManager;

    public TestDataGenerator(DatabaseConnector connector, ConfigManager configManager) {
        this.connector = connector;
        this.configManager = configManager;
    }

    /**
     * 导入所有测试数据
     */
    public void importTestData() throws Exception {
        String schema = "test_schema";
        
        // 删除并重新创建测试schema
        recreateSchema(schema);
        
        // 生成各种测试数据
        generateEncodeTestData();
        System.out.println("[INFO] 编码检测测试数据生成完成");
        
        generatePseudoEncryptionTestData();
        System.out.println("[INFO] 伪加密测试数据生成完成");
        
        generateWeakEncryptionTestData();
        System.out.println("[INFO] 弱加密测试数据生成完成");
        
        generateStrongEncryptionTestData();
        System.out.println("[INFO] 强加密测试数据生成完成");
    }

    /**
     * 删除并重新创建Schema/数据库 - 支持多数据库
     * MySQL创建数据库，其他数据库创建schema
     */
    private void recreateSchema(String schema) throws Exception {
        String dbType = configManager.getJdbcType().toLowerCase();
        Connection conn = connector.getConnection();
        
        try (Statement stmt = conn.createStatement()) {
            // 根据数据库类型执行不同的SQL
            switch(dbType) {
                case "mysql":
                    stmt.executeUpdate("DROP DATABASE IF EXISTS " + schema);
                    stmt.executeUpdate("CREATE DATABASE " + schema);
                    stmt.executeUpdate("USE " + schema);
                    break;
                    
                case "postgresql":
                    stmt.executeUpdate("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
                    stmt.executeUpdate("CREATE SCHEMA " + schema);
                    stmt.executeUpdate("SET search_path TO " + schema);
                    break;
                    
                case "sqlserver":
                    stmt.executeUpdate("IF SCHEMA_ID('" + schema + "') IS NOT NULL DROP SCHEMA [" + schema + "]");
                    stmt.executeUpdate("CREATE SCHEMA [" + schema + "]");
                    break;
                    
                case "oracle":
                    // Oracle使用当前schema，需要删除表
                    stmt.executeUpdate("BEGIN FOR r IN (SELECT table_name FROM user_tables) LOOP EXECUTE IMMEDIATE 'DROP TABLE '||r.table_name; END LOOP; END;");
                    break;
                    
                default:
                    throw new UnsupportedOperationException("不支持的数据库类型: " + dbType);
            }
            
            
            System.out.println("[INFO] Schema " + schema + " 已重新创建（" + dbType + "）");
        }
    }

    /**
     * 获取带schema前缀的表名
     */
    private String getTableName(String tableBaseName) {
        String dbType = configManager.getJdbcType().toLowerCase();
        
        if ("mysql".equals(dbType)) {
            // MySQL：database.table 或 仅 table（因为已执行USE）
            return tableBaseName;
        } else if ("postgresql".equals(dbType)) {
            // PostgreSQL：schema.table
            return TEST_SCHEMA + "." + tableBaseName;
        } else if ("sqlserver".equals(dbType)) {
            // SQL Server：[schema].[table]
            return "[" + TEST_SCHEMA + "].[" + tableBaseName + "]";
        } else if ("oracle".equals(dbType)) {
            // Oracle：OWNER.TABLE（大写）
            return tableBaseName.toUpperCase();
        }
        
        return tableBaseName;
    }

    /**
     * 生成CREATE TABLE语句中的主键部分
     */
    private String getPrimaryKeyDefinition() {
        String dbType = configManager.getJdbcType().toLowerCase();
        
        switch(dbType) {
            case "postgresql":
                return "id BIGSERIAL PRIMARY KEY";
            case "sqlserver":
                return "id INT PRIMARY KEY IDENTITY(1,1)";
            case "oracle":
                return "id NUMBER PRIMARY KEY";
            case "mysql":
            default:
                return "id INT AUTO_INCREMENT PRIMARY KEY";
        }
    }

    /**
     * 获取兼容多数据库的字符编码声明
     */
    private String getCharsetClause() {
        String dbType = configManager.getJdbcType().toLowerCase();
        
        // MySQL和SQLServer支持字符集，PostgreSQL和Oracle使用数据库级别设置
        if ("mysql".equals(dbType)) {
            return " CHARACTER SET utf8mb4";
        } else if ("sqlserver".equals(dbType)) {
            return " COLLATE Latin1_General_CI_AS"; // 或其他合适的整理规则
        }
        // PostgreSQL和Oracle不需要列级字符集
        return "";
    }

    /**
     * 获取兼容多数据库的BLOB数据类型
     */
    private String getBlobType() {
        String dbType = configManager.getJdbcType().toLowerCase();
        
        switch(dbType) {
            case "mysql":
                return "LONGBLOB";
            case "postgresql":
                return "BYTEA";
            case "sqlserver":
                return "VARBINARY(MAX)";
            case "oracle":
                return "BLOB";
            default:
                return "LONGBLOB";
        }
    }

    /**
     * 生成编码检测测试数据表
     */
    private void generateEncodeTestData() throws Exception {
        Connection conn = connector.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // 创建表
            String tableName = getTableName("encode_test_data");
            String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    getPrimaryKeyDefinition() + "," +
                    "idcard VARCHAR(255)," +
                    "idcard_hex VARCHAR(1024)," +
                    "idcard_base64 VARCHAR(1024)" +
                    ")";
            stmt.executeUpdate(createTableSql);

            // 生成1000条身份证数据 - 前20个值的平均出现次数在5-10之间
            int avgFrequency = 7;  // 平均出现次数：7次
            List<String> idcards = generateIdcards(1000, avgFrequency);
            
            // 使用PreparedStatement实现跨数据库兼容
            String insertSql = "INSERT INTO " + tableName + " (idcard, idcard_hex, idcard_base64) VALUES (?, ?, ?)";
            java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql);
            
            for (String idcard : idcards) {
                String hex = stringToHex(idcard);
                String base64 = stringToBase64(idcard);
                
                pstmt.setString(1, idcard);
                pstmt.setString(2, hex);
                pstmt.setString(3, base64);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            pstmt.close();
            System.out.println("[INFO] 生成 1000 条编码检测测试数据");
        }
    }

    /**
     * 生成伪加密测试数据表
     */
    private void generatePseudoEncryptionTestData() throws Exception {
        Connection conn = connector.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // 创建表
            String tableName = getTableName("pseudo_encryption_test_data");
            String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    getPrimaryKeyDefinition() + "," +
                    "address VARCHAR(255)" + getCharsetClause() + "," +
                    "address_hex VARCHAR(1024)," +
                    "address_base64 VARCHAR(1024)," +
                    "address_xor_hex VARCHAR(1024)," +
                    "address_xor_base64 VARCHAR(1024)" +
                    ")";
            stmt.executeUpdate(createTableSql);

            // 生成1000条随机长度中文地址 - 前20个值的平均出现次数在5-10之间
            int avgFrequency = 7;  // 平均出现次数：7次
            List<String> addresses = generateChineseAddresses(1000, avgFrequency);
            
            String insertSql = "INSERT INTO " + tableName + 
                    "(address, address_hex, address_base64, address_xor_hex, address_xor_base64) VALUES (?, ?, ?, ?, ?)";
            
            java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql);
            for (String address : addresses) {
                String hex = stringToHex(address);
                String base64 = stringToBase64(address);
                byte[] xorBytes = xorBytes(address.getBytes(StandardCharsets.UTF_8), (byte) 0xAA);
                String xorHex = bytesToHex(xorBytes);
                String xorBase64 = Base64.getEncoder().encodeToString(xorBytes);
                
                pstmt.setString(1, address);
                pstmt.setString(2, hex);
                pstmt.setString(3, base64);
                pstmt.setString(4, xorHex);
                pstmt.setString(5, xorBase64);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            
            System.out.println("[INFO] 生成 1000 条伪加密测试数据");
        }
    }

    /**
     * 生成弱加密测试数据表
     */
    private void generateWeakEncryptionTestData() throws Exception {
        Connection conn = connector.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // 创建表
            String tableName = getTableName("weak_encryption_test_data");
            String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    getPrimaryKeyDefinition() + "," +
                    "name VARCHAR(255)" + getCharsetClause() + "," +
                    "name_encrypted " + getBlobType() + "," +
                    "name_encrypted_hex VARCHAR(1024)," +
                    "name_encrypted_base64 VARCHAR(1024)" +
                    ")";
            stmt.executeUpdate(createTableSql);

            // 生成1000条随机中文姓名 - 前20个值的平均出现次数在5-10之间
            int avgFrequency = 7;  // 平均出现次数：7次
            List<String> names = generateChineseNames(1000, avgFrequency);
            
            String insertSql = "INSERT INTO " + tableName + 
                    "(name, name_encrypted, name_encrypted_hex, name_encrypted_base64) VALUES (?, ?, ?, ?)";
            
            java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql);
            for (String name : names) {
                byte[] encrypted = encryptAES(name);
                String encryptedHex = bytesToHex(encrypted);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
                
                pstmt.setString(1, name);
                pstmt.setBytes(2, encrypted);
                pstmt.setString(3, encryptedHex);
                pstmt.setString(4, encryptedBase64);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            
            System.out.println("[INFO] 生成 1000 条弱加密测试数据");
        }
    }

    /**
     * 生成强加密测试数据表
     */
    private void generateStrongEncryptionTestData() throws Exception {
        Connection conn = connector.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // 创建表
            String tableName = getTableName("strong_encryption_test_data");
            String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    getPrimaryKeyDefinition() + "," +
                    "phone VARCHAR(255)," +
                    "phone_encrypted " + getBlobType() + "," +
                    "phone_encrypted_hex VARCHAR(1024)," +
                    "phone_encrypted_base64 VARCHAR(1024)" +
                    ")";
            stmt.executeUpdate(createTableSql);

            // 生成1000条随机手机号 - 前20个值的平均出现次数在5-10之间
            int avgFrequency = 7;  // 平均出现次数：7次
            List<String> phones = generatePhoneNumbers(1000, avgFrequency);
            
            String insertSql = "INSERT INTO " + tableName + " " +
                    "(phone, phone_encrypted, phone_encrypted_hex, phone_encrypted_base64) VALUES (?, ?, ?, ?)";
            
            java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql);
            for (String phone : phones) {
                // 前追加8个字节随机内容，然后加密
                byte[] randomBytes = new byte[8];
                RANDOM.nextBytes(randomBytes);
                byte[] phoneBytes = phone.getBytes(StandardCharsets.UTF_8);
                byte[] combined = new byte[randomBytes.length + phoneBytes.length];
                System.arraycopy(randomBytes, 0, combined, 0, randomBytes.length);
                System.arraycopy(phoneBytes, 0, combined, randomBytes.length, phoneBytes.length);
                
                byte[] encrypted = encryptAESBytes(combined);
                String encryptedHex = bytesToHex(encrypted);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
                
                pstmt.setString(1, phone);
                pstmt.setBytes(2, encrypted);
                pstmt.setString(3, encryptedHex);
                pstmt.setString(4, encryptedBase64);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            
            System.out.println("[INFO] 生成 1000 条强加密测试数据");
        }
    }

    // ================== 辅助方法 ==================

    /**
     * 生成随机身份证号（前20个值的平均出现次数在5-10之间）
     */
    private List<String> generateIdcards(int count, int avgFrequencyTop20) {
        List<String> idcards = new ArrayList<>();
        // 前20个高频值的个数
        int highFreqCount = 20;
        // 每个高频值出现的次数
        int frequencyPerValue = avgFrequencyTop20;
        // 前20个值总共占用的数据个数
        int top20Count = highFreqCount * frequencyPerValue;
        // 剩余数据个数（这些数据来自其他唯一值，每个值出现1次）
        int remainingCount = count - top20Count;
        
        // 生成所有唯一值
        List<String> uniqueIdcards = new ArrayList<>();
        // 前20个高频值
        for (int i = 0; i < highFreqCount; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 18; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            uniqueIdcards.add(sb.toString());
        }
        // 剩余的唯一值
        for (int i = 0; i < remainingCount; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 18; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            uniqueIdcards.add(sb.toString());
        }
        
        // 添加前20个值各出现 frequencyPerValue 次
        for (int i = 0; i < highFreqCount; i++) {
            String value = uniqueIdcards.get(i);
            for (int f = 0; f < frequencyPerValue; f++) {
                idcards.add(value);
            }
        }
        
        // 添加剩余的值（每个出现1次）
        for (int i = highFreqCount; i < uniqueIdcards.size(); i++) {
            idcards.add(uniqueIdcards.get(i));
        }
        
        return idcards;
    }

    /**
     * 生成随机中文地址（前20个值的平均出现次数在5-10之间）
     */
    private List<String> generateChineseAddresses(int count, int avgFrequencyTop20) {
        List<String> addresses = new ArrayList<>();
        String[] provinces = {"北京", "上海", "广东", "浙江", "江苏"};
        String[] cities = {"市中心", "郊区", "开发区", "新城", "老城"};
        String[] streets = {"一街", "二街", "三街", "四街", "五街"};
        
        // 前20个高频值的个数
        int highFreqCount = 20;
        // 每个高频值出现的次数
        int frequencyPerValue = avgFrequencyTop20;
        // 前20个值总共占用的数据个数
        int top20Count = highFreqCount * frequencyPerValue;
        // 剩余数据个数
        int remainingCount = count - top20Count;
        
        // 生成所有唯一值
        List<String> uniqueAddresses = new ArrayList<>();
        // 前20个高频值
        for (int i = 0; i < highFreqCount; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(provinces[RANDOM.nextInt(provinces.length)]);
            sb.append(cities[RANDOM.nextInt(cities.length)]);
            sb.append(streets[RANDOM.nextInt(streets.length)]);
            int houseNum = 100 + RANDOM.nextInt(9900);
            sb.append(houseNum);
            if (RANDOM.nextBoolean()) {
                sb.append("号楼");
                if (RANDOM.nextBoolean()) {
                    sb.append(RANDOM.nextInt(50)).append("单元");
                }
                if (RANDOM.nextBoolean()) {
                    sb.append(RANDOM.nextInt(99)).append("号");
                }
            }
            uniqueAddresses.add(sb.toString());
        }
        // 剩余的唯一值
        for (int i = 0; i < remainingCount; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(provinces[RANDOM.nextInt(provinces.length)]);
            sb.append(cities[RANDOM.nextInt(cities.length)]);
            sb.append(streets[RANDOM.nextInt(streets.length)]);
            int houseNum = 100 + RANDOM.nextInt(9900);
            sb.append(houseNum);
            if (RANDOM.nextBoolean()) {
                sb.append("号楼");
                if (RANDOM.nextBoolean()) {
                    sb.append(RANDOM.nextInt(50)).append("单元");
                }
                if (RANDOM.nextBoolean()) {
                    sb.append(RANDOM.nextInt(99)).append("号");
                }
            }
            uniqueAddresses.add(sb.toString());
        }
        
        // 添加前20个值各出现 frequencyPerValue 次
        for (int i = 0; i < highFreqCount; i++) {
            String value = uniqueAddresses.get(i);
            for (int f = 0; f < frequencyPerValue; f++) {
                addresses.add(value);
            }
        }
        
        // 添加剩余的值（每个出现1次）
        for (int i = highFreqCount; i < uniqueAddresses.size(); i++) {
            addresses.add(uniqueAddresses.get(i));
        }
        
        return addresses;
    }

    /**
     * 生成随机中文姓名（前20个值的平均出现次数在5-10之间）
     */
    private List<String> generateChineseNames(int count, int avgFrequencyTop20) {
        List<String> names = new ArrayList<>();
        String[] surnames = {"张", "王", "李", "刘", "陈", "杨", "赵", "黄", "周", "吴"};
        String[] givenNames = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        
        // 前20个高频值的个数
        int highFreqCount = 20;
        // 每个高频值出现的次数
        int frequencyPerValue = avgFrequencyTop20;
        // 前20个值总共占用的数据个数
        int top20Count = highFreqCount * frequencyPerValue;
        // 剩余数据个数
        int remainingCount = count - top20Count;
        
        // 生成所有唯一值
        List<String> uniqueNames = new ArrayList<>();
        // 前20个高频值
        for (int i = 0; i < highFreqCount; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(surnames[RANDOM.nextInt(surnames.length)]);
            sb.append(givenNames[RANDOM.nextInt(givenNames.length)]);
            if (RANDOM.nextBoolean()) {
                sb.append(givenNames[RANDOM.nextInt(givenNames.length)]);
            }
            uniqueNames.add(sb.toString());
        }
        // 剩余的唯一值
        for (int i = 0; i < remainingCount; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(surnames[RANDOM.nextInt(surnames.length)]);
            sb.append(givenNames[RANDOM.nextInt(givenNames.length)]);
            if (RANDOM.nextBoolean()) {
                sb.append(givenNames[RANDOM.nextInt(givenNames.length)]);
            }
            uniqueNames.add(sb.toString());
        }
        
        // 添加前20个值各出现 frequencyPerValue 次
        for (int i = 0; i < highFreqCount; i++) {
            String value = uniqueNames.get(i);
            for (int f = 0; f < frequencyPerValue; f++) {
                names.add(value);
            }
        }
        
        // 添加剩余的值（每个出现1次）
        for (int i = highFreqCount; i < uniqueNames.size(); i++) {
            names.add(uniqueNames.get(i));
        }
        
        return names;
    }

    /**
     * 生成随机手机号（前20个值的平均出现次数在5-10之间）
     */
    private List<String> generatePhoneNumbers(int count, int avgFrequencyTop20) {
        List<String> phones = new ArrayList<>();
        String[] prefixes = {"134", "135", "136", "137", "138", "139", "150", "151", "152", "153"};
        
        // 前20个高频值的个数
        int highFreqCount = 20;
        // 每个高频值出现的次数
        int frequencyPerValue = avgFrequencyTop20;
        // 前20个值总共占用的数据个数
        int top20Count = highFreqCount * frequencyPerValue;
        // 剩余数据个数
        int remainingCount = count - top20Count;
        
        // 生成所有唯一值
        List<String> uniquePhones = new ArrayList<>();
        // 前20个高频值
        for (int i = 0; i < highFreqCount; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefixes[RANDOM.nextInt(prefixes.length)]);
            for (int j = 0; j < 8; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            uniquePhones.add(sb.toString());
        }
        // 剩余的唯一值
        for (int i = 0; i < remainingCount; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefixes[RANDOM.nextInt(prefixes.length)]);
            for (int j = 0; j < 8; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            uniquePhones.add(sb.toString());
        }
        
        // 添加前20个值各出现 frequencyPerValue 次
        for (int i = 0; i < highFreqCount; i++) {
            String value = uniquePhones.get(i);
            for (int f = 0; f < frequencyPerValue; f++) {
                phones.add(value);
            }
        }
        
        // 添加剩余的值（每个出现1次）
        for (int i = highFreqCount; i < uniquePhones.size(); i++) {
            phones.add(uniquePhones.get(i));
        }
        
        return phones;
    }

    /**
     * 字符串转16进制字符串
     */
    private String stringToHex(String str) {
        return bytesToHex(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 字节数组转16进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 字符串转Base64
     */
    private String stringToBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 字节XOR操作
     */
    private byte[] xorBytes(byte[] bytes, byte key) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] ^ key);
        }
        return result;
    }

    /**
     * AES加密（字符串）
     */
    private byte[] encryptAES(String data) throws Exception {
        return encryptAESBytes(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * AES加密（字节数组）
     */
    private byte[] encryptAESBytes(byte[] data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }
}
