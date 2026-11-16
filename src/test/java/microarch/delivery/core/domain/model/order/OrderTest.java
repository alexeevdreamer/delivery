package microarch.delivery.core.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.Location;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final UUID ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void startsCreatedWithProvidedIdentityAndValues() {
        Location location = location(3, 4);
        Volume volume = volume(5);

        Order order = Order.create(ID, location, volume).getValue();

        assertThat(order.getId()).isEqualTo(ID);
        assertThat(order.getLocation()).isSameAs(location);
        assertThat(order.getVolume()).isSameAs(volume);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void requiresIdentityLocationAndVolume() {
        assertRequired(Order.create(null, location(1, 1), volume(1)), "id");
        assertRequired(Order.create(new UUID(0, 0), location(1, 1), volume(1)), "id");
        assertRequired(Order.create(ID, null, volume(1)), "location");
        assertRequired(Order.create(ID, location(1, 1), null), "volume");
    }

    @Test
    void onlyAllowsCreatedAssignedCompletedSequence() {
        Order order = order();

        assertThat(order.complete().getError().getCode()).isEqualTo("order.cannot.complete");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.assign().isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(order.assign().getError().getCode()).isEqualTo("order.cannot.assign");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(order.complete().isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.assign().getError().getCode()).isEqualTo("order.cannot.assign");
        assertThat(order.complete().getError().getCode()).isEqualTo("order.cannot.complete");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void aggregateIdentityDoesNotChangeWithStatus() {
        Order first = order();
        Order sameId = Order.create(ID, location(8, 8), volume(9)).getValue();

        assertThat(first).isEqualTo(sameId);
        assertThat(first.assign().isSuccess()).isTrue();
        assertThat(first).isEqualTo(sameId);
    }

    private static Order order() {
        return Order.create(ID, location(3, 4), volume(5)).getValue();
    }

    private static Location location(int x, int y) {
        return Location.create(x, y).getValue();
    }

    private static Volume volume(int value) {
        return Volume.create(value).getValue();
    }

    private static void assertRequired(Result<Order, Error> result, String field) {
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains(field);
    }
}
