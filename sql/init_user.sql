-- 用户表（登录验证用）
-- 在 taskmaster 库中执行
CREATE TABLE IF NOT EXISTS t_user (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    login_name  VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    login_pwd   VARCHAR(50)  NOT NULL COMMENT '密码（MD5 16位摘要，非明文）',
    nickname    VARCHAR(50)  COMMENT '昵称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常，0=禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 测试账号：admin / 123456
-- 说明：Hutool 的 MD5.create().digestHex16("123456") 结果为 49ba59abbe56e057
INSERT INTO t_user (login_name, login_pwd, nickname, status)
VALUES ('admin', '49ba59abbe56e057', '管理员', 1)
ON DUPLICATE KEY UPDATE login_pwd='49ba59abbe56e057', nickname='管理员', status=1;
