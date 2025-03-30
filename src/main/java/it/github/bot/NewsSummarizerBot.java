package it.github.bot;

import io.github.cdimascio.dotenv.Dotenv;
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
    private static final Dotenv dotEnv = Dotenv.configure().load();


    private static final String[] RANDOM_NEWS = {
            "Gli scienziati hanno scoperto un nuovo pianeta abitabile a soli 40 anni luce dalla Terra.",
            "Un'importante azienda tecnologica ha annunciato una rivoluzionaria batteria che dura una settimana.",
            "Un nuovo studio dimostra che dormire 8 ore a notte può ridurre il rischio di malattie cardiache del 30%.",
            "Un team italiano ha vinto il campionato mondiale di robotica con un robot in grado di risolvere problemi complessi.",
            "Una startup ha creato un sistema che può convertire la plastica in carburante con un'efficienza del 90%."
    };

    private static final String POLITICS_NEWS = "Ecco le ultime notizie di politica:\n\n" +
            "- Il governo ha approvato un nuovo piano per le infrastrutture\n" +
            "- Incontro al vertice tra i leader dei principali partiti\n" +
            "- Nuove misure fiscali in discussione al parlamento";

    private static final String TECH_NEWS = "Ecco le ultime notizie tecnologiche:\n\n" +
            "- Apple ha presentato il nuovo iPhone\n" +
            "- Meta annuncia progressi significativi nell'IA generativa\n" +
            "- Breakthrough nella tecnologia delle batterie a stato solido";

    private static final String FINANCE_NEWS = "Ecco le ultime notizie finanziarie:\n\n" +
            "- La borsa ha registrato un aumento del 2% questa settimana\n" +
            "- La BCE mantiene i tassi di interesse invariati\n" +
            "- Bitcoin supera quota 60.000 dollari";

    private static final String DEFAULT_RESPONSE = "Non ho capito. Ecco i comandi disponibili:";

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

        if (COMMAND_START.equals(messageText)) {
            sendWelcomeMessage(chatId, message.getFrom().getFirstName());
        } else if (messageText.contains(CATEGORY_POLITICS)) {
            sendText(chatId, POLITICS_NEWS);
        } else if (messageText.contains(CATEGORY_TECH)) {
            sendText(chatId, TECH_NEWS);
        } else if (messageText.contains(CATEGORY_FINANCE)) {
            sendText(chatId, FINANCE_NEWS);
        } else if (messageText.contains(CATEGORY_RANDOM)) {
            sendRandomNews(chatId);
        } else {
            sendKeyboard(chatId, DEFAULT_RESPONSE);
        }
    }

    private boolean isValidMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }
    private void sendWelcomeMessage(Long chatId, String firstName) {
        String welcomeText = String.format(
                "Ciao %s! 👋\n\n" +
                        "Benvenuto nel News Summarizer Bot. Questo bot ti permette di ricevere aggiornamenti sulle ultime notizie in diverse categorie.\n\n" +
                        "Come funziona:\n" +
                        "- Usa i bottoni qui sotto per selezionare la categoria di notizie che ti interessa\n" +
                        "- Riceverai un sommario delle ultime notizie in quella categoria\n" +
                        "- Con 'Notizia Casuale' riceverai una notizia selezionata casualmente\n\n" +
                        "Seleziona una categoria per iniziare:", firstName);

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
    private void sendRandomNews(Long chatId) {
        Random random = new Random();
        int index = random.nextInt(RANDOM_NEWS.length);
        sendText(chatId, "📰 Notizia casuale:\n\n" + RANDOM_NEWS[index]);
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
