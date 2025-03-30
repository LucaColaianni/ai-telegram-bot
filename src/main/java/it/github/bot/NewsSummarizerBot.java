package it.github.bot;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class NewsSummarizerBot extends TelegramLongPollingBot {

    private static final Dotenv dotEnv = Dotenv.configure().load();

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            User user = msg.getFrom();
            Long chatId = msg.getChatId();
            String messageText = msg.getText();

            System.out.println(user.getFirstName() + ": " + messageText);

            sendText(chatId, messageText);
        }
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage
                .builder()
                .chatId(who.toString())
                .text(what)
                .build();

        try {
            execute(sm);
        } catch (TelegramApiException e) {
            System.out.println("Errore nell'invio del messaggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
