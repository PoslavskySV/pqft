package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.Expression;
import cc.redberry.core.tensor.TensorField;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/** Feynman rule for external leg */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LegRule extends AbstractRule {
    public LegRule() {}

    public LegRule(TensorField func, Expression definition) {
        super(func, definition);
    }
}
