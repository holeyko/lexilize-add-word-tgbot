import bot.Bot;
import help.Config;
import help.FilesController;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi TGBotsAPI = new TelegramBotsApi(DefaultBotSession.class);
            TGBotsAPI.registerBot(new Bot(
                    Config.BOT_NAME,
                    System.getenv("BOT_TOKEN"),
                    new FilesController(Config.PATH_TO_RESOURCES)
            ));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
