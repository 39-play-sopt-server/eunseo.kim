package org.sopt.blog.client;

import org.sopt.blog.domain.post.controller.PostController;
import org.sopt.blog.domain.post.repository.PostRepository;
import org.sopt.blog.domain.post.service.PostService;

public class Main {
    public static void main(String[] args) {
        PostView postView = new PostView();
        PostRepository postRepository = new PostRepository();
        PostService postService = new PostService(postRepository);
        PostController postController = new PostController(postView, postService);
        postController.run();
    }
}