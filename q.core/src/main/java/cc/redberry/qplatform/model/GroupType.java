package cc.redberry.qplatform.model;

import cc.redberry.core.indices.IndexType;
import cc.redberry.core.indices.IndicesFactory;
import cc.redberry.core.indices.IndicesUtils;
import cc.redberry.core.tensor.SimpleTensor;
import cc.redberry.core.tensor.Tensors;

/** Types of Lie groups available in the Platform */
public enum GroupType {
    Lorentz(IndexType.LatinLower),
    Spinor(IndexType.Matrix1),
    Unitary(IndexType.Matrix2, IndexType.LatinUpper),
    Sigma1(IndexType.GreekLower),
    Sigma2(IndexType.GreekUpper);

    GroupType(IndexType indexType) {
        this(indexType, null);
    }

    GroupType(IndexType indexType, IndexType adjointIndexType) {
        this.indexType = indexType;
        this.adjointIndexType = adjointIndexType;
    }

    public final IndexType indexType;
    public final IndexType adjointIndexType;

    public boolean hasAdjointRep() { return adjointIndexType != null; }

    public static SimpleTensor momenta(String name) {
        return Tensors.simpleTensor(name, IndicesFactory.createSimple(null, IndicesUtils.createIndex(0, Lorentz.indexType, false)));
    }
}
