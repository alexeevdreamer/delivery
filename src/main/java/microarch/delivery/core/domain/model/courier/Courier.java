package microarch.delivery.core.domain.model.courier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.order.Volume;

/** A courier controls movement, capacity, and its assignments. */
public final class Courier extends Aggregate<UUID> {

    private static final int MAX_VOLUME = 20;

    private final String name;
    private Location location;
    private final Volume maxVolume;
    private final List<Assignment> assignments = new ArrayList<>();

    private Courier(String name, Location location) {
        super(UUID.randomUUID());
        this.name = name;
        this.location = location;
        this.maxVolume = Volume.create(MAX_VOLUME).getValueOrThrow();
    }

    public static Result<Courier, Error> create(String name, Location location) {
        Error error = Guard.combine(Guard.againstNullOrEmpty(name, "name"),
                location == null ? GeneralErrors.valueIsRequired("location") : null);
        if (error != null) {
            return Result.failure(error);
        }
        return Result.success(new Courier(name, location));
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public Volume getMaxVolume() {
        return maxVolume;
    }

    public List<Assignment> getAssignments() {
        return List.copyOf(assignments);
    }

    public boolean canTakeOrder(Volume volume) {
        if (volume == null) {
            return false;
        }
        long activeVolume = assignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED)
                .mapToLong(assignment -> assignment.getVolume().getValue()).sum();
        return activeVolume + volume.getValue() <= maxVolume.getValue();
    }

    public Result<Assignment, Error> assignOrder(UUID orderId, Volume volume, Location orderLocation) {
        Error error = Guard.combine(Guard.againstNullOrEmpty(orderId, "orderId"),
                volume == null ? GeneralErrors.valueIsRequired("volume") : null,
                orderLocation == null ? GeneralErrors.valueIsRequired("orderLocation") : null);
        if (error != null) {
            return Result.failure(error);
        }
        if (assignments.stream().anyMatch(assignment -> assignment.getOrderId().equals(orderId))) {
            return Result
                    .failure(Error.of("courier.order.already.assigned", "Order is already assigned to this courier"));
        }
        if (!canTakeOrder(volume)) {
            return Result.failure(Error.of("courier.capacity.exceeded", "Order exceeds courier capacity"));
        }

        Assignment assignment = Assignment.create(UUID.randomUUID(), orderId, volume, orderLocation).getValueOrThrow();
        assignments.add(assignment);
        return Result.success(assignment);
    }

    public UnitResult<Error> completeAssignment(UUID orderId) {
        Error error = Guard.againstNullOrEmpty(orderId, "orderId");
        if (error != null) {
            return UnitResult.failure(error);
        }
        for (Assignment assignment : assignments) {
            if (assignment.getOrderId().equals(orderId)) {
                return assignment.complete(location);
            }
        }
        return UnitResult.failure(GeneralErrors.notFound("assignment for order", orderId));
    }

    public UnitResult<Error> move(Location destination) {
        if (destination == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("destination"));
        }
        if (location.distanceTo(destination) > 1) {
            return UnitResult.failure(Error.of("courier.move.too.far", "Courier can move at most one step"));
        }
        location = destination;
        return UnitResult.success();
    }
}
