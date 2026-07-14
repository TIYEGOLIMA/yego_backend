package com.yego.backend.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YangoCookiePoolTest {

    @Test
    void rejectsUseWhenCookiesAreNotConfigured() {
        YangoCookiePool pool = new YangoCookiePool("");

        assertThatThrownBy(pool::randomCookie)
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
    void resetsInvalidCookiesWhenRandomCookieNeedsOne() {
        YangoCookiePool pool = new YangoCookiePool("cookie-a");
        pool.markInvalid(0);

        assertThat(pool.randomCookie()).isEqualTo("cookie-a");
    }
}
