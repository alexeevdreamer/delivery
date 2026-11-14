package microarch.delivery.core.domain.model.courier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import libs.errs.Error;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.order.Volume;
import org.junit.jupiter.api.Test;

class AssignmentTest {

    private static final UUID ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void startsAssignedWithRequiredOrderData() {
        Assignment assignment = assignment(ID, ORDER_ID);

        assertThat(assignment.getId()).isEqualTo(ID);
        assertThat(assignment.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(assignment.getVolume()).isEqualTo(volume(2));
        assertThat(assignment.getLocation()).isEqualTo(location(3, 3));
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void rejectsMissingRequiredData() {
        assertRequired(Assignment.create(null, ORDER_ID, volume(2), location(3, 3)), "id");
        assertRequired(Assignment.create(new UUID(0, 0), ORDER_ID, volume(2), location(3, 3)), "id");
        assertRequired(Assignment.create(ID, null, volume(2), location(3, 3)), "orderId");
        assertRequired(Assignment.create(ID, new UUID(0, 0), volume(2), location(3, 3)), "orderId");
        assertRequired(Assignment.create(ID, ORDER_ID, null, location(3, 3)), "volume");
        assertRequired(Assignment.create(ID, ORDER_ID, volume(2), null), "location");
    }

    @Test
    void equalityDependsOnIdentityRatherThanOrderDataOrStatus() {
        Assignment first = assignment(ID, ORDER_ID);
        Assignment sameId = assignment(ID, UUID.randomUUID());
        Assignment differentId = assignment(UUID.randomUUID(), ORDER_ID);

        assertThat(first).isEqualTo(sameId).hasSameHashCodeAs(sameId);
        assertThat(first).isNotEqualTo(differentId);
        assertThat(first.complete(location(3, 3)).isSuccess()).isTrue();
        assertThat(first).isEqualTo(sameId);
    }

    @Test
    void completesAtOrderLocationOrOneOrthogonalStepAway() {
        for (Location courierLocation : new Location[] { location(3, 3), location(2, 3), location(4, 3), location(3, 2),
                location(3, 4) }) {
            Assignment assignment = assignment(UUID.randomUUID(), ORDER_ID);

            assertThat(assignment.complete(courierLocation).isSuccess()).isTrue();
            assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        }
    }

    @Test
    void rejectsDistantCourierWithoutChangingStatus() {
        Assignment assignment = assignment(ID, ORDER_ID);

        UnitResult<Error> diagonal = assignment.complete(location(4, 4));
        UnitResult<Error> twoSteps = assignment.complete(location(5, 3));

        assertThat(diagonal.getError().getCode()).isEqualTo("assignment.courier.too.far");
        assertThat(twoSteps.getError().getCode()).isEqualTo("assignment.courier.too.far");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void rejectsNullLocationAndRepeatedCompletion() {
        Assignment assignment = assignment(ID, ORDER_ID);

        assertThat(assignment.complete(null).getError().getCode()).isEqualTo("value.is.required");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(assignment.complete(location(3, 3)).isSuccess()).isTrue();
        assertThat(assignment.complete(location(3, 3)).getError().getCode()).isEqualTo("assignment.already.completed");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    private static Assignment assignment(UUID id, UUID orderId) {
        return Assignment.create(id, orderId, volume(2), location(3, 3)).getValue();
    }

    private static Volume volume(int value) {
        return Volume.create(value).getValue();
    }

    private static Location location(int x, int y) {
        return Location.create(x, y).getValue();
    }

    private static void assertRequired(Result<Assignment, Error> result, String field) {
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains(field);
    }
}
