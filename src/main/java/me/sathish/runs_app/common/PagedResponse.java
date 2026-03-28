package me.sathish.runs_app.common;

import java.util.List;
import org.springframework.data.domain.Page;


public class PagedResponse<T> {

    private List<T> content;
    private PageMetadata page;

    public PagedResponse(Page<T> springPage) {
        this.content = springPage.getContent();
        this.page = new PageMetadata(
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.getNumber(),
                springPage.getSize()
        );
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public PageMetadata getPage() {
        return page;
    }

    public void setPage(PageMetadata page) {
        this.page = page;
    }

    public static class PageMetadata {
        private long totalElements;
        private int totalPages;
        private int number;
        private int size;

        public PageMetadata(long totalElements, int totalPages, int number, int size) {
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.number = number;
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
