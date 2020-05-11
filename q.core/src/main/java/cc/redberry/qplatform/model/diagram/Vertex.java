package cc.redberry.qplatform.model.diagram;

import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.qplatform.model.FieldType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/** Interaction point */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Vertex extends DiagramElement {
    /** particles */
    public List<FieldType> particlesIds;
    /** indices of particles */
    public int[] particleIndices;
    /** momentums */
    public List<Tensor> momentums;

    public Vertex() {}

    public Vertex(TensorField expression, List<FieldType> particlesIds, int[] particleIndices, List<Tensor> momentums) {
        super(expression);
        this.particlesIds = particlesIds;
        this.particleIndices = particleIndices;
        this.momentums = momentums;
    }
}
