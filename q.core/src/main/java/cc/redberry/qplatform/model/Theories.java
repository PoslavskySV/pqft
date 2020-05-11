package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.TensorField;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cc.redberry.core.tensor.Tensors.parse;
import static cc.redberry.core.tensor.Tensors.parseExpression;

/** Known theories (QED, QCD, etc.) */
public final class Theories {
    private Theories() {}

    public static Theory qed() {
        return QED.theory;
    }

    public static final class QED {
        public static final FieldRules electron, photon;
        private static final Theory theory;

        static {
            electron = new FieldRules();
            electron.fieldType = new FieldType("electron", new FieldRepresentations(Map.of(GroupType.Spinor, 1), Collections.emptyMap()));
            electron.mass = "me";
            electron.legs = Map.of(
                    LegType.ParticleIn, new LegRule((TensorField) parse("u^i'[p_m]"), parseExpression("u^i'[p_m] = u^i'[p_m]")),
                    LegType.ParticleOut, new LegRule((TensorField) parse("u_i'[p_m]"), parseExpression("u_i'[p_m] = u_i'[p_m]")),
                    LegType.AntiparticleIn, new LegRule((TensorField) parse("u_i'[p_m]"), parseExpression("u_i'[p_m] = u_i'[p_m]")),
                    LegType.AntiparticleOut, new LegRule((TensorField) parse("u^i'[p_m]"), parseExpression("u^i'[p_m] = u^i'[p_m]"))
            );
            electron.propagator = new PropagatorRule((TensorField) parse("D^i'_j'[p_a]"), parseExpression("D^i'_j'[p_a] = d^i'_j'"));
            electron.antiPropagator = new PropagatorRule((TensorField) parse("D^i'_j'[p_a]"), parseExpression("D^i'_j'[p_a] = d^i'_j'"));

            photon = new FieldRules();
            photon.fieldType = new FieldType("photon", new FieldRepresentations(Map.of(GroupType.Lorentz, 1), Collections.emptyMap()));
            photon.mass = "0";
            photon.legs = Map.of(
                    LegType.ParticleIn, new LegRule((TensorField) parse("eps_a[l_a]"), parseExpression("eps_a[l_a] = eps_a[l_a]")),
                    LegType.ParticleOut, new LegRule((TensorField) parse("eps_a[l_a]"), parseExpression("eps_a[l_a] = eps_a[l_a]")),
                    LegType.AntiparticleIn, new LegRule((TensorField) parse("eps_a[l_a]"), parseExpression("eps_a[l_a] = eps_a[l_a]")),
                    LegType.AntiparticleOut, new LegRule((TensorField) parse("eps_a[l_a]"), parseExpression("eps_a[l_a] = eps_a[l_a]")));
            photon.propagator = new PropagatorRule((TensorField) parse("D_ab[p_a]"), parseExpression("D_ab[p_a] = g_ab"));
            photon.antiPropagator = new PropagatorRule((TensorField) parse("D_ab[p_a]"), parseExpression("D_ab[p_a] = g_ab"));


            var vertex = new VertexRule(
                    (TensorField) parse("V_a^i'_j'[k_a, p_a, q_a]"),
                    parseExpression("V_a^i'_j'[p_a, k_a, q_a] = p_a*d^i'_j'"),
                    List.of(electron, electron, photon),
                    new boolean[]{true, false, false}
            );


            var rules = new FeynmanRules();
            rules.settings = new Settings();
            rules.particles = new FieldRules[]{electron, photon};
            rules.interactions = new VertexRule[]{vertex};

            theory = new Theory("QED", rules);
        }
    }
}
