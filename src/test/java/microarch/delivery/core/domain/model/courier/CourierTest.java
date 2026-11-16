package microarch.delivery.core.domain.model.courier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.order.Volume;
import org.junit.jupiter.api.Test;

class CourierTest {

    private static final UUID ORDER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void startsWithGeneratedIdentityTwentyLitersAndNoAssignments() {
        Courier first = courier(1, 1);
        Courier second = courier(1, 1);

        assertThat(first.getId()).isNotNull().isNotEqualTo(new UUID(0, 0)).isNotEqualTo(second.getId());
        assertThat(first.getName()).isEqualTo("Alex");
        assertThat(first.getLocation()).isEqualTo(location(1, 1));
        assertThat(first.getMaxVolume()).isEqualTo(volume(20));
        assertThat(first.getAssignments()).isEmpty();
    }

    @Test
    void requiresNameAndLocation() {
        assertRequired(Courier.create(null, location(1, 1)), "name");
        assertRequired(Courier.create("  ", location(1, 1)), "name");
        assertRequired(Courier.create("Alex", null), "location");
    }

    @Test
    void acceptsCapacityBoundaryAndRejectsOverflowWithoutChangingAssignments() {
        Courier courier = courier(1, 1);

        assertThat(courier.canTakeOrder(volume(20))).isTrue();
        Assignment first = courier.assignOrder(ORDER_ID, volume(12), location(2, 1)).getValue();
        assertThat(first.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(first.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(courier.canTakeOrder(volume(8))).isTrue();
        assertThat(courier.canTakeOrder(volume(9))).isFalse();
        assertThat(courier.assignOrder(UUID.randomUUID(), volume(8), location(3, 1)).isSuccess()).isTrue();
        assertThat(courier.canTakeOrder(volume(1))).isFalse();
        assertThat(courier.assignOrder(UUID.randomUUID(), volume(1), location(3, 1)).getError().getCode())
                .isEqualTo("courier.capacity.exceeded");
        assertThat(courier.getAssignments()).hasSize(2);
    }

    @Test
    void rejectsInvalidAndDuplicateOrdersWithoutChangingAssignments() {
        Courier courier = courier(1, 1);

        assertRequired(courier.assignOrder(null, volume(1), location(1, 1)), "orderId");
        assertRequired(courier.assignOrder(new UUID(0, 0), volume(1), location(1, 1)), "orderId");
        assertRequired(courier.assignOrder(ORDER_ID, null, location(1, 1)), "volume");
        assertRequired(courier.assignOrder(ORDER_ID, volume(1), null), "orderLocation");
        assertThat(courier.canTakeOrder(null)).isFalse();
        assertThat(courier.assignOrder(ORDER_ID, volume(1), location(1, 1)).isSuccess()).isTrue();
        assertThat(courier.assignOrder(ORDER_ID, volume(1), location(1, 1)).getError().getCode())
                .isEqualTo("courier.order.already.assigned");
        assertThat(courier.getAssignments()).hasSize(1);
        assertThatThrownBy(() -> courier.getAssignments().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void completionRequiresProximityAndReleasesCapacity() {
        Courier courier = courier(1, 1);
        courier.assignOrder(ORDER_ID, volume(20), location(3, 1)).getValue();

        assertThat(courier.completeAssignment(ORDER_ID).getError().getCode()).isEqualTo("assignment.courier.too.far");
        assertThat(courier.getAssignments().getFirst().getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(courier.canTakeOrder(volume(1))).isFalse();
        assertThat(courier.move(location(2, 1)).isSuccess()).isTrue();
        assertThat(courier.completeAssignment(ORDER_ID).isSuccess()).isTrue();
        assertThat(courier.getAssignments().getFirst().getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(courier.canTakeOrder(volume(20))).isTrue();
        assertThat(courier.assignOrder(UUID.randomUUID(), volume(20), location(4, 1)).isSuccess()).isTrue();
        assertThat(courier.completeAssignment(ORDER_ID).getError().getCode()).isEqualTo("assignment.already.completed");
    }

    @Test
    void completionRequiresKnownOrderId() {
        Courier courier = courier(1, 1);

        assertThat(courier.completeAssignment(null).getError().getCode()).isEqualTo("value.is.required");
        assertThat(courier.completeAssignment(new UUID(0, 0)).getError().getCode()).isEqualTo("value.is.required");
        assertThat(courier.completeAssignment(ORDER_ID).getError().getCode()).isEqualTo("record.not.found");
    }

    @Test
    void movesAtMostOneOrthogonalStepAndKeepsLocationOnFailure() {
        Courier courier = courier(2, 2);

        assertThat(courier.move(null).getError().getCode()).isEqualTo("value.is.required");
        assertThat(courier.move(location(3, 3)).getError().getCode()).isEqualTo("courier.move.too.far");
        assertThat(courier.move(location(4, 2)).getError().getCode()).isEqualTo("courier.move.too.far");
        assertThat(courier.getLocation()).isEqualTo(location(2, 2));
        assertThat(courier.move(location(2, 2)).isSuccess()).isTrue();
        assertThat(courier.move(location(3, 2)).isSuccess()).isTrue();
        assertThat(courier.move(location(3, 1)).isSuccess()).isTrue();
        assertThat(courier.getLocation()).isEqualTo(location(3, 1));
    }

    @Test
    void hugeVolumeDoesNotOverflowCapacityCalculation() {
        Courier courier = courier(1, 1);

        assertThat(courier.canTakeOrder(volume(Integer.MAX_VALUE))).isFalse();
        assertThat(courier.assignOrder(ORDER_ID, volume(Integer.MAX_VALUE), location(1, 1)).getError().getCode())
                .isEqualTo("courier.capacity.exceeded");
    }

    private static Courier courier(int x, int y) {
        return Courier.create("Alex", location(x, y)).getValue();
    }

    private static Location location(int x, int y) {
        return Location.create(x, y).getValue();
    }

    private static Volume volume(int value) {
        return Volume.create(value).getValue();
    }

    private static void assertRequired(Result<?, Error> result, String field) {
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains(field);
    }
}
