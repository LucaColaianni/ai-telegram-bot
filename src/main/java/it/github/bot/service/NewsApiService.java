package it.github.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.github.bot.exception.NewsServiceException;
import it.github.bot.interfaces.NewsService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class NewsApiService implements NewsService {

    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private static final String DESCRIPTION_FIELD = "description";
    private static final String AUTHOR_FIELD = "author";
    private static final String URL_FIELD = "url";


    public NewsApiService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<NewsItem> getNewsByCategory(NewsCategory category, int limit) throws NewsServiceException {
        if("RANDOM".equalsIgnoreCase(category.name())){
            return getRandomNews(limit);
        }else {
        return callEndpoint(category.name().toLowerCase(), limit);
        }
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
        urlBuilder.addQueryParameter("from", LocalDate.now().minusDays(1).toString());
        urlBuilder.addQueryParameter("to", LocalDate.now().minusDays(1).toString());
        urlBuilder.addQueryParameter("sortBy", "popularity");
        urlBuilder.addQueryParameter("q", category);


        log.debug("Endpoint -> [{}]", urlBuilder);
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

                String description = item.has(DESCRIPTION_FIELD) && !item.get(DESCRIPTION_FIELD).isNull() ?
                        item.get(DESCRIPTION_FIELD).asText() :
                        "Nessuna descrizione disponibile.";

                String author = item.has(AUTHOR_FIELD) && !item.get(AUTHOR_FIELD).isNull() ?
                        item.get(AUTHOR_FIELD).asText() : "Nome autore non disponibile.";

                String url = item.has(URL_FIELD) && !item.get(URL_FIELD).isNull() ?
                        item.get(URL_FIELD).asText() : "URL non disponibile.";

                newsList.add(new NewsItem(title, description, author, url));
            }

            return newsList;
        } catch (Exception e) {
            throw new NewsServiceException("Errore durante l'elaborazione del JSON: " + e.getMessage(), e);
        }
    }

    public List<String> formatNewsItems(List<NewsItem> news, String category) {

        List<String> newsFormatted = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Ecco le ultime notizie di %s:%n%n", category));

        for (NewsItem singleNews : news) {
            builder.append("TITOLO: ")
                    .append("\n")
                    .append(singleNews.getTitle())
                    .append("\n\n");

            if (singleNews.getDescription() != null && !singleNews.getDescription().isEmpty()) {
                builder.append("DESCRIZIONE: ")
                        .append("\n")
                        .append(singleNews.getDescription())
                        .append("\n\n");
            }

            if (singleNews.getAuthor() != null && !singleNews.getAuthor().isEmpty()) {
                builder.append("AUTORE: ")
                        .append("\n")
                        .append(singleNews.getAuthor())
                        .append("\n\n");
            }
            if (singleNews.getUrl() != null && !singleNews.getUrl().isEmpty()) {
                builder.append("URL: ")
                        .append("\n")
                        .append(singleNews.getUrl())
                        .append("\n\n");
            }

            newsFormatted.add(builder.toString());
            builder.delete(0, builder.length());

        }
        return newsFormatted;
    }
}

