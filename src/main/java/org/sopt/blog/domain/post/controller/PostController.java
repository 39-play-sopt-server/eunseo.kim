package org.sopt.blog.domain.post.controller;

import org.sopt.blog.client.PostView;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.service.PostService;
import org.sopt.blog.global.common.exception.PostNotFoundException;

import java.util.List;


public class PostController {
    private final PostView view;
    private final PostService postService;

    // 생성자
    public PostController(PostView view, PostService postService) {
        this.view = view;
        this.postService = postService;
    }

    public void run() {
        while (true) {
            view.printMenu();
            int command = view.readCommand();
            switch (command) {
                case 1 -> createPost();
                case 2 -> readPosts();
                case 3 -> readPost();
                case 4 -> updatePost();
                case 5 -> deletePost();
                case 6 -> {
                    view.printMessage("프로그램을 종료합니다.");
                    return;
                }
                default -> view.printMessage("잘못된 입력입니다.");
            }
        }
    }

    private void createPost() {
        String title = view.readTitle();
        String content = view.readContent();
        Category category = view.readCategory();
        String author = view.readAuthor();
        try {
            postService.createPost(title, content, category, author);
            view.printMessage("게시글이 작성되었습니다.");
        } catch (IllegalArgumentException e) {
            view.printMessage(e.getMessage());
        }

    }

    private void readPosts() {
        List<Post> posts = postService.getAllPosts();
        if (posts.isEmpty()) {
            view.printMessage("게시글이 없습니다.");
            return;
        }
        for (int i = 0; i < posts.size(); i++) {
            view.printMessage((i + 1) + ". " + posts.get(i).getTitle());
        }
    }

    private void readPost() {
        int index = view.readPostNumber("조회할 게시글 번호: ") - 1;
        try {
            Post post = postService.getPost(index);
            view.printPost(post);
        } catch (PostNotFoundException e) {
            view.printMessage(e.getMessage());
        }
    }

    private void updatePost() {
        int index = view.readPostNumber("수정할 게시글 번호: ") - 1;
        String title = view.readTitle();
        String content = view.readContent();
        try {
            postService.updatePost(index, title, content);
            view.printMessage("게시글이 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            view.printMessage(e.getMessage());
        } catch (PostNotFoundException e) {
            view.printMessage(e.getMessage());
        }
    }

    private void deletePost() {
        int index = view.readPostNumber("삭제할 게시글 번호: ") - 1;
        try {
            postService.deletePost(index);
            view.printMessage("게시글이 삭제되었습니다.");
        } catch (PostNotFoundException e) {
            view.printMessage(e.getMessage());
        }
    }
}