package com.yego.backend.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YangoCookiePoolTest {

    @Test
    void rejectsUseWhenCookiesAreNotConfigured() {
        YangoCookiePool pool = new YangoCookiePool("");

        assertThatThrownBy(pool::randomValidIndex)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("YEGO_YANGO_COOKIES");
    }

    @Test
    void parsesAndRotatesConfiguredCookiesWithoutExposingTheirContent() {
        YangoCookiePool pool = new YangoCookiePool("cookie-a|||cookie-b");

        assertThat(pool.size()).isEqualTo(2);
        int firstIndex = pool.randomValidIndex();
        pool.markInvalid(firstIndex);

        assertThat(pool.randomValidIndex()).isNotEqualTo(firstIndex);
    }

    @Test
    void prioritizesCookieConfiguredForRequestedPark() {
        YangoCookiePool pool = new YangoCookiePool(
                "Session_id=first; park_id=park-a|||Session_id=second; park_id=park-b");

        assertThat(pool.validIndicesForPark("park-b")).containsExactly(1, 0);
    }

    @Test
    void excludesCookieRejectedForRequestedParkOnly() {
        YangoCookiePool pool = new YangoCookiePool(
                "Session_id=first; park_id=park-a|||Session_id=second; park_id=park-b");

        pool.markUnauthorizedForPark(1, "park-b");

        assertThat(pool.validIndicesForPark("park-b")).containsExactly(0);
        assertThat(pool.validIndicesForPark("park-b-other")).containsExactly(0, 1);
    }
}
