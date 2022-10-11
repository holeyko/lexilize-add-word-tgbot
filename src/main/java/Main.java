import bot.Bot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import util.Config;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi TGBotsAPI = new TelegramBotsApi(DefaultBotSession.class);
            TGBotsAPI.registerBot(new Bot(
                    Config.BOT_NAME,
                    System.getenv("BOT_TOKEN")
            ));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
