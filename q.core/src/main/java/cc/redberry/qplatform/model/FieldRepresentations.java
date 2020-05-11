package cc.redberry.qplatform.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Map;
import java.util.Objects;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FieldRepresentations {
    /** Map group type -> number of group indices */
    public Map<GroupType, Integer> representations;
    /** Map group type -> number of group indices in adjoint representation */
    public Map<GroupType, Integer> adjointRepresentations;

    public FieldRepresentations() {}

    public FieldRepresentations(Map<GroupType, Integer> representations,
                                Map<GroupType, Integer> adjointRepresentations) {
        this.representations = representations;
        this.adjointRepresentations = adjointRepresentations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldRepresentations that = (FieldRepresentations) o;
        return Objects.equals(representations, that.representations) &&
                Objects.equals(adjointRepresentations, that.adjointRepresentations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(representations, adjointRepresentations);
    }
}
