package org.sopt.blog.domain.post.entity;

public class Post {

    private String title;
    private String content;
    private Category category;
    private String author;

    public Post(String title, String content, Category category, String author) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }

        this.title = title;
        this.content = content;
        this.category = category;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Category getCategory() {
        return category;
    }

    public String getAuthor() {
        return author;
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