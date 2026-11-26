package org.sopt.blog.global.config;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties adminBootstrapProperties;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        String adminEmail = adminBootstrapProperties.getEmail();

        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }

        memberRepository.findByEmail(adminEmail)
                .filter(member -> !member.isDeleted())
                .ifPresent(member -> {
                    member.grantAdminRole();
                    memberRepository.save(member);
                });
    }
}
