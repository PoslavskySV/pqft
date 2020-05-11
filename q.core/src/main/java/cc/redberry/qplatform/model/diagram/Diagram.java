package cc.redberry.qplatform.model.diagram;

import cc.redberry.core.tensor.ProductBuilder;
import cc.redberry.core.tensor.Tensor;
import cc.redberry.qplatform.model.FeynmanRules;
import cc.redberry.qplatform.model.FieldRules;
import cc.redberry.qplatform.model.LegRule;
import cc.redberry.qplatform.model.VertexRule;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 *
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Diagram {
    /** Numeric factor */
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    public Tensor factor;
    /** legs */
    public List<Leg> legs;
    /** propagators */
    public List<Propagator> propagators;
    /** vertices */
    public List<Vertex> vertices;
    /** diagram serial number */
    public int index;
    /** number of loops */
    public int nLoops;

    public Diagram() {}

    public Diagram(Tensor factor, List<Leg> legs, List<Propagator> propagators, List<Vertex> vertices, int index, int nLoops) {
        this.factor = factor;
        this.legs = legs;
        this.propagators = propagators;
        this.vertices = vertices;
        this.index = index;
        this.nLoops = nLoops;
    }

    /** Analytical expression for the diagram */
    public Tensor expression() {
        var pb = new ProductBuilder();
        pb.put(factor);

        for (Leg leg : legs)
            pb.put(leg.expression);

        for (Propagator propagator : propagators)
            pb.put(propagator.expression);

        for (Vertex vertex : vertices)
            pb.put(vertex.expression);

        return pb.build();
    }

    /** Evaluate by substituting Feynman rules */
    public Tensor applyRules(FeynmanRules feynmanRules) {
        var expr = expression();

        for (FieldRules particle : feynmanRules.particles) {
            expr = particle.propagator.apply(expr);
            expr = particle.antiPropagator.apply(expr);
            for (LegRule leg : particle.legs.values())
                expr = leg.apply(expr);
        }

        for (VertexRule vertex : feynmanRules.interactions)
            expr = vertex.apply(expr);

        return expr;
    }
}
