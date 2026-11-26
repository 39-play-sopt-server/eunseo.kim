package org.sopt.blog.global.config;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.entity.MemberRole;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.boot.ApplicationArguments;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapRunnerTest {

    private final AdminBootstrapProperties adminBootstrapProperties = new AdminBootstrapProperties();
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AdminBootstrapRunner adminBootstrapRunner =
            new AdminBootstrapRunner(adminBootstrapProperties, memberRepository);

    @Test
    void 설정된_활성_회원을_ADMIN으로_승격한다() throws Exception {
        Member member = new Member("관리자", "admin@example.com", "encoded-password");
        adminBootstrapProperties.setEmail("admin@example.com");
        when(memberRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(member));

        adminBootstrapRunner.run(mock(ApplicationArguments.class));

        assertEquals(MemberRole.ROLE_ADMIN, member.getRole());
        verify(memberRepository).save(member);
    }

    @Test
    void 관리자_이메일이_없으면_아무_회원도_변경하지_않는다() throws Exception {
        adminBootstrapRunner.run(mock(ApplicationArguments.class));

        verify(memberRepository, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }
}
