package cc.redberry.qplatform.qgraf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QgrafParticle {
    public String name;
    public boolean isFermion;

    public QgrafParticle() {}

    public QgrafParticle(String name, boolean isFermion) {
        this.name = name;
        this.isFermion = isFermion;
    }
}
