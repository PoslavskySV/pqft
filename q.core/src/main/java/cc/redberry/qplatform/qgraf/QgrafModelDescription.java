package cc.redberry.qplatform.qgraf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** That's translated to QGRAF model file */
public class QgrafModelDescription {
    public String name;
    public List<QgrafPropagator> propagators;
    public List<QgrafVertex> vertices;

    public QgrafModelDescription() {}

    public QgrafModelDescription(String name, List<QgrafPropagator> propagators, List<QgrafVertex> vertices) {
        this.name = name;
        this.propagators = propagators;
        this.vertices = vertices;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        sb.append("%  propagators\n");
        for (QgrafPropagator pr : propagators)
            sb.append(pr.toString()).append("\n");

        sb.append("\n");

        sb.append("%  vertices\n");
        for (QgrafVertex vx : vertices)
            sb.append(vx.toString()).append("\n");

        return sb.toString();
    }

    public static QgrafModelDescription merge(QgrafModelDescription a, QgrafModelDescription b, QgrafVertex... vertices) {
        var result = new QgrafModelDescription();
        result.name = a.name + b.name;
        result.propagators = new ArrayList<>();
        result.propagators.addAll(a.propagators);
        result.propagators.addAll(b.propagators);

        result.vertices = new ArrayList<>();
        result.vertices.addAll(a.vertices);
        result.vertices.addAll(b.vertices);
        result.vertices.addAll(Arrays.asList(vertices));
        return result;
    }
}
