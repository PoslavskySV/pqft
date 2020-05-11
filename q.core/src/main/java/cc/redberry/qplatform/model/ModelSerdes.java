package cc.redberry.qplatform.model;

import cc.redberry.qplatform.model.diagram.Diagram;
import cc.redberry.qplatform.util.JsonSerde;

/**
 *
 */
public class ModelSerdes {
    /** Kafka (De)Serializers for FeynmanRules */
    public static final JsonSerde<FeynmanRules> FeynmanRules = new JsonSerde<>(FeynmanRules.class);
    /** Kafka (De)Serializers for Theory */
    public static final JsonSerde<Theory> Theory = new JsonSerde<>(Theory.class);
    /** Kafka (De)Serializers for MatrixElementDescription */
    public static final JsonSerde<MatrixElementDescription> MatrixElementDescription = new JsonSerde<>(MatrixElementDescription.class);
    /** Kafka (De)Serializers for raw diagrams */
    public static final JsonSerde<Diagram> Diagram = new JsonSerde<>(Diagram.class);

}
