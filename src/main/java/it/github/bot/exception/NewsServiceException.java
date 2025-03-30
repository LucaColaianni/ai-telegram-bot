package it.github.bot.exception;

public class NewsServiceException extends Exception{
    public NewsServiceException(String message) {
        super(message);
    }

    public NewsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
