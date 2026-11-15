package cn.edu.mju.band;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动入口类（替代原来的 web.xml + Tomcat 部署方式）。
 *
 * <p>@SpringBootApplication 是一个组合注解，等价于：
 * <ul>
 *   <li>@SpringBootConfiguration —— 声明这是一个配置类；</li>
 *   <li>@EnableAutoConfiguration —— 按 classpath 上的依赖自动装配（内嵌 Tomcat、DataSource、MyBatis、Thymeleaf 等）；</li>
 *   <li>@ComponentScan —— 扫描本类所在包 cn.edu.mju.band 及其子包下的 @Controller/@Service 等组件。</li>
 * </ul>
 *
 * <p>@MapperScan 告诉 MyBatis 去哪个包生成 Mapper 代理对象，
 * 替代原 applicationContext.xml 里的 MapperScannerConfigurer。
 */
@SpringBootApplication
@MapperScan("cn.edu.mju.band.mapper")
public class TaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }
}
