package cc.redberry.qplatform.model;

import cc.redberry.qplatform.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class FeynmanRules {
    /** optional settings */
    public Settings settings;

    /** Particle content of the model */
    public FieldRules[] particles;

    /** Interactions in the theory */
    public VertexRule[] interactions;

    public FeynmanRules() {}

    public FeynmanRules(Settings settings, FieldRules[] particles, VertexRule[] interactions) {
        this.settings = settings;
        this.particles = particles;
        this.interactions = interactions;
    }

    public Map<FieldType, FieldRules> getParticleRulesMap() {
        return Arrays.stream(particles).collect(Collectors.toMap(p -> p.fieldType, p -> p));
    }

    @Override
    public String toString() {
        try {
            return JsonUtil.JsonMapperPretty.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public FeynmanRules merge(FeynmanRules oth) {
        var result = new FeynmanRules();
        result.settings = settings;

        result.particles = Arrays.copyOf(particles, particles.length + oth.particles.length);
        System.arraycopy(oth.particles, 0, result.particles, particles.length, oth.particles.length);

        result.interactions = Arrays.copyOf(interactions, interactions.length + oth.interactions.length);
        System.arraycopy(oth.interactions, 0, result.interactions, interactions.length, oth.interactions.length);

        return result;
    }

}
