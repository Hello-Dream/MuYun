package net.ximatai.muyun.test.plaform;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.ximatai.muyun.core.config.MuYunConfig;
import net.ximatai.muyun.platform.PlatformConst;
import net.ximatai.muyun.test.testcontainers.PostgresTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(value = PostgresTestResource.class)
public class TestUserPasswordComplexity {
    @Inject
    MuYunConfig config;

    String base = PlatformConst.BASE_PATH;

    private String id = "";

    @BeforeEach
    void setUp() {
        if (id.isEmpty()) {
            //新增用户
            id = given()
                .header("userID", config.superUserId())
                .contentType("application/json")
                .body(Map.of(
                    "v_name", "测试",
                    "dict_user_gender", "0"
                ))
                .when()
                .post("/api%s/userinfo/create".formatted(base))
                .then()
                .statusCode(200)
                .extract()
                .asString();
            // 设置用户
            given()
                .header("userID", config.superUserId())
                .contentType("application/json")
                .body(Map.of(
                    "v_username", "test",
                    "v_password", "pw123456",
                    "v_password2", "pw123456",
                    "roles", List.of("1", "2")
                ))
                .when()
                .post("/api%s/userinfo/setUser/%s".formatted(base, id))
                .then()
                .statusCode(200)
                .extract()
                .asString();
        }
    }

    @Test
    @DisplayName("验证密码长度少于8位 但 包含英文字母")
    void testPasswordComplexity1() {
        //情况1：密码长度少于8位
        // 修改密码
        String s1 = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345z",
                "v_password2", "12345z"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();
        assertEquals("密码长度应不少于8位", s1);
        // 修改密码（自助）
        String s2 = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_old_password", "pw123456",
                "v_password", "12345z",
                "v_password2", "12345z"
            ))
            .when()
            .post("/api%s/userinfo/setPasswordSelf/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();
        assertEquals("密码长度应不少于8位", s2);
    }

    @Test
    @DisplayName("验证密码不包含英文字母 但 长度达到8位")
    void testPasswordComplexity2() {
        //情况2：密码不包含英文字母
        // 修改密码
        String s1 = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345678",
                "v_password2", "12345678"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();
        assertEquals("密码应至少包含一个英文字母", s1);
        // 修改密码（自助）
        String s2 = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_old_password", "pw123456",
                "v_password", "12345678",
                "v_password2", "12345678"
            ))
            .when()
            .post("/api%s/userinfo/setPasswordSelf/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();
        assertEquals("密码应至少包含一个英文字母", s2);
    }

    @Test
    @DisplayName("验证密码长度少于8位 且 不包含英文字母")
    void testPasswordComplexity3() {
        //情况3：密码少于8位 且 不包含英文字母
        // 修改密码
        String s1 = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "123",
                "v_password2", "123"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();
        assertEquals("密码长度应不少于8位，密码应至少包含一个英文字母", s1);
        // 修改密码（自助）
        String s2 = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_old_password", "pw123456",
                "v_password", "123",
                "v_password2", "123"
            ))
            .when()
            .post("/api%s/userinfo/setPasswordSelf/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();
        assertEquals("密码长度应不少于8位，密码应至少包含一个英文字母", s2);
    }

    @Test
    @DisplayName("验证密码长度达到8位 且 密码包含英文字母")
    void testPasswordComplexity4() {
        //情况4：修改密码成功
        // 修改密码
        given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345678z",
                "v_password2", "12345678z"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(200)
            .extract()
            .asString();
        // 修改密码（自助）
        given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_old_password", "12345678z",
                "v_password", "z12345678",
                "v_password2", "z12345678"
            ))
            .when()
            .post("/api%s/userinfo/setPasswordSelf/%s".formatted(base, id))
            .then()
            .statusCode(200)
            .extract()
            .asString();
    }

    @Test
    @DisplayName("修改重复的密码")
    void testSetPasswordDuplicate() {
        given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345678abc",
                "v_password2", "12345678abc"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(200);

        given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345678abc2",
                "v_password2", "12345678abc2"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(200);

        given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345678abc3",
                "v_password2", "12345678abc3"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(200);

        String string = given()
            .header("userID", config.superUserId())
            .contentType("application/json")
            .body(Map.of(
                "v_password", "12345678abc",
                "v_password2", "12345678abc"
            ))
            .when()
            .post("/api%s/userinfo/setPassword/%s".formatted(base, id))
            .then()
            .statusCode(500)
            .extract()
            .asString();

        assertEquals("新设置的密码曾经使用过，不允许再次使用", string);

    }

    @Test
    @DisplayName("检查密码有效期")
    void testPasswordValid() {
        String day = given()
            .header("userID", config.superUserId())
            .get("/api%s/userinfo/passwordValidDays/%s".formatted(base, id))
            .then()
            .statusCode(200)
            .extract()
            .asString();

        assertEquals("90", day);
    }
}
