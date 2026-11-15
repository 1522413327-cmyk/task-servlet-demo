package cn.edu.mju.band.buss;

/**
 * 用户业务接口（登录验证）。
 */
public interface IUserBuss {

    /**
     * 校验用户名与密码是否正确。
     * @param username 登录账号
     * @param password 明文密码
     * @return 校验通过返回 true，否则 false
     */
    boolean login(String username, String password);
}
