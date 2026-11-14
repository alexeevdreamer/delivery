package microarch.delivery.core.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VolumeTest {

    @Test
    void requiresPositiveVolume() {
        assertThat(Volume.create(0).isFailure()).isTrue();
        assertThat(Volume.create(-1).isFailure()).isTrue();
        assertThat(Volume.create(1).getValue().getValue()).isEqualTo(1);
    }

    @Test
    void comparesByValue() {
        Volume first = Volume.create(3).getValue();
        Volume sameValue = Volume.create(3).getValue();

        assertThat(first).isEqualTo(sameValue).hasSameHashCodeAs(sameValue);
        assertThat(first).isNotEqualTo(Volume.create(4).getValue());
    }
}
