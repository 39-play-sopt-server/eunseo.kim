package org.sopt.blog.domain.auth.service;

import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.domain.member.service.MemberService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MemberService memberService;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            MemberService memberService
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.memberService = memberService;
    }

    @Transactional
    public Long signup(String name, String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        Member member = new Member(name, email, passwordEncoder.encode(password));
        return memberRepository.save(member).getId();
    }

    @Transactional
    public TokenPair login(String email, String password){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.isDeleted()
                || !passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return tokenService.issueTokenPair(member);
    }

    @Transactional
    public void withdraw(
            Long memberId,
            String accessToken,
            String refreshToken
    ) {
        memberService.deleteMember(memberId);
        tokenService.logout(accessToken, refreshToken);
        tokenService.revokeAllRefreshTokens(memberId);
    }
}
