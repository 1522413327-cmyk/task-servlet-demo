package cn.edu.mju.band.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 环节二：多环境配置验证。
 * 不启动 Spring 容器、不连数据库，直接解析 classpath 下的 yml，
 * 证明 dev / test / prod 三套配置的端口、数据源、日志等级与业务参数确实互相隔离。
 */
@DisplayName("application.yml 多环境配置验证")
class ProfileConfigTest {

    /** 用 Spring Boot 自己的 YamlPropertySourceLoader 读，和真实启动时解析规则一致 */
    private Map<String, Object> load(String file) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(file, new ClassPathResource(file));
        Map<String, Object> props = new LinkedHashMap<>();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    props.put(name, source.getProperty(name));
                }
            }
        }
        return props;
    }

    @Test
    @DisplayName("主配置默认激活 dev，且只放公共项")
    void mainConfigShouldActivateDevByDefault() throws IOException {
        Map<String, Object> main = load("application.yml");

        assertThat(main.get("spring.profiles.active")).isEqualTo("dev");
        assertThat(main.get("mybatis-plus.mapper-locations"))
                .isEqualTo("classpath:mapper/*.xml");
        // 数据源细节不应再写死在主配置里
        assertThat(main.containsKey("spring.datasource.url")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "dev,  8088, 5,  100",
            "test, 8089, 3,  20",
            "prod, 8080, 10, 50"
    })
    @DisplayName("三套 profile 的端口与业务参数各不相同，且都定义了独立数据源")
    void eachProfileShouldIsolatePortAndBusinessParams(
            String profile, int port, int defaultSize, int maxSize) throws IOException {
        Map<String, Object> props = load("application-" + profile + ".yml");

        assertThat(String.valueOf(props.get("server.port"))).isEqualTo(String.valueOf(port));
        assertThat(String.valueOf(props.get("app.task.default-page-size")))
                .isEqualTo(String.valueOf(defaultSize));
        assertThat(String.valueOf(props.get("app.task.max-page-size")))
                .isEqualTo(String.valueOf(maxSize));
        assertThat(String.valueOf(props.get("spring.datasource.url"))).isNotBlank();
        assertThat(String.valueOf(props.get("logging.level.root"))).isNotBlank();
    }

    @Test
    @DisplayName("prod 的密码只接受外部注入，不带任何默认值；并开启模板缓存")
    void prodShouldNotCarryPlaintextCredentials() throws IOException {
        Map<String, Object> prod = load("application-prod.yml");

        // 形如 ${DB_PASSWORD:xxx} 的默认值写法不应出现在生产配置中
        assertThat(String.valueOf(prod.get("spring.datasource.password")))
                .startsWith("${DB_")
                .doesNotContain(":");
        assertThat(String.valueOf(prod.get("spring.thymeleaf.cache"))).isEqualTo("true");
    }

    @Test
    @DisplayName("test 环境不预建连接，保证命令行切换时启动不依赖数据库是否就绪")
    void testProfileShouldNotEagerlyConnect() throws IOException {
        Map<String, Object> test = load("application-test.yml");

        assertThat(String.valueOf(test.get("spring.datasource.druid.initial-size")))
                .isEqualTo("0");
        assertThat(String.valueOf(test.get("spring.datasource.url")))
                .contains("taskmaster_test");
    }
}
