-- PostgreSQL 数据库初始化脚本
-- 创建测试数据库

CREATE SCHEMA IF NOT EXISTS dbscan_test_schema;

-- 创建用户表（包含敏感数据：身份证、手机号）
CREATE TABLE IF NOT EXISTS dbscan_test_schema.users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    id_card VARCHAR(18),
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE dbscan_test_schema.users IS '用户表';
COMMENT ON COLUMN dbscan_test_schema.users.id_card IS '身份证号';
COMMENT ON COLUMN dbscan_test_schema.users.phone IS '手机号';

-- 创建订单表（包含敏感数据：手机号）
CREATE TABLE IF NOT EXISTS dbscan_test_schema.orders (
    order_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES dbscan_test_schema.users(id),
    order_no VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(255),
    amount DECIMAL(10, 2),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE dbscan_test_schema.orders IS '订单表';
COMMENT ON COLUMN dbscan_test_schema.orders.phone IS '收货手机号';

-- 创建账户表（包含敏感数据：身份证、银行卡号）
CREATE TABLE IF NOT EXISTS dbscan_test_schema.accounts (
    account_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES dbscan_test_schema.users(id),
    id_card VARCHAR(18),
    bank_card VARCHAR(25),
    bank_name VARCHAR(50),
    account_holder VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE dbscan_test_schema.accounts IS '账户表';
COMMENT ON COLUMN dbscan_test_schema.accounts.id_card IS '身份证号';
COMMENT ON COLUMN dbscan_test_schema.accounts.bank_card IS '银行卡号';

-- 创建日志表（包含敏感数据：手机号、邮箱）
CREATE TABLE IF NOT EXISTS dbscan_test_schema.logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    phone VARCHAR(20),
    email VARCHAR(100),
    action VARCHAR(50),
    detail VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE logs IS '日志表';
COMMENT ON COLUMN logs.phone IS '用户手机号';
COMMENT ON COLUMN logs.email IS '用户邮箱';

-- 创建索引
CREATE INDEX idx_users_phone ON dbscan_test_schema.users(phone);
CREATE INDEX idx_users_id_card ON dbscan_test_schema.users(id_card);
CREATE INDEX idx_orders_user_id ON dbscan_test_schema.orders(user_id);
CREATE INDEX idx_orders_phone ON dbscan_test_schema.orders(phone);
CREATE INDEX idx_accounts_user_id ON dbscan_test_schema.accounts(user_id);
CREATE INDEX idx_logs_user_id ON dbscan_test_schema.logs(user_id);

-- ===================== 数据插入 =====================

-- 插入用户表数据
INSERT INTO dbscan_test_schema.users (name, id_card, phone, email, address) VALUES
('张三', '110101199001011234', '13800138000', 'zhangsan@example.com', '北京市朝阳区'),
('李四', '110101199101012345', '13900139001', 'lisi@example.com', '北京市朝阳区'),
('王五', '110101199201013456', '13912345678', 'wangwu@example.com', '北京市海淀区'),
('赵六', '110101199301014567', '15812345678', 'zhaoliu@example.com', '北京市海淀区'),
('孙七', '110101199401015678', '18678945678', 'sunqi@example.com', '北京市东城区'),
('周八', '110101199501016789', '17712345678', 'zhouba@example.com', '北京市东城区'),
('吴九', '110101199601017890', '19912345678', 'wujiu@example.com', '北京市西城区'),
('郑十', '110101199701018901', '13500138000', 'zhengshi@example.com', '北京市西城区'),
('刘十一', '110101199801019012', '13600138000', 'liushiyi@example.com', '北京市丰台区'),
('陈十二', '110101199901010123', '13700138000', 'chenshier@example.com', '北京市丰台区');

-- 插入订单表数据
INSERT INTO dbscan_test_schema.orders (user_id, order_no, phone, address, amount, status) VALUES
(1, 'ORD001', '13800138000', '北京市朝阳区', 199.99, 'completed'),
(1, 'ORD002', '13800138000', '北京市朝阳区', 299.99, 'completed'),
(2, 'ORD003', '13900139001', '北京市朝阳区', 399.99, 'pending'),
(2, 'ORD004', '13900139001', '北京市朝阳区', 499.99, 'completed'),
(3, 'ORD005', '13912345678', '北京市海淀区', 599.99, 'completed'),
(3, 'ORD006', '13912345678', '北京市海淀区', 699.99, 'canceled'),
(4, 'ORD007', '15812345678', '北京市海淀区', 799.99, 'completed'),
(5, 'ORD008', '18612345678', '北京市东城区', 899.99, 'pending'),
(6, 'ORD009', '17712345678', '北京市东城区', 999.99, 'completed'),
(7, 'ORD010', '19912345678', '北京市西城区', 1099.99, 'completed'),
(8, 'ORD011', '13500138000', '北京市西城区', 1199.99, 'pending'),
(9, 'ORD012', '13600138000', '北京市丰台区', 1299.99, 'completed'),
(10, 'ORD013', '13700138000', '北京市丰台区', 1399.99, 'completed');

-- 插入账户表数据
INSERT INTO dbscan_test_schema.accounts (user_id, id_card, bank_card, bank_name, account_holder) VALUES
(1, '110101199001011234', '6222024000001234567', '工商银行', '张三'),
(2, '110101199101012345', '6226091234567890123', '建设银行', '李四'),
(3, '110101199201013456', '6216210123456789012', '农业银行', '王五'),
(4, '110101199301014567', '6216201234567890123', '中国银行', '赵六'),
(5, '110101199401015678', '6214101234567890123', '招商银行', '孙七'),
(6, '110101199501016789', '6216581234567890123', '交通银行', '周八'),
(7, '110101199601017890', '6211941234567890123', '兴业银行', '吴九'),
(8, '110101199701018901', '6214161234567890123', '浦发银行', '郑十'),
(9, '110101199801019012', '6212261234567890123', '民生银行', '刘十一'),
(10, '110101199901010123', '6214981234567890123', '光大银行', '陈十二');

-- 插入日志表数据
INSERT INTO dbscan_test_schema.logs (user_id, phone, email, action, detail) VALUES
(1, '13800138000', 'zhangsan@example.com', 'login', '用户登录'),
(2, '13900139001', 'lisi@example.com', 'login', '用户登录'),
(3, '13912345678', 'wangwu@example.com', 'purchase', '用户购买'),
(4, '15812345678', 'zhaoliu@example.com', 'login', '用户登录'),
(5, '18612345678', 'sunqi@example.com', 'purchase', '用户购买'),
(1, '13800138000', 'zhangsan@example.com', 'logout', '用户登出'),
(2, '13900139001', 'lisi@example.com', 'update_profile', '用户更新资料'),
(6, '17712345678', 'zhouba@example.com', 'login', '用户登录'),
(7, '19912345678', 'wujiu@example.com', 'purchase', '用户购买'),
(8, '13500138000', 'zhengshi@example.com', 'login', '用户登录'),
(3, '13912345678', 'wangwu@example.com', 'password_change', '用户修改密码'),
(9, '13600138000', 'liushiyi@example.com', 'login', '用户登录'),
(10, '13700138000', 'chenshier@example.com', 'purchase', '用户购买'),
(4, '15812345678', 'zhaoliu@example.com', 'refund', '用户退款');
