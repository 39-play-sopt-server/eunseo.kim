package org.sopt.blog.domain.auth.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthProviderTest {

    @Test
    void URL의_소문자_provider를_enum으로_변환한다() {
        assertEquals(OAuthProvider.KAKAO, OAuthProvider.from("kakao"));
    }

    @Test
    void 지원하지_않는_provider는_거절한다() {
        assertThrows(IllegalArgumentException.class, () -> OAuthProvider.from("unknown"));
    }
}
