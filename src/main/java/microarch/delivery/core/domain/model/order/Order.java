package microarch.delivery.core.domain.model.order;

import java.util.UUID;
import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.Location;

/** An order controls its own lifecycle. */
public final class Order extends Aggregate<UUID> {

    private final Location location;
    private final Volume volume;
    private OrderStatus status;

    private Order(UUID id, Location location, Volume volume) {
        super(id);
        this.location = location;
        this.volume = volume;
        this.status = OrderStatus.CREATED;
    }

    public static Result<Order, Error> create(UUID id, Location location, Volume volume) {
        Error error = Guard.combine(Guard.againstNullOrEmpty(id, "id"),
                location == null ? GeneralErrors.valueIsRequired("location") : null,
                volume == null ? GeneralErrors.valueIsRequired("volume") : null);
        if (error != null) {
            return Result.failure(error);
        }
        return Result.success(new Order(id, location, volume));
    }

    public Location getLocation() {
        return location;
    }

    public Volume getVolume() {
        return volume;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public UnitResult<Error> assign() {
        if (status != OrderStatus.CREATED) {
            return UnitResult.failure(Error.of("order.cannot.assign", "Only a created order can be assigned"));
        }
        status = OrderStatus.ASSIGNED;
        return UnitResult.success();
    }

    public UnitResult<Error> complete() {
        if (status != OrderStatus.ASSIGNED) {
            return UnitResult.failure(Error.of("order.cannot.complete", "Only an assigned order can be completed"));
        }
        status = OrderStatus.COMPLETED;
        return UnitResult.success();
    }
}
