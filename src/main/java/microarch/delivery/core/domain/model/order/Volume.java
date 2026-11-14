package microarch.delivery.core.domain.model.order;

import java.util.List;
import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;

/** The positive amount of space occupied by an order. */
public final class Volume extends ValueObject<Volume> {

    private final int value;

    private Volume(int value) {
        this.value = value;
    }

    public static Result<Volume, Error> create(int value) {
        Error error = Guard.againstOutOfRange(value, 1, Integer.MAX_VALUE, "volume");
        if (error != null) {
            return Result.failure(error);
        }
        return Result.success(new Volume(value));
    }

    public int getValue() {
        return value;
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(value);
    }
}
