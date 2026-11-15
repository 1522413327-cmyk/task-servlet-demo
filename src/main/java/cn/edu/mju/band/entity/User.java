package cn.edu.mju.band.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 用户实体。与 Task 同一套写法：MP 注解负责「映射到哪张表哪一列」，
 * Lombok 注解负责「生成样板代码」，两者互不干扰。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("login_name")
    private String loginName;

    /**
     * @ToString.Exclude：@Data 会生成 toString()，默认把所有字段拼进去。
     * 密码摘要不该出现在日志、异常堆栈或前端调试输出里，单独排除它。
     * 这个注解只影响 toString，getter 和数据库读写完全不受影响。
     */
    @ToString.Exclude
    @TableField("login_pwd")
    private String loginPwd;

    /** nickname 与列名一致，无需注解 */
    private String nickname;

    private Byte status;
}
