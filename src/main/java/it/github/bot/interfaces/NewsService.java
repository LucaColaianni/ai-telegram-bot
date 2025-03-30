package it.github.bot.interfaces;

import it.github.bot.exception.NewsServiceException;
import it.github.bot.service.NewsCategory;
import it.github.bot.service.NewsItem;

import java.util.List;

public interface NewsService {

    List<NewsItem> getNewsByCategory(NewsCategory category, int limit) throws NewsServiceException;
    List<NewsItem> getRandomNews(int limit) throws NewsServiceException;
    List<String> formatNewsItems(List<NewsItem> news, String category);
}
