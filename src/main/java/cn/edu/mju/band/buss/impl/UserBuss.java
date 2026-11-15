package cn.edu.mju.band.buss.impl;

import cn.edu.mju.band.buss.IUserBuss;
import cn.edu.mju.band.entity.User;
import cn.edu.mju.band.mapper.UserMapper;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现。
 * 依赖通过 Spring 注入（setter 注入），不自行 new 实现类 —— 与 TaskBuss 保持一致的 IoC 解耦风格。
 * 原来由 applicationContext.xml 的 &lt;bean id="userBuss"&gt; 注册，现在用 @Service 交给组件扫描。
 *
 * <p><b>改造说明（实验环节四 / 五）</b>：补上空白入参短路与「数据库宕机」异常降级，
 * 使登录校验在持久层完全不可用时仍能给出确定的 false，而不是把异常抛到登录页。</p>
 */
@Service
public class UserBuss implements IUserBuss {

    private static final Logger log = LoggerFactory.getLogger(UserBuss.class);

    // 由 @MapperScan 生成的 UserMapper 代理注入（等价于原来的 <property name="userMapper" ref="userMapper"/>）
    private UserMapper userMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean login(String username, String password) {
        // 1) 入参校验先于任何外部依赖：空白账号/密码直接短路，连库都不碰
        if (username == null || password == null
                || username.isBlank() || password.isBlank()) {
            return false;
        }

        // 原来是 userMapper.findByLoginName(username)，SQL 写在 UserMapper.xml 里；
        // 现在由 BaseMapper.selectOne + LambdaQueryWrapper 生成等价 SQL：
        //   SELECT id, login_name, login_pwd, nickname, status FROM t_user WHERE login_name = ?
        User user;
        try {
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getLoginName, username));
        } catch (RuntimeException e) {
            // 2) 数据库宕机 / 断网：按「登录失败」处理，不把异常暴露给页面
            log.error("登录查询失败，按登录失败处理: username={}", username, e);
            return false;
        }

        // 3) 用户必须存在
        if (user == null) {
            return false;
        }
        // 4) 账号状态必须为 1（正常/启用），对应视频中的 status==1 判断
        if (user.getStatus() == null || user.getStatus() != 1) {
            return false;
        }
        // 5) 对明文密码做 MD5 摘要（与入库时已摘要的密码比对），避免明文存储
        //    兼容两种入库格式：16 位摘要（课件写法）与 32 位摘要（标准 MD5）
        String hashed16 = MD5.create().digestHex16(password);
        String hashed32 = MD5.create().digestHex(password);
        String stored = user.getLoginPwd();
        return stored != null
                && (hashed16.equalsIgnoreCase(stored) || hashed32.equalsIgnoreCase(stored));
    }
}
