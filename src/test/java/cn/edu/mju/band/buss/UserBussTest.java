package cn.edu.mju.band.buss;

import cn.edu.mju.band.buss.impl.UserBuss;
import cn.edu.mju.band.entity.User;
import cn.edu.mju.band.mapper.UserMapper;
import cn.edu.mju.band.support.MybatisPlusTestSupport;
import cn.hutool.crypto.digest.MD5;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 环节三 / 四：UserBuss 登录逻辑的单元测试。
 * UserMapper 用 @Mock 打桩，密码摘要用 Hutool 现算，不查任何真实账号。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserBuss 登录逻辑单元测试（持久层完全隔离）")
class UserBussTest {

    private static final String RAW_PASSWORD = "123456";

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserBuss userBuss;

    private User enabledUser;

    @BeforeAll
    static void initMetadata() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        enabledUser = new User();
        enabledUser.setId(1);
        enabledUser.setLoginName("admin");
        enabledUser.setNickname("管理员");
        enabledUser.setStatus((byte) 1);
        enabledUser.setLoginPwd(MD5.create().digestHex16(RAW_PASSWORD));
    }

    // ==================== 正常流 ====================

    @Test
    @DisplayName("密码正确（16 位 MD5 摘要）登录成功")
    void loginShouldAccept16DigitMd5() {
        doReturn(enabledUser).when(userMapper).selectOne(any());

        assertThat(userBuss.login("admin", RAW_PASSWORD)).isTrue();
        verify(userMapper, times(1)).selectOne(any());
    }

    @Test
    @DisplayName("密码正确（32 位标准 MD5 摘要）同样登录成功")
    void loginShouldAccept32DigitMd5() {
        enabledUser.setLoginPwd(MD5.create().digestHex(RAW_PASSWORD));
        doReturn(enabledUser).when(userMapper).selectOne(any());

        assertThat(userBuss.login("admin", RAW_PASSWORD)).isTrue();
    }

    // ==================== 边界值 / 异常流 ====================

    static Stream<Arguments> blankCredentials() {
        return Stream.of(
                Arguments.of(null, RAW_PASSWORD),
                Arguments.of("admin", null),
                Arguments.of("", RAW_PASSWORD),
                Arguments.of("   ", RAW_PASSWORD),
                Arguments.of("admin", ""),
                Arguments.of("admin", "   ")
        );
    }

    @ParameterizedTest
    @MethodSource("blankCredentials")
    @DisplayName("账号或密码为 null / 空白：直接短路返回 false，且不查库")
    void loginShouldRejectBlankCredentials(String username, String password) {
        assertThat(userBuss.login(username, password)).isFalse();
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("用户不存在返回 false")
    void loginShouldRejectUnknownUser() {
        doReturn(null).when(userMapper).selectOne(any());
        assertThat(userBuss.login("nobody", RAW_PASSWORD)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(bytes = {0, 2, 3})
    @DisplayName("账号被禁用 / 状态异常返回 false")
    void loginShouldRejectDisabledUser(byte status) {
        enabledUser.setStatus(status);
        doReturn(enabledUser).when(userMapper).selectOne(any());
        assertThat(userBuss.login("admin", RAW_PASSWORD)).isFalse();
    }

    @Test
    @DisplayName("status 为 null 也按未启用处理，不拆箱 NPE")
    void loginShouldRejectNullStatus() {
        enabledUser.setStatus(null);
        doReturn(enabledUser).when(userMapper).selectOne(any());
        assertThat(userBuss.login("admin", RAW_PASSWORD)).isFalse();
    }

    @Test
    @DisplayName("密码错误返回 false")
    void loginShouldRejectWrongPassword() {
        doReturn(enabledUser).when(userMapper).selectOne(any());
        assertThat(userBuss.login("admin", "wrong-password")).isFalse();
    }

    @Test
    @DisplayName("库里没有存密码摘要时返回 false")
    void loginShouldRejectNullStoredPassword() {
        enabledUser.setLoginPwd(null);
        doReturn(enabledUser).when(userMapper).selectOne(any());
        assertThat(userBuss.login("admin", RAW_PASSWORD)).isFalse();
    }

    @Test
    @DisplayName("环节四：数据库宕机 / 断网时登录失败但不抛异常，页面不 500")
    void loginShouldReturnFalseWhenDatabaseDown() {
        doThrow(new RuntimeException("Connection refused"))
                .when(userMapper).selectOne(any());

        assertThat(userBuss.login("admin", RAW_PASSWORD)).isFalse();
        verify(userMapper, times(1)).selectOne(any());
    }
}
