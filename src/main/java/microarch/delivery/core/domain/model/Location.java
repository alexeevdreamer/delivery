package microarch.delivery.core.domain.model;

import java.util.List;
import java.util.Objects;
import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;

/** A position on the 10 by 10 delivery board. */
public final class Location extends ValueObject<Location> {

    private static final int MIN_COORDINATE = 1;
    private static final int MAX_COORDINATE = 10;

    private final int x;
    private final int y;

    private Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Result<Location, Error> create(int x, int y) {
        Error error = Guard.combine(Guard.againstOutOfRange(x, MIN_COORDINATE, MAX_COORDINATE, "x"),
                Guard.againstOutOfRange(y, MIN_COORDINATE, MAX_COORDINATE, "y"));

        if (error != null) {
            return Result.failure(error);
        }
        return Result.success(new Location(x, y));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** Number of horizontal and vertical steps needed to reach {@code other}. */
    public int distanceTo(Location other) {
        Objects.requireNonNull(other, "other location");
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(x, y);
    }
}
