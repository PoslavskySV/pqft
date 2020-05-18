package cc.redberry.qplatform.model.diagram;

import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.qplatform.model.FieldType;
import cc.redberry.qplatform.model.LegType;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/** External leg */
public class Leg extends DiagramElement {
    /** particle */
    public FieldType fieldType;
    /** leg type */
    public LegType legType;
    /** edge index */
    public int particleIndex;

    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    public Tensor momentum;

    public Leg() {}

    public Leg(TensorField expression, FieldType fieldType, LegType legType, int particleIndex, Tensor momentum) {
        super(expression);
        this.fieldType = fieldType;
        this.legType = legType;
        this.particleIndex = particleIndex;
        this.momentum = momentum;
    }
}
