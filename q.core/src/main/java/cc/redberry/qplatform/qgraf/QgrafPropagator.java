package cc.redberry.qplatform.qgraf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QgrafPropagator {
    public QgrafParticle particle, antiparticle;
    public String functionName, mass;

    public QgrafPropagator() {}

    public QgrafPropagator(QgrafParticle particle, QgrafParticle antiparticle, String functionName, String mass) {
        this.particle = particle;
        this.antiparticle = antiparticle;
        this.functionName = functionName;
        this.mass = mass;
    }

    @Override
    public String toString() {
        return "[particle, antiparticle, sign ; pfunct= 'functionName', m= 'mass']"
                .replace("antiparticle", antiparticle.name)
                .replace("particle", particle.name)
                .replace("sign", particle.isFermion ? "-" : "+")
                .replace("functionName", functionName)
                .replace("mass", mass);
    }
}
