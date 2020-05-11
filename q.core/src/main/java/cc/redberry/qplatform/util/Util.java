package cc.redberry.qplatform.util;

import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class Util {
    private Util() {}

    public static Properties map2prop(Map<String, Object> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }
}
