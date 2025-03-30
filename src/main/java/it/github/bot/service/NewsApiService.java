package it.github.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.github.bot.exception.NewsServiceException;
import it.github.bot.interfaces.NewsService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

public class NewsApiService implements NewsService {

    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public NewsApiService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<NewsItem> getNewsByCategory(NewsCategory category, int limit) throws NewsServiceException {
        return callEndpoint(category.name().toLowerCase(), limit);
    }

    @Override
    public List<NewsItem> getRandomNews(int limit) throws NewsServiceException {
        NewsCategory[] categories = {NewsCategory.TECHNOLOGY, NewsCategory.BUSINESS, NewsCategory.POLITICA};
        NewsCategory randomCategory = categories[(int) (Math.random() * categories.length)];
        return callEndpoint(randomCategory.name().toLowerCase(), limit);
    }

    private List<NewsItem> callEndpoint(String category, int limit) throws NewsServiceException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(NEWS_API_URL).newBuilder();
        urlBuilder.addQueryParameter("language", "it");
        urlBuilder.addQueryParameter("from", "2025-03-29");
        urlBuilder.addQueryParameter("to", "2025-03-29");
        urlBuilder.addQueryParameter("sortBy", "popularity");
        urlBuilder.addQueryParameter("q", category);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("x-api-key", apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new NewsServiceException("Errore nella chiamata API: " + response.message());
            }

            String jsonResponse = response.body().string();
            return fetchNewsFromJson(jsonResponse, limit);

        } catch (Exception e) {
            throw new NewsServiceException("Errore durante il recupero delle notizie: " + e.getMessage(), e);
        }
    }

    private List<NewsItem> fetchNewsFromJson(String jsonResponse, int limit) throws NewsServiceException {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode articles = root.get("articles");
            List<NewsItem> newsList = new ArrayList<>();

            for (int i = 0; i < Math.min(limit, articles.size()); i++) {
                JsonNode item = articles.get(i);
                String title = item.get("title").asText();
                String description = item.has("description") && !item.get("description").isNull() ?
                        item.get("description").asText() :
                        "Nessuna descrizione disponibile.";
                String author = item.has("author") && !item.get("author").isNull() ?
                        item.get("author").asText() : null;
                String url = item.has("url") && !item.get("url").isNull() ?
                        item.get("url").asText() : null;

                newsList.add(new NewsItem(title, description, author, url));
            }

            return newsList;
        } catch (Exception e) {
            throw new NewsServiceException("Errore durante l'elaborazione del JSON: " + e.getMessage(), e);
        }
    }
}

