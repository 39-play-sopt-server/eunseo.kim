package org.sopt.blog.domain.post.repository;

import org.sopt.blog.domain.post.entity.Post;

import java.util.ArrayList;
import java.util.List;

public class PostRepository {
    private final List<Post> posts = new ArrayList<>();

    public void save(Post post) {
        posts.add(post);
    }
    public List<Post> findAll() {
        return posts;
    }
    public Post findByIndex(int index) {
        return posts.get(index);
    }
    public void deleteByIndex(int index) {
        posts.remove(index);
    }
    public int size() {
        return posts.size();
    }
}

