package bot;

import help.FilesController;
import jxl.write.WriteException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import sheets.FileIsNotExcelException;
import sheets.SheetsControler;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private final String name;
    private final String token;
    private final FilesController filesController;
    private final SheetsControler sheetsControler = new SheetsControler();
    private Map<String, ReplyKeyboardMarkup> preparedKeyboardMarkups;
    private RequestContextHistory contextHistory;

    private final List<String> COMMANDS = List.of("/start", "/help");
    public static final int HISTORY_LENGTH = 4;
    public static final int COUNT_BUTTONS_IN_ROW = 2;

    public Bot(String name, String token, FilesController filesController) {
        this.name = name;
        this.token = token;
        this.filesController = filesController;
        this.contextHistory = new RequestContextHistory(HISTORY_LENGTH);

        this.preparedKeyboardMarkups = new HashMap<>();

        {
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> rows = new ArrayList<>();
            KeyboardRow firstRow = new KeyboardRow();
            KeyboardRow secondRow = new KeyboardRow();
            
            firstRow.add(Buttons.CREATE_WORDS.innerText);
            secondRow.add(Buttons.CREATE_PACK.innerText);
            secondRow.add(Buttons.EXPORT_PACK.innerText);
            rows.add(firstRow);
            rows.add(secondRow);
            keyboardMarkup.setKeyboard(rows);
            keyboardMarkup.setResizeKeyboard(true);
            preparedKeyboardMarkups.put("main", keyboardMarkup);
        }

        {
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> rows = new ArrayList<>();
            KeyboardRow row = new KeyboardRow();

            row.add(Buttons.MAIN_MENU.innerText);
            rows.add(row);
            keyboardMarkup.setKeyboard(rows);
            keyboardMarkup.setResizeKeyboard(true);
            preparedKeyboardMarkups.put("return-to-main", keyboardMarkup);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            String messageText = message.hasText() ? message.getText() : "";
            Long chatId = message.getChatId();

            filesController.addFolderIfNotExists(Long.toString(chatId));

            SendMessage returnMessage = new SendMessage();
            String returnText = "I don't understand you\nWrite /help command";
            UserState stateAfterRequest = UserState.NOTHING;
            ReplyKeyboardMarkup returnKeyboardMarkup = preparedKeyboardMarkups.get("main");

            if (COMMANDS.contains(messageText)) {
                switch (messageText) {
                    case "/start" -> returnText = "start"; //TODO: write start message
                    case "/help" -> returnText = "help"; //TODO: write help message
                    default -> {
                        break; //TODO: write err log
                    }
                }
            } else if (messageText.equals(Buttons.MAIN_MENU.innerText)) {
                returnText = "main menu"; //TODO: write main menu message
            } else if (messageText.equals(Buttons.CREATE_PACK.innerText)) {
                returnText = "write pack's name"; //TODO: write pack's name message
                returnKeyboardMarkup = preparedKeyboardMarkups.get("return-to-main");
                stateAfterRequest = UserState.WRITING_PACK_NAME;
            } else if (messageText.equals(Buttons.EXPORT_PACK) || messageText.equals(Buttons.CREATE_WORDS)) {
                List<KeyboardRow> rows = keyboardRowsChoosingPack(chatId);
                if (rows.isEmpty()) {
                    returnText = "you haven't any pack"; //TODO: write a message
                } else {
                    KeyboardRow backToMainMenu = new KeyboardRow();
                    backToMainMenu.add(Buttons.MAIN_MENU.innerText);
                    rows.add(backToMainMenu);

                    returnKeyboardMarkup = new ReplyKeyboardMarkup(rows);
                    returnText = "choose a pack"; //TODO: write a message
                    stateAfterRequest = UserState.CHOOSING_PACK;
                }
            } else {
                if (contextHistory.sizeOfHistory > 0) {
                    RequestContext prevRequest = contextHistory.getContext(chatId, 0);
                    UserState prevUserState = prevRequest.getState();
                    Message prevMessage = prevRequest.getUpdate().getMessage();
                    String prevText = prevMessage.getText();

                    if (prevUserState == UserState.WRITING_PACK_NAME) {
                        try {
                            filesController.createFile(getPathToFile(Long.toString(chatId), messageText + ".xlsx"));
                            returnText = "successful"; //TODO: write a message
                        } catch (IOException e) {
                            returnText = "incorrect pack's name format"; //TODO: write a message
                        }
                    } else if (prevUserState == UserState.CHOOSING_PACK) {
                        if (prevText.equals(Buttons.EXPORT_PACK)) {
                            try {
                                File excel = filesController.openFile(getPathToFile(Long.toString(chatId), messageText + ".xlsx"));
                                SendDocument returnExcel = new SendDocument(Long.toString(chatId), new InputFile(excel));
                                execute(returnExcel);
                                return;
                            } catch (IllegalArgumentException e) {
                                throw new RuntimeException(e); //TODO: send err log
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e); //TODO: send err log
                            }
                        } else if (prevText.equals(Buttons.CREATE_WORDS)) {
                            returnText = "write words as \"word | phrase : translation\" without quotes"; //TODO: write a message
                            stateAfterRequest = UserState.WRITING_WORDS;
                            returnKeyboardMarkup = preparedKeyboardMarkups.get("return-to-main");
                        }
                    } else if (prevUserState == UserState.WRITING_WORDS) {
                        try {
                            Scanner scan = new Scanner(messageText);
                            StringBuilder sb = new StringBuilder(); //TODO: write a message
                            int curRow = sheetsControler.countWrittenRows(
                                    getPathToFile(Long.toString(chatId), messageText + ".xlsx"),
                                    "Lexillize"
                            );
                            sheetsControler.openToWrite(
                                    getPathToFile(Long.toString(chatId), messageText + ".xlsx"),
                                    "Lexillize"
                            );
                            while (scan.hasNextLine()) {
                                String line = scan.nextLine();
                                String[] parsedLine = line.split(":");
                                if (parsedLine.length != 2) {
                                    if (sb.isEmpty()) {
                                        sb.append("Incorrect format:\n");
                                    }
                                    sb.append("\t" + line + "\n");
                                } else {
                                    String word = parsedLine[0].trim();
                                    String translation = parsedLine[1].trim();
                                    if (word.isBlank() || translation.isBlank()) {
                                        sb.append("\t" + line + "\n");
                                    } else {
                                        sheetsControler.write("Lexillize", curRow, 0, word);
                                        sheetsControler.write("Lexillize", curRow, 1, translation);
                                        ++curRow;
                                    }
                                }
                            }

                            if (sb.isEmpty()) {
                                returnText = "add words were successful added"; //TODO: write a message
                            } else {
                                sb.append("other words were successful added");
                                returnText = sb.toString();
                            }
                        } catch (FileIsNotExcelException e) {
                            throw new RuntimeException(e); //TODO: write err log
                        } catch (WriteException e) {
                            throw new RuntimeException(e); //TODO: write err log
                        } catch (IOException e) {
                            throw new RuntimeException(e); //TODO: write err log
                        }
                    }
                }
            }

            returnMessage.setText(returnText);
            returnMessage.setReplyMarkup(returnKeyboardMarkup);
            try {
                execute(returnMessage);
            } catch (TelegramApiException e) {
                //TODO: send a err log
            }
        } else {
            //TODO: send a err log
        }
    }

    private String getPathToFile(String pathToFolder, String fileName) {
        return new File(new File(pathToFolder), fileName).getPath();
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    private List<KeyboardRow> keyboardRowsChoosingPack(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for (String packName : filesController.getAllNamesFileSameType(Long.toString(chatId), ".xlsx")) {
            row.add(packName);
            if (row.size() >= COUNT_BUTTONS_IN_ROW) {
                rows.add(row);
                row = new KeyboardRow();
            }
        }

        return rows;
    }

    private enum Buttons {
        MAIN_MENU("Main Menu"),
        CREATE_PACK("Create Pack"),
        REMOVE_PACK("Remove Pack"),
        SHOW_PACKS("Show Packs"),
        CREATE_WORDS("Add Words"),
        EXPORT_PACK("Export Pack");

        private String innerText;

        Buttons(String innerText) {
            this.innerText = innerText;
        }
    }

    private enum UserState {
        NOTHING,
        WRITING_PACK_NAME, REMOVING_PACK, CHOOSING_PACK, SEND_PACK,
        CREATING_WORDS, WRITING_WORDS
    }

    private class RequestContext {
        private final Update update;
        private final UserState state;

        public RequestContext(Update update, UserState state) {
            this.update = update;
            this.state = state;
        }

        public Update getUpdate() {
            return this.update;
        }

        public UserState getState() {
            return this.state;
        }
    }

    private class RequestContextHistory implements Serializable {
        private Map<Long, LinkedList<RequestContext>> userContextHistory = new HashMap<>();
        private final int sizeOfHistory;

        public RequestContextHistory(int sizeOfHistory) {
            this.sizeOfHistory = sizeOfHistory;
        }

        public void addContext(Update update, UserState state) {
            Long chatId = update.getMessage().getChatId();
            LinkedList<RequestContext> history = userContextHistory.getOrDefault(chatId, new LinkedList<>());
            history.add(new RequestContext(update, state));

            if (history.size() > sizeOfHistory) {
                history.remove();
            }

            userContextHistory.put(chatId, history);
        }

        public RequestContext getContext(Long chatId, int number) throws IllegalArgumentException {
            LinkedList<RequestContext> history = userContextHistory.getOrDefault(chatId, new LinkedList<>());

            if (history.size() <= number) {
                throw new IllegalArgumentException(String.format("Size of history (%d) less then number (%d)", history.size(), number));
            }
            return history.get(history.size() - number - 1);
        }
    }
}
