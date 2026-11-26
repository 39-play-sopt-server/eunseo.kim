package org.sopt.blog.global.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenHashProviderTest {

    private final TokenHashProvider tokenHashProvider = new TokenHashProvider();

    @Test
    void 토큰을_SHA_256_해시값으로_변환한다() {
        String hash = tokenHashProvider.hash("abc");

        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hash
        );
    }

    @Test
    void 빈_토큰은_해시할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> tokenHashProvider.hash(" "));
    }
}
