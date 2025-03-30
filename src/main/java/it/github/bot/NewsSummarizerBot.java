package it.github.bot;

import io.github.cdimascio.dotenv.Dotenv;
import it.github.bot.exception.NewsServiceException;
import it.github.bot.interfaces.NewsService;
import it.github.bot.service.NewsApiService;
import it.github.bot.service.NewsCategory;
import it.github.bot.service.NewsItem;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NewsSummarizerBot extends TelegramLongPollingBot {

    private static final String COMMAND_START = "/start";
    private static final String CATEGORY_POLITICS = "Notizie Politiche";
    private static final String CATEGORY_TECH = "Notizie Tech";
    private static final String CATEGORY_FINANCE = "Notizie Finanza";
    private static final String CATEGORY_RANDOM = "Notizia Casuale";
    private static final String DEFAULT_RESPONSE = "Non ho capito. Ecco i comandi disponibili:";

    private final Dotenv dotEnv;
    private final Random random;
    private final NewsService newsService;
    public NewsSummarizerBot() {
        this.dotEnv = Dotenv.configure().load();
        this.random = new Random();
        this.newsService = new NewsApiService(dotEnv.get("NEWS_API_KEY"));
    }

    public static void main(String[] args) throws TelegramApiException {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            NewsSummarizerBot bot = new NewsSummarizerBot();
            botsApi.registerBot(bot);
            System.out.println("Bot avviato con successo!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.out.println("Errore durante l'avvio del bot: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return dotEnv.get("BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return dotEnv.get("BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!isValidMessage(update)) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String messageText = message.getText();

        try {
            if (COMMAND_START.equals(messageText)) {
                sendWelcomeMessage(chatId, message.getFrom().getFirstName());
            } else if (messageText.contains(CATEGORY_POLITICS)) {
                sendCategoryNews(chatId, NewsCategory.POLITICA);
            } else if (messageText.contains(CATEGORY_TECH)) {
                sendCategoryNews(chatId, NewsCategory.TECHNOLOGY);
            } else if (messageText.contains(CATEGORY_FINANCE)) {
                sendCategoryNews(chatId, NewsCategory.BUSINESS);
            } else if (messageText.contains(CATEGORY_RANDOM)) {
                sendRandomNews(chatId);
            } else {
                sendKeyboard(chatId, DEFAULT_RESPONSE);
            }
        } catch (NewsServiceException e) {
            sendText(chatId, "Mi dispiace, c'è stato un problema nel recuperare le notizie. Riprova più tardi.");
            System.err.println("Errore nel servizio notizie: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isValidMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }
    private void sendWelcomeMessage(Long chatId, String firstName) {

        String welcomeText = String.format("""
            Ciao %s! 👋
                    Benvenuto nel News Summarizer Bot. Questo bot ti permette di ricevere aggiornamenti sulle ultime notizie in diverse categorie.
                    Come funziona:
                    - Usa i bottoni qui sotto per selezionare la categoria di notizie che ti interessa
                    - Riceverai un sommario delle ultime notizie in quella categoria
                    - Con 'Notizia Casuale' riceverai una notizia selezionata casualmente
                    Seleziona una categoria per iniziare:
                     """, firstName);

        sendKeyboard(chatId, welcomeText);
    }

    private void sendKeyboard(Long chatId, String text) {
        SendMessage message = createBasicMessage(chatId, text);
        message.setReplyMarkup(createNewsKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Errore nell'invio della tastiera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createNewsKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // First row
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📊 " + CATEGORY_POLITICS));
        row1.add(new KeyboardButton("💻 " + CATEGORY_TECH));
        keyboard.add(row1);

        // Second row
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("💰 " + CATEGORY_FINANCE));
        row2.add(new KeyboardButton("🎲 " + CATEGORY_RANDOM));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
    private void sendCategoryNews(Long chatId, NewsCategory category) throws NewsServiceException {
        int newsLimitForMessage = 3;
        List<NewsItem> news = newsService.getNewsByCategory(category, newsLimitForMessage);
        String formattedNews = formatNewsItems(news, getCategoryDisplayName(category));
        sendText(chatId, formattedNews);
    }
    private String getCategoryDisplayName(NewsCategory category) {
        switch (category) {
            case POLITICA:
                return "politica";
            case TECHNOLOGY:
                return "tecnologia";
            case BUSINESS:
                return "finanza";
            default:
                return "generali";
        }
    }
    private String formatNewsItems(List<NewsItem> news, String category) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Ecco le ultime notizie di %s:\n\n", category));

        for (NewsItem item : news) {
            builder.append("- ")
                    .append(item.getTitle())
                    .append("\n");

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                builder.append("  ")
                        .append(item.getDescription())
                        .append("\n");
            }

            builder.append("\n");
        }

        return builder.toString();
    }
    private void sendRandomNews(Long chatId) throws NewsServiceException {
        List<NewsItem> news = newsService.getRandomNews(1);
        if (!news.isEmpty()) {
            NewsItem randomNews = news.get(0);
            String formattedNews = String.format(
                    """
                    📰 Notizia casuale: %s %s""",
                    randomNews.getTitle(),
                    randomNews.getDescription() != null ? randomNews.getDescription() : ""
            );
            sendText(chatId, formattedNews);
        } else {
            sendText(chatId, "Mi dispiace, non sono riuscito a trovare notizie casuali al momento.");
        }
    }


    private void sendText(Long chatId, String text) {
        SendMessage message = createBasicMessage(chatId, text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Errore nell'invio del messaggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private SendMessage createBasicMessage(Long chatId, String text) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
    }
}
