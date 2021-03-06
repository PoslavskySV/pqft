package cc.redberry.qplatform.model.diagram;

import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.qplatform.model.FieldType;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/** Propagator */
public class Propagator extends DiagramElement {
    /** particle */
    public FieldType fieldType;
    /** index of particle on the first edge */
    public int particleIndex1;
    /** index of particle on the second edge */
    public int particleIndex2;
    /** propagator momentum */
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    public Tensor momentum;

    public Propagator() {}

    public Propagator(TensorField expression, FieldType fieldType, int particleIndex1, int particleIndex2, Tensor momentum) {
        super(expression);
        this.fieldType = fieldType;
        this.particleIndex1 = particleIndex1;
        this.particleIndex2 = particleIndex2;
        this.momentum = momentum;
    }
}
