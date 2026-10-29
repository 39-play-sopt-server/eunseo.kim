package org.sopt.blog.domain.like.entity;

import jakarta.persistence.*;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.global.common.entity.BaseEntity;

@Entity
@Table(
        name = "likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_like_post_member",
                columnNames = {"post_id", "member_id"}
        )
)
public class Like extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected Like() {}

    public Like(Post post, Member member) {
        if (post == null) throw new IllegalArgumentException("게시글은 필수입니다.");
        if (member == null) throw new IllegalArgumentException("회원은 필수입니다.");
        this.post = post;
        this.member = member;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public Member getMember() { return member; }
}
