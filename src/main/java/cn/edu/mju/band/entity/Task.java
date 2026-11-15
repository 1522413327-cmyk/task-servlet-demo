package cn.edu.mju.band.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 任务实体。
 *
 * <p>三组 MP 注解：@TableName 指表名、@TableId 指主键策略、@TableField 做列名映射。
 * MP 只在“名字对不上”的地方需要注解，能猜出来的不用写。</p>
 *
 * <p>Lombok 三个注解：@Data 生成 getter/setter/toString/equals/hashCode，
 * @NoArgsConstructor 生成无参构造，@AllArgsConstructor 生成全参构造。</p>
 *
 * <p><b>为什么两个构造器注解要成对出现</b>：Java 规定「一旦显式声明了任何构造器，
 * 默认无参构造就消失」。只用 @AllArgsConstructor 时，Task 就只剩全参构造，
 * 而 MyBatis 通过反射创建实体时必须调用无参构造 —— 启动/查询会直接报
 * “No constructor found” 或实例化失败。所以这两个必须成对写。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_task")
public class Task {

    /** IdType.AUTO = 交给 MySQL 的 auto_increment，insert 后 MP 会把自增 id 回填到该属性 */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** title 与列名一致，无需注解 */
    private String title;

    /** tinyint(1 字节) 用 Byte 接收，与列名一致 */
    private Byte status;

    private Byte priority;

    /**
     * due_date -> dueDate。
     * updateStrategy = ALWAYS：即使值为 null 也拼进 SET，
     * 否则 updateById 会跳过 null，导致「把截止日期清空」保存不进去。
     */
    @TableField(value = "due_date", updateStrategy = FieldStrategy.ALWAYS)
    private Date dueDate;

    /** create_date 由数据库 DEFAULT CURRENT_TIMESTAMP 生成，业务代码不该写它 */
    @TableField(value = "create_date",
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER)
    private Date createDate;
}
