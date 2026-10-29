package org.sopt.blog.domain.post.entity;

import jakarta.persistence.*;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.global.common.entity.BaseEntity;

@Entity
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    private int viewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected Post() {

    }

    public Post(String title, String content, Category category, Member member) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }
        if (member == null) {
            throw new IllegalArgumentException("작성자는 필수입니다.");
        }

        this.title = title;
        this.content = content;
        this.category = category;
        this.member = member;
        this.viewCount = 0;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public Member getMember() {
        return member;
    }

    public void updateTitle(String title) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        this.title = title;
    }

    public void updateContent(String content) {
        if (content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }
        this.content = content;
    }
}