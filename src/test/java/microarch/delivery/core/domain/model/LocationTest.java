package microarch.delivery.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import libs.errs.Error;
import libs.errs.Result;
import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void acceptsBothBoardCorners() {
        assertThat(location(1, 1).getX()).isEqualTo(1);
        assertThat(location(1, 1).getY()).isEqualTo(1);
        assertThat(location(10, 10).getX()).isEqualTo(10);
        assertThat(location(10, 10).getY()).isEqualTo(10);
    }

    @Test
    void rejectsCoordinatesOutsideBoard() {
        assertInvalid(0, 5, "x");
        assertInvalid(11, 5, "x");
        assertInvalid(5, 0, "y");
        assertInvalid(5, 11, "y");
    }

    @Test
    void equalCoordinatesHaveValueEquality() {
        Location first = location(2, 3);
        Location sameValue = location(2, 3);

        assertThat(first).isEqualTo(sameValue).hasSameHashCodeAs(sameValue);
        assertThat(first.compareTo(sameValue)).isZero();
        assertThat(first).isNotEqualTo(location(3, 2));
        assertThat(first).isNotEqualTo(location(2, 4));
    }

    @Test
    void distanceCountsHorizontalAndVerticalSteps() {
        Location courier = location(2, 3);
        Location order = location(4, 6);

        assertThat(courier.distanceTo(order)).isEqualTo(5);
        assertThat(order.distanceTo(courier)).isEqualTo(5);
        assertThat(courier.distanceTo(courier)).isZero();
        assertThat(location(1, 1).distanceTo(location(10, 10))).isEqualTo(18);
    }

    @Test
    void distanceRequiresAnotherLocation() {
        assertThatNullPointerException().isThrownBy(() -> location(1, 1).distanceTo(null))
                .withMessage("other location");
    }

    private static Location location(int x, int y) {
        return Location.create(x, y).getValue();
    }

    private static void assertInvalid(int x, int y, String coordinate) {
        Result<Location, Error> result = Location.create(x, y);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.out.of.range");
        assertThat(result.getError().getMessage()).contains(coordinate);
    }
}
