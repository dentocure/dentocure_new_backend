package com.dentocure.dto;

/** Pagination metadata included in list responses. */
public class PageMeta {

    private final int page;
    private final int limit;
    private final long total;
    private final int totalPages;

    public PageMeta(int page, int limit, long total) {
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = limit > 0 ? (int) Math.ceil((double) total / limit) : 0;
    }

    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public long getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
}
