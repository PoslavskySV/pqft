package cc.redberry.qplatform.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Map;

/** Rules for particles */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class FieldRules {
    /** particle */
    public FieldType fieldType;
    /** the mass */
    public String mass;
    /** rules for legs */
    public Map<LegType, LegRule> legs;
    /** rules for field propagator */
    public PropagatorRule propagator;
    /** rules for anti-field propagator */
    public PropagatorRule antiPropagator;

    public FieldRules() {}

    public FieldRules(FieldType fieldType, String mass, Map<LegType, LegRule> legs, PropagatorRule propagator, PropagatorRule antiPropagator) {
        this.fieldType = fieldType;
        this.mass = mass;
        this.legs = legs;
        this.propagator = propagator;
        this.antiPropagator = antiPropagator;
    }
}
