package cc.redberry.qplatform.util;

import cc.redberry.qplatform.model.JsonModel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.function.Function;

/**
 *
 */
public class Either<L, R> implements JsonModel {
    @JsonProperty("left")
    public final L left;
    @JsonProperty("right")
    public final R right;

    @JsonCreator
    private Either(@JsonProperty("left") L left, @JsonProperty("right") R right) {
        assert (left == null) != (right == null);
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings("unchecked")
    public <NL> Either<NL, R> mapLeft(Function<L, NL> f) {
        if (right != null)
            return (Either<NL, R>) this;
        return left(f.apply(this.left));
    }

    @SuppressWarnings("unchecked")
    public <NR> Either<L, NR> mapRight(Function<R, NR> f) {
        if (left != null)
            return (Either<L, NR>) this;
        return right(f.apply(this.right));
    }

    public boolean isLeft() {return right == null;}

    public boolean isRight() {return left == null;}

    @Override
    public String toString() {
        return isLeft()
                ? "Left[" + left + "]"
                : "Right[" + right + "]";
    }

    public static <L, R> Either<L, R> left(L l) {
        return new Either<>(l, null);
    }

    public static <L, R> Either<L, R> right(R r) {
        return new Either<>(null, r);
    }
}
