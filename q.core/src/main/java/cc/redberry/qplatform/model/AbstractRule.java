package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.Expression;
import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.core.tensor.Tensors;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
/** Parent for all Feynman rules */
public abstract class AbstractRule {
    /** function that designates the corresponding diagram element; arguments are momentas */
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    @JsonProperty("func")
    public TensorField func;

    /** the rule */
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    @JsonProperty("rule")
    public Expression rule;

    public AbstractRule() {}

    public AbstractRule(TensorField func, Expression rule) {
        this.func = func;
        this.rule = rule;
    }

    /** Apply rule to analytica expression (diagram) */
    public Tensor apply(Tensor expr) {
        return rule.transform(expr);
    }

    /** Get func with specified momentums */
    public TensorField getFunc(Tensor... momentums) {
        return Tensors.field(func.getName(), func.getIndices(), momentums);
    }
}
