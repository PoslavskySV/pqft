package cc.redberry.qplatform.model;

import cc.redberry.qplatform.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Theory {
    /** Unique theory name */
    public String id;
    /** Feynman rules */
    public FeynmanRules feynmanRules;

    public Theory() {}

    public Theory(String id, FeynmanRules feynmanRules) {
        this.id = id;
        this.feynmanRules = feynmanRules;
    }

    @Override
    public String toString() {
        try {
            return JsonUtil.JsonMapperPretty.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
