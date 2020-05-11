package cc.redberry.qplatform.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 *
 */
public class JsonUtil {
    public static final ObjectMapper JsonMapper = new ObjectMapper();
    public static final ObjectMapper JsonMapperPretty = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
}
