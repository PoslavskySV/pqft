package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.SimpleTensor;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ScatteringProcess {
    public FeynmanRules feynmanRules;

    public FieldType[] in;
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    public SimpleTensor[] inMomentums;

    public FieldType[] out;
    @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
    @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
    public SimpleTensor[] outMomentums;

    public int nLoops;
}
