package org.jeecg.config.shiro;

import org.jeecg.test.security.PrintTestResultExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【issue/9840】字典接口 JWT 规则必须出现在静态后缀 anon 之前；catch-all jwt 仍保留。
 */
@ExtendWith(PrintTestResultExtension.class)
public class Issue9840_ShiroDictJwtFilterOrderTest {

    @Test
    @DisplayName("getDictItems 为 jwt 且排在 /**/*.js 之前")
    void dictJwtBeforeStaticJsAnon() {
        Map<String, String> chain = new LinkedHashMap<>();
        ShiroConfig.putDictJwtThenStaticResourceAnon(chain);

        assertEquals("jwt", chain.get("/sys/dict/getDictItems/**"));
        List<String> keys = new ArrayList<>(chain.keySet());
        int dictIdx = keys.indexOf("/sys/dict/getDictItems/**");
        int jsIdx = keys.indexOf("/**/*.js");
        assertTrue(dictIdx >= 0, "missing dict jwt path");
        assertTrue(jsIdx >= 0, "missing static js anon rule");
        assertTrue(dictIdx < jsIdx, "dict jwt must be registered before /**/*.js");
    }

    @Test
    @DisplayName("全部字典 JWT 路径均为 jwt 且均在 /**/*.js 之前")
    void allDictJwtPathsBeforeStaticJs() {
        Map<String, String> chain = new LinkedHashMap<>();
        ShiroConfig.putDictJwtThenStaticResourceAnon(chain);
        List<String> keys = new ArrayList<>(chain.keySet());
        int jsIdx = keys.indexOf("/**/*.js");
        assertTrue(jsIdx >= 0);
        for (String path : ShiroConfig.DICT_API_JWT_PATHS) {
            assertEquals("jwt", chain.get(path), path);
            int idx = keys.indexOf(path);
            assertTrue(idx >= 0 && idx < jsIdx, path + " must appear before /**/*.js");
        }
        assertEquals("anon", chain.get("/**/*.js"));
    }

    @Test
    @DisplayName("shiroFilter 源码仍在链尾登记 /** jwt")
    void catchAllJwtStillPresentInSource() throws Exception {
        Path src = Paths.get("src/main/java/org/jeecg/config/shiro/ShiroConfig.java");
        if (!Files.isRegularFile(src)) {
            src = Paths.get("jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/config/shiro/ShiroConfig.java");
        }
        assertTrue(Files.isRegularFile(src), "ShiroConfig.java not found: " + src.toAbsolutePath());
        String content = Files.readString(src, StandardCharsets.UTF_8);
        assertTrue(content.contains("put(\"/**\", \"jwt\")"), "catch-all jwt must remain");
        int dictHelper = content.indexOf("putDictJwtThenStaticResourceAnon");
        int catchAll = content.lastIndexOf("put(\"/**\", \"jwt\")");
        assertTrue(dictHelper >= 0 && catchAll > dictHelper, "catch-all jwt must stay after dict jwt registration");
    }
}
