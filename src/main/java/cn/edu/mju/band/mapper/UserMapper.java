package cn.edu.mju.band.mapper;

import cn.edu.mju.band.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户表 Mapper（取代原 IUserDal / UserDal）。
 *
 * <p>注意：这个接口现在一个方法都没有，但已经具备完整的单表 CRUD 能力 ——
 * 全部来自 {@link BaseMapper}。登录查询由 {@code selectOne + LambdaQueryWrapper}
 * 完成，见 UserBuss。</p>
 *
 * <p>对照组：TaskMapper 因为要演示手写 LIMIT 分页，还留着 search / count 两个自定义方法；
 * 而 UserMapper 只做一次等值查询，用 Wrapper 就够了，所以彻底变成空接口。
 * 这两种写法在一套代码里同时存在，就是为了说明“什么时候该写 SQL，什么时候不该写”。</p>
 */
public interface UserMapper extends BaseMapper<User> {
}
