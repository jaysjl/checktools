-- Oracle 数据库初始化脚本

-- 使用 SQL*Plus 执行此脚本
-- sqlplus / as sysdba @04_oracle_init.sql

CREATE USER dbscan_test_schema IDENTIFIED BY "dbscan123";

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO dbscan_test_schema;
-- 使用 dbscan_test_schema 用户
-- CONN dbscan_test_schema/dbscan123@orcl

-- 创建用户表（包含敏感数据：身份证、手机号）
CREATE TABLE dbscan_test_schema.users (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    id_card VARCHAR2(18),
    phone VARCHAR2(20),
    email VARCHAR2(100),
    address VARCHAR2(255),
    created_at TIMESTAMP DEFAULT SYSDATE
);

-- 创建序列用于自动增长
CREATE SEQUENCE users_seq
  START WITH 1
  INCREMENT BY 1
  NOCACHE;

-- 创建订单表（包含敏感数据：手机号）
CREATE TABLE dbscan_test_schema.orders (
    order_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL REFERENCES dbscan_test_schema.users(id),
    order_no VARCHAR2(50) NOT NULL UNIQUE,
    phone VARCHAR2(20),
    address VARCHAR2(255),
    amount NUMBER(10, 2),
    status VARCHAR2(20),
    created_at TIMESTAMP DEFAULT SYSDATE
);

-- 创建序列用于自动增长
CREATE SEQUENCE orders_seq
  START WITH 1
  INCREMENT BY 1
  NOCACHE;

-- 创建账户表（包含敏感数据：身份证、银行卡号）
CREATE TABLE dbscan_test_schema.accounts (
    account_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL REFERENCES dbscan_test_schema.users(id),
    id_card VARCHAR2(18),
    bank_card VARCHAR2(25),
    bank_name VARCHAR2(50),
    account_holder VARCHAR2(100),
    created_at TIMESTAMP DEFAULT SYSDATE
);

-- 创建序列用于自动增长
CREATE SEQUENCE accounts_seq
  START WITH 1
  INCREMENT BY 1
  NOCACHE;

-- 创建日志表（包含敏感数据：手机号、邮箱）
CREATE TABLE dbscan_test_schema.logs (
    log_id NUMBER PRIMARY KEY,
    user_id NUMBER,
    phone VARCHAR2(20),
    email VARCHAR2(100),
    action VARCHAR2(50),
    detail VARCHAR2(255),
    created_at TIMESTAMP DEFAULT SYSDATE
);

-- 创建序列用于自动增长
CREATE SEQUENCE logs_seq
  START WITH 1
  INCREMENT BY 1
  NOCACHE;

-- 创建索引
CREATE INDEX idx_users_phone ON dbscan_test_schema.users(phone);
CREATE INDEX idx_users_id_card ON dbscan_test_schema.users(id_card);
CREATE INDEX idx_orders_user_id ON dbscan_test_schema.orders(user_id);
CREATE INDEX idx_orders_phone ON dbscan_test_schema.orders(phone);
CREATE INDEX idx_accounts_user_id ON dbscan_test_schema.accounts(user_id);
CREATE INDEX idx_logs_user_id ON dbscan_test_schema.logs(user_id);

-- ===================== 数据插入 =====================

-- 插入用户表数据
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '张三', '110101199001011234', '13800138000', 'zhangsan@example.com', '北京市朝阳区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '李四', '110101199101012345', '13900139001', 'lisi@example.com', '北京市朝阳区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '王五', '110101199201013456', '13912345678', 'wangwu@example.com', '北京市海淀区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '赵六', '110101199301014567', '15812345678', 'zhaoliu@example.com', '北京市海淀区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '孙七', '110101199401015678', '18612345678', 'sunqi@example.com', '北京市东城区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '周八', '110101199501016789', '17712345678', 'zhouba@example.com', '北京市东城区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '吴九', '110101199601017890', '19912345678', 'wujiu@example.com', '北京市西城区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '郑十', '110101199701018901', '13500138000', 'zhengshi@example.com', '北京市西城区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '刘十一', '110101199801019012', '13600138000', 'liushiyi@example.com', '北京市丰台区');
INSERT INTO dbscan_test_schema.users (id, name, id_card, phone, email, address) VALUES (users_seq.NEXTVAL, '陈十二', '110101199901010123', '13700138000', 'chenshier@example.com', '北京市丰台区');

COMMIT;

-- 插入订单表数据
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 1, 'ORD001', '13800138000', '北京市朝阳区', 199.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 1, 'ORD002', '13800138000', '北京市朝阳区', 299.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 2, 'ORD003', '13900139001', '北京市朝阳区', 399.99, 'pending');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 2, 'ORD004', '13900139001', '北京市朝阳区', 499.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 3, 'ORD005', '13912345678', '北京市海淀区', 599.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 3, 'ORD006', '13912345678', '北京市海淀区', 699.99, 'canceled');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 4, 'ORD007', '15812345678', '北京市海淀区', 799.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 5, 'ORD008', '18612345678', '北京市东城区', 899.99, 'pending');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 6, 'ORD009', '17712345678', '北京市东城区', 999.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 7, 'ORD010', '19912345678', '北京市西城区', 1099.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 8, 'ORD011', '13500138000', '北京市西城区', 1199.99, 'pending');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 9, 'ORD012', '13600138000', '北京市丰台区', 1299.99, 'completed');
INSERT INTO dbscan_test_schema.orders (order_id, user_id, order_no, phone, address, amount, status) VALUES (orders_seq.NEXTVAL, 10, 'ORD013', '13700138000', '北京市丰台区', 1399.99, 'completed');

COMMIT;

-- 插入账户表数据
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 1, '110101199001011234', '6222024000001234567', '工商银行', '张三');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 2, '110101199101012345', '6226091234567890123', '建设银行', '李四');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 3, '110101199201013456', '6216210123456789012', '农业银行', '王五');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 4, '110101199301014567', '6216201234567890123', '中国银行', '赵六');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 5, '110101199401015678', '6214101234567890123', '招商银行', '孙七');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 6, '110101199501016789', '6216581234567890123', '交通银行', '周八');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 7, '110101199601017890', '6211941234567890123', '兴业银行', '吴九');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 8, '110101199701018901', '6214161234567890123', '浦发银行', '郑十');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 9, '110101199801019012', '6212261234567890123', '民生银行', '刘十一');
INSERT INTO dbscan_test_schema.accounts (account_id, user_id, id_card, bank_card, bank_name, account_holder) VALUES (accounts_seq.NEXTVAL, 10, '110101199901010123', '6214981234567890123', '光大银行', '陈十二');

COMMIT;

-- 插入日志表数据
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 1, '13800138000', 'zhangsan@example.com', 'login', '用户登录');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 2, '13900139001', 'lisi@example.com', 'login', '用户登录');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 3, '13912345678', 'wangwu@example.com', 'purchase', '用户购买');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 4, '15812345678', 'zhaoliu@example.com', 'login', '用户登录');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 5, '18612345678', 'sunqi@example.com', 'purchase', '用户购买');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 1, '13800138000', 'zhangsan@example.com', 'logout', '用户登出');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 2, '13900139001', 'lisi@example.com', 'update_profile', '用户更新资料');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 6, '17712345678', 'zhouba@example.com', 'login', '用户登录');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 7, '19912345678', 'wujiu@example.com', 'purchase', '用户购买');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 8, '13500138000', 'zhengshi@example.com', 'login', '用户登录');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 3, '13912345678', 'wangwu@example.com', 'password_change', '用户修改密码');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 9, '13600138000', 'liushiyi@example.com', 'login', '用户登录');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 10, '13700138000', 'chenshier@example.com', 'purchase', '用户购买');
INSERT INTO dbscan_test_schema.logs (log_id, user_id, phone, email, action, detail) VALUES (logs_seq.NEXTVAL, 4, '15812345678', 'zhaoliu@example.com', 'refund', '用户退款');

COMMIT;
