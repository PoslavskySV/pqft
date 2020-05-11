package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.Expression;
import cc.redberry.core.tensor.TensorField;
import com.fasterxml.jackson.annotation.JsonAutoDetect;


/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PropagatorRule extends AbstractRule {
    public PropagatorRule() { }

    public PropagatorRule(TensorField func, Expression definition) {
        super(func, definition);
    }
}
