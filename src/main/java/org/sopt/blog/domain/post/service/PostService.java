package org.sopt.blog.domain.post.service;

import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.repository.PostRepository;
import org.sopt.blog.global.common.exception.PostNotFoundException;

import java.util.List;

public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < postRepository.size();
    }

    public void createPost(String title, String content, Category category, String author) {
        postRepository.save(new Post(title, content, category, author));
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Post getPost(int index) {
        if (!isValidIndex(index)) {
            throw new PostNotFoundException();
        }
        return postRepository.findByIndex(index);
    }

    public void updatePost(int index, String title, String content){
        Post post = getPost(index);
        post.updateTitle(title);
        post.updateContent(content);
    }

    public void deletePost(int index) {
       getPost(index);
       postRepository.deleteByIndex(index);
    }
}
