package org.bks.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


@Slf4j
public class JacksonUtil {

    private JacksonUtil() {
        throw new RuntimeException("Utility class");
    }

    /**
     * 单例get
     *
     * @return
     */
    public static ObjectMapper getInstance() {
        return SingletonHolder.mapper;
    }


    /**
     * 单例内部类
     */
    private static class SingletonHolder {
        private static final ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        private static Configuration jsonPathConf = null;

        static {
            //Jackson 支持org.json.JSONObject
            mapper.registerModule(new JsonOrgModule());
            // 支持LocalDateTime
            mapper.registerModule(new JavaTimeModule());

            //Jackson 反序列化配置
            // 属性在json有, entity有, 但标记为ignore注解, 不抛出异常
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            // 属性在json有, entity没有,  不抛出异常
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // 支持json中的key无双引号
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            // 支持带单引号的key
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            // 支持0开头的整数, 如001
            mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
            // 支持回车符号
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            // int类型为null, 则抛出异常
            mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
            // 枚举找不到值, 不抛出异常
            mapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);


            //Jackson 序列化配置
            // 空值输出 字段名: null
            mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
            // transient注解不输出字段
            mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

            //JsonPath
            Configuration.setDefaults(new Configuration.Defaults() {
                private final JsonProvider jsonProvider = new JacksonJsonProvider(mapper);
                private final MappingProvider mappingProvider = new JacksonMappingProvider(mapper);

                @Override
                public JsonProvider jsonProvider() {
                    return jsonProvider;
                }

                @Override
                public MappingProvider mappingProvider() {
                    return mappingProvider;
                }

                @Override
                public Set<Option> options() {
                    return EnumSet.noneOf(Option.class);
                }
            });

            jsonPathConf = Configuration.defaultConfiguration()
                    .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
                    .addOptions(Option.SUPPRESS_EXCEPTIONS)
            ;

            //others
        }
    }
}
