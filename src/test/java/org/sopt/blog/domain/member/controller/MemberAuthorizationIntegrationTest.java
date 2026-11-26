package org.sopt.blog.domain.member.controller;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.global.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 인증_없이_전체_회원_목록을_조회하면_401을_응답한다() throws Exception {
        mockMvc.perform(get("/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증_없이_Swagger_UI를_조회할_수_있다() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void USER_권한은_전체_회원_목록을_조회하면_403을_응답한다() throws Exception {
        Member member = saveMember("user@example.com");

        mockMvc.perform(get("/members")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ADMIN_권한은_전체_회원_목록을_조회하면_200을_응답한다() throws Exception {
        Member admin = saveMember("admin@example.com");
        admin.grantAdminRole();
        memberRepository.saveAndFlush(admin);

        mockMvc.perform(get("/members")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private Member saveMember(String email) {
        Member member = new Member(
                "테스트 회원",
                email,
                passwordEncoder.encode("password1234")
        );
        return memberRepository.saveAndFlush(member);
    }

    private String bearerToken(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId(), member.getRole());
    }
}
