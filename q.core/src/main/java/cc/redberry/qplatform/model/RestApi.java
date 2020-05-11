package cc.redberry.qplatform.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class RestApi {
    /** value or the default value */
    public static final class ValueOrDefault<V> implements JsonModel {
        @JsonProperty("value")
        public V value;
        @JsonProperty("isDefault")
        public boolean isDefault;

        public ValueOrDefault() {}

        public ValueOrDefault(V value, boolean isDefault) {
            this.value = value;
            this.isDefault = isDefault;
        }
    }

    /** Request generate relevant Feynman diagrams */
    public static class DiagramsCalculationRequest implements JsonModel {
        @JsonProperty("theory")
        public Theory theory;
        @JsonProperty("matrixElementDescription")
        public MatrixElementDescription description;

        public DiagramsCalculationRequest() {}

        public DiagramsCalculationRequest(Theory theory, MatrixElementDescription description) {
            this.theory = theory;
            this.description = description;
        }
    }
}
