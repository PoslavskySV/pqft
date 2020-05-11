package cc.redberry.qplatform.model.diagram;

import cc.redberry.core.indices.SimpleIndices;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/** Abstract diagram element */
public abstract class DiagramElement {
    /** Analytical expression for diagram element */
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    public TensorField expression;

    public DiagramElement() {}

    public DiagramElement(TensorField expression) {
        this.expression = expression;
    }

    /** Indices of expression */
    public SimpleIndices indices() {
        return expression.getIndices();
    }
}
