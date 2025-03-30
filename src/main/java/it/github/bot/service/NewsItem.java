package it.github.bot.service;

public class NewsItem {
    private final String title;
    private final String description;
    private final String author;
    private final String url;

    public NewsItem(String title, String description, String author, String url) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.url = url;
    }


    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
