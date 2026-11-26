package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.entity.RefreshTokenStatus;
import org.sopt.blog.domain.auth.repository.RefreshTokenRepository;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 과거_토큰_재사용과_최신_토큰_재발급이_경쟁해도_family에_활성_토큰이_남지_않는다() throws Exception {
        Member member = memberRepository.saveAndFlush(new Member(
                "동시성 회원",
                "refresh-race@example.com",
                passwordEncoder.encode("password")
        ));
        TokenPair first = tokenService.issueTokenPair(member);
        TokenPair current = tokenService.reissue(first.refreshToken());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> replayResult = executor.submit(() -> {
                start.await();
                return reissueSucceeds(first.refreshToken());
            });
            Future<Boolean> currentResult = executor.submit(() -> {
                start.await();
                return reissueSucceeds(current.refreshToken());
            });

            start.countDown();

            assertFalse(replayResult.get(), "이미 회전된 Refresh Token은 항상 거절되어야 합니다.");
            currentResult.get();
            assertTrue(
                    refreshTokenRepository.findAllByMember_IdAndStatus(
                            member.getId(),
                            RefreshTokenStatus.ACTIVE
                    ).isEmpty(),
                    "재사용 탐지 후에는 같은 family에 활성 Refresh Token이 남아서는 안 됩니다."
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean reissueSucceeds(String refreshToken) {
        try {
            tokenService.reissue(refreshToken);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
