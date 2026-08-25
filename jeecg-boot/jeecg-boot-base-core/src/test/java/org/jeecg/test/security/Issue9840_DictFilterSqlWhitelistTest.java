package org.jeecg.test.security;

import org.jeecg.common.exception.JeecgSqlInjectionException;
import org.jeecg.common.util.SqlInjectionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 【issue/9840】dict filterSql 白名单：仅接受简单比较表达式。
 */
@ExtendWith(PrintTestResultExtension.class)
public class Issue9840_DictFilterSqlWhitelistTest {

    @Test
    @DisplayName("合法 filterSql del_flag=0 通过")
    void acceptsDelFlagEqualsZero() {
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql("del_flag=0"));
    }

    @Test
    @DisplayName("合法 filterSql status='1' 通过")
    void acceptsQuotedStatus() {
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql("status='1'"));
    }

    @Test
    @DisplayName("合法 filterSql del_flag=0 and status='1' 通过")
    void acceptsAndChain() {
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql("del_flag=0 and status='1'"));
    }

    @Test
    @DisplayName("空白与换行分隔的简单比较仍通过")
    void acceptsWhitespaceBetweenTokens() {
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql("del_flag=0\nand\nstatus='1'"));
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql("status IN ('1','2')"));
    }

    @Test
    @DisplayName("空/null 视为无过滤条件")
    void emptyIsNoOp() {
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql(""));
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql(null));
        assertDoesNotThrow(() -> SqlInjectionUtil.specialFilterContentForDictSql("   "));
    }

    @Test
    @DisplayName("含分号的语法被拒绝")
    void rejectsSemicolon() {
        assertThrows(JeecgSqlInjectionException.class,
                () -> SqlInjectionUtil.specialFilterContentForDictSql("id=1;"));
    }

    @Test
    @DisplayName("含 SQL 行注释标记被拒绝")
    void rejectsLineCommentMarker() {
        assertThrows(RuntimeException.class,
                () -> SqlInjectionUtil.specialFilterContentForDictSql("id=1--"));
    }

    @Test
    @DisplayName("含块注释标记被拒绝")
    void rejectsBlockCommentMarker() {
        assertThrows(RuntimeException.class,
                () -> SqlInjectionUtil.specialFilterContentForDictSql("id=1 /*"));
    }

    @Test
    @DisplayName("函数调用形态 sleep( 被拒绝")
    void rejectsParenCallPattern() {
        assertThrows(JeecgSqlInjectionException.class,
                () -> SqlInjectionUtil.specialFilterContentForDictSql("id=sleep("));
    }

    @Test
    @DisplayName("select 作为多余 token 被拒绝")
    void rejectsSelectAsExtraToken() {
        assertThrows(JeecgSqlInjectionException.class,
                () -> SqlInjectionUtil.specialFilterContentForDictSql("id=1 select"));
    }
}
