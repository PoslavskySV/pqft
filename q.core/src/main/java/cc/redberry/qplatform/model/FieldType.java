package cc.redberry.qplatform.model;

import cc.redberry.qplatform.qgraf.QgrafParticle;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** Field identifier; must conform `[a-zA-Z]+` regex */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FieldType {
    public static final String AntiparticlePrefix = "anti";

    @JsonProperty("id")
    public final String id;
    /** representations */
    @JsonProperty("representations")
    public FieldRepresentations representations;

    @JsonCreator
    public FieldType(@JsonProperty("id") String id,
                     @JsonProperty("representations") FieldRepresentations representations) {
        this.id = id;
        this.representations = representations;
    }

    public boolean isFermionic() {
        return representations.representations.getOrDefault(GroupType.Spinor, 0) != 0;
    }

    /** Particle name for the field */
    public String particleName() {
        return id;
    }

    /** Antiparticle name for the field */
    public String antiparticleName() {
        return isFermionic() ? AntiparticlePrefix + particleName() : particleName();
    }

    /** To QGRAF rep */
    public QgrafParticle qgrafParticle() {
        return new QgrafParticle(particleName(), isFermionic());
    }

    /** To QGRAF rep */
    public QgrafParticle qgrafAntiparticle() {
        return new QgrafParticle(antiparticleName(), isFermionic());
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldType fieldType = (FieldType) o;
        return Objects.equals(id, fieldType.id) &&
                Objects.equals(representations, fieldType.representations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, representations);
    }
}
