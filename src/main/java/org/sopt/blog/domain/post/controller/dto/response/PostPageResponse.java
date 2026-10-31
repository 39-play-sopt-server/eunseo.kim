package org.sopt.blog.domain.post.controller.dto.response;

import java.util.List;

public record PostPageResponse(
        List<PostSummaryResponse> posts,
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean hasNext
) {
    public static PostPageResponse of(List<PostSummaryResponse> posts, int page, int size, long totalCount) {
        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean hasNext = page < totalPages - 1;
        return new PostPageResponse(posts, page, size, totalCount, totalPages, hasNext);
    }
}