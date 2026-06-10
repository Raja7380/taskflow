package com.taskflow.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 *
 * Instead of returning a raw List (which could be 50,000 items),
 * we return a page of results plus metadata about the full dataset.
 *
 * WHAT THE CLIENT RECEIVES:
 * {
 *   "content": [ {...}, {...}, {...} ],   ← the actual data (e.g., 20 tasks)
 *   "page": 0,                            ← current page number (0-indexed)
 *   "size": 20,                           ← items per page
 *   "totalElements": 157,                 ← total matching records in DB
 *   "totalPages": 8,                      ← ceil(157 / 20)
 *   "first": true,                        ← is this the first page?
 *   "last": false                         ← is this the last page?
 * }
 *
 * The <T> generic means this works for ANY type:
 *   PagedResponse<TaskResponse>
 *   PagedResponse<ProjectResponse>
 *   PagedResponse<UserResponse>
 */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    /**
     * Factory method — converts Spring's Page<T> into our PagedResponse<T>.
     *
     * Spring's Page<T> has all this data but is a framework type —
     * we don't want to expose framework types directly to the client.
     * This DTO gives us control over the exact JSON shape.
     */
    public static <T> PagedResponse<T> from(Page<T> springPage) {
        return PagedResponse.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .first(springPage.isFirst())
                .last(springPage.isLast())
                .build();
    }
}
