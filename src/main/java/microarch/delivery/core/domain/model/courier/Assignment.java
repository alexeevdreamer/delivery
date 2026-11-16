package microarch.delivery.core.domain.model.courier;

import java.util.UUID;
import libs.ddd.BaseEntity;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.order.Volume;

/** A courier's delivery task for one order. */
public final class Assignment extends BaseEntity<UUID> {

    private final UUID orderId;
    private final Volume volume;
    private final Location location;
    private AssignmentStatus status;

    private Assignment(UUID id, UUID orderId, Volume volume, Location location) {
        super(id);
        this.orderId = orderId;
        this.volume = volume;
        this.location = location;
        this.status = AssignmentStatus.ASSIGNED;
    }

    public static Result<Assignment, Error> create(UUID id, UUID orderId, Volume volume, Location location) {
        Error error = Guard.combine(Guard.againstNullOrEmpty(id, "id"), Guard.againstNullOrEmpty(orderId, "orderId"),
                volume == null ? GeneralErrors.valueIsRequired("volume") : null,
                location == null ? GeneralErrors.valueIsRequired("location") : null);
        if (error != null) {
            return Result.failure(error);
        }
        return Result.success(new Assignment(id, orderId, volume, location));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Volume getVolume() {
        return volume;
    }

    public Location getLocation() {
        return location;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    UnitResult<Error> complete(Location courierLocation) {
        if (courierLocation == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("courierLocation"));
        }
        if (status == AssignmentStatus.COMPLETED) {
            return UnitResult.failure(Error.of("assignment.already.completed", "Assignment is already completed"));
        }
        if (location.distanceTo(courierLocation) > 1) {
            return UnitResult
                    .failure(Error.of("assignment.courier.too.far", "Courier must be within one step of the order"));
        }
        status = AssignmentStatus.COMPLETED;
        return UnitResult.success();
    }
}
