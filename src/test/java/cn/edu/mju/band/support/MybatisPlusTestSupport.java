package cn.edu.mju.band.support;

import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.entity.User;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 纯单元测试（JUnit + Mockito）不启动 Spring 容器，也不扫描 Mapper，
 * 但业务层的 {@code LambdaQueryWrapper} 里用了 {@code Task::getId}、
 * {@code User::getLoginName} 这类方法引用，MyBatis-Plus 需要先把
 * 「实体属性 -> 数据库列名」的 TableInfo 建立起来才能解析。
 *
 * <p>这个类手动做一次等价于 MyBatis 启动扫描的动作，
 * 让 Service 层可以在完全不连数据库的前提下被测试（实验环节四）。</p>
 */
public final class MybatisPlusTestSupport {

    private static boolean initialized;

    private MybatisPlusTestSupport() {
    }

    public static synchronized void initTableInfo() {
        if (initialized) {
            return;
        }
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test");
        TableInfoHelper.initTableInfo(assistant, Task.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        initialized = true;
    }
}
