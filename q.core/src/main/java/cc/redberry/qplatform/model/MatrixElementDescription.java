package cc.redberry.qplatform.model;

import cc.redberry.core.indices.SimpleIndices;
import cc.redberry.core.tensor.Tensor;
import cc.redberry.qplatform.qgraf.QgrafParticle;
import cc.redberry.qplatform.util.TensorSerializers;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Operator matrix element. */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class MatrixElementDescription {
    /** maximal number of loops */
    public int nLoops;
    /** border points */
    public List<ExternalPoint> points;

    public MatrixElementDescription() {}

    public MatrixElementDescription(int nLoops, ExternalPoint... points) {
        this.nLoops = nLoops;
        this.points = List.of(points);
    }

    /** whether diagram element is truncated (has free indices) */
    public boolean isTruncated() {
        return points.stream().anyMatch(s -> s.truncated);
    }

    /** Unique identifier of the process */
    public String identifier() {
        Set<String> ids = new HashSet<>();
        for (ExternalPoint p : points)
            ids.add(p.field.id + (p.antifield ? "t" : "f") + (p.isIncoming ? "i" : "o") + (p.truncated ? "t" : "_"));
        return String.join("", ids);
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class ExternalPoint {
        /** field type */
        public FieldType field;
        /** whether antiparticle is expected */
        public boolean antifield;
        /** whether point is truncated */
        public boolean truncated;
        /** is incoming */
        public boolean isIncoming;
        /** indices (if truncated) */
        public SimpleIndices indices;
        /** momentum (abs) */
        @JsonSerialize(using = TensorSerializers.TensorSerializer.class)
        @JsonDeserialize(using = TensorSerializers.TensorDeserializer.class)
        public Tensor momentum;

        public ExternalPoint() {}

        public ExternalPoint(FieldType field, boolean antifield, boolean truncated, boolean isIncoming, SimpleIndices indices, Tensor momentum) {
            this.field = field;
            this.antifield = antifield;
            this.truncated = truncated;
            this.isIncoming = isIncoming;
            this.indices = indices;
            this.momentum = momentum;
        }

        public QgrafParticle qgrafParticle() {
            return antifield ? field.qgrafAntiparticle() : field.qgrafParticle();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExternalPoint that = (ExternalPoint) o;
            return antifield == that.antifield &&
                    truncated == that.truncated &&
                    isIncoming == that.isIncoming &&
                    Objects.equals(field, that.field) &&
                    Objects.equals(indices, that.indices) &&
                    Objects.equals(momentum, that.momentum);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, antifield, truncated, isIncoming, indices, momentum);
        }
    }
}
