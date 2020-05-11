package cc.redberry.qplatform.qgraf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QgrafVertex {
    public List<QgrafParticle> particles;
    public String name;

    public QgrafVertex(QgrafParticle... particles) {
        this.particles = List.of(particles);
    }

    @Override
    public String toString() {
        return "[" + particles.stream().map(p -> p.name).collect(Collectors.joining(", ")) + " ; vfunct= '" + name + "']";
    }
}
