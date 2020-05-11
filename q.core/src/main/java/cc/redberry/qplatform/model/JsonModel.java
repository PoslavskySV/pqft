package cc.redberry.qplatform.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Passes JSON serialization settings to it's descendants
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE)
public interface JsonModel {}
