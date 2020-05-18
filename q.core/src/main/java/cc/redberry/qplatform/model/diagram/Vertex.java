package cc.redberry.qplatform.model.diagram;

import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.qplatform.model.FieldType;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/** Interaction point */
public class Vertex extends DiagramElement {
    /** particles */
    public List<FieldType> particlesIds;
    /** indices of particles */
    public int[] particleIndices;
    /** momentums */
    @JsonSerialize(contentUsing = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(contentUsing = TensorSerializers.TensorDeserializer.class)
    public List<Tensor> momentums;

    public Vertex() {}

    public Vertex(TensorField expression, List<FieldType> particlesIds, int[] particleIndices, List<Tensor> momentums) {
        super(expression);
        this.particlesIds = particlesIds;
        this.particleIndices = particleIndices;
        this.momentums = momentums;
    }
}
