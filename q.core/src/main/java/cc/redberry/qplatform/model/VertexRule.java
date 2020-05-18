package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.Expression;
import cc.redberry.core.tensor.TensorField;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 *
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class VertexRule extends AbstractRule {
    /** Particles in vertex (all incoming) */
    public List<FieldRules> particles;
    /** Whether particle is conjugated */
    public boolean[] conjugates;

    public VertexRule() { }

    public VertexRule(TensorField func, Expression definition, List<FieldRules> particles, boolean[] conjugates) {
        super(func, definition);
        this.particles = particles;
        this.conjugates = conjugates;
    }
}
