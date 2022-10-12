package bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import sheets.SheetsController;
import util.Config;
import util.DataController;
import util.SimpleLog;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private final String name;
    private final String token;
    private final DataController dataController;
    private final SheetsController sheetsController = new SheetsController();
    private final Map<String, ReplyKeyboardMarkup> preparedKeyboardMarkups;
    private final RequestContextHistory contextHistory;

    private final List<String> COMMANDS = List.of("/start", "/help", "/main");
    private static final int HISTORY_LENGTH = 4;
    private static final int COUNT_BUTTONS_IN_ROW = 2;
    private final String SHEET_NAME = "Lexillize";

    public Bot(String name, String token) {
        this.name = name;
        this.token = token;
        this.dataController = new DataController(Config.RELATIVE_PATH_TO_DATA_FOLDER);
        this.contextHistory = new RequestContextHistory(HISTORY_LENGTH);

        this.preparedKeyboardMarkups = new HashMap<>();
        preparedKeyboardMarkups.put(
                "main",
                createReplyKeyboardMarkup(List.of(
                        List.of(Buttons.CREATE_WORDS.innerText),
                        List.of(Buttons.CREATE_PACK.innerText, Buttons.EXPORT_PACK.innerText),
                        List.of(Buttons.DELETE_PACK.innerText, Buttons.SHOW_PACKS.innerText)
                ), true)
        );
        preparedKeyboardMarkups.put(
                "return-to-main",
                createReplyKeyboardMarkup(List.of(
                        List.of(Buttons.MAIN_MENU.innerText)
                ), true)
        );
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            String messageText = message.hasText() ? message.getText() : "";
            Long chatId = message.getChatId();

            dataController.addFolderIfNotExists(Long.toString(chatId));

            SendMessage returnMessage = new SendMessage();
            String returnText = "I don't understand you\nWrite /help command";
            UserState stateAfterRequest = UserState.NOTHING;
            ReplyKeyboardMarkup returnKeyboardMarkup = preparedKeyboardMarkups.get("main");

            if (COMMANDS.contains(messageText)) {
                switch (messageText) {
                    case "/start", "/help" -> returnText = """
                            Hello, I'm Lexillize Bot
                            I will help you learn English
                            You can create packs, add words to packs, export packs to import its in Lexillize app and delete packs
                            """;
                    case "/main" -> returnText = "Main menu";
                    default -> {
                        SimpleLog.err("Unexpected command: " + messageText);
                    }
                }
            } else if (messageText.equals(Buttons.MAIN_MENU.innerText)) {
                returnText = "Main menu";
            } else if (messageText.equals(Buttons.CREATE_PACK.innerText)) {
                returnText = "Write a pack's name";
                returnKeyboardMarkup = preparedKeyboardMarkups.get("return-to-main");
                stateAfterRequest = UserState.WRITING_PACK_NAME;
            } else if (messageText.equals(Buttons.SHOW_PACKS.innerText)) {
                StringBuilder sb = new StringBuilder();

                for (File pack : dataController.getAllNamesFileSameType(Long.toString(chatId), Config.SHEET_FILE_FORMAT)) {
                    try {
                        sheetsController.openExcelFile(pack);
                        int countRows = sheetsController.getCountRowInSheet(SHEET_NAME);
                        sb.append("\t");
                        sb.append(removeFileExtension(pack.getName(), Config.SHEET_FILE_FORMAT) + " - " + countRows + " words");
                        sb.append("\n");
                    } catch (IOException e) {
                        sb.append("\t");
                        sb.append(pack.getName() + ": failed to collect information about this pack");
                        sb.append("\n");
                        SimpleLog.err("Error while collecting pack's information. Error: " + e);
                    } finally {
                        try {
                            sheetsController.close();
                        } catch (IOException e) {
                            SimpleLog.err(e.toString());
                        }
                    }
                }

                if (sb.isEmpty()) {
                    returnText = "You haven't any pack";
                } else {
                    returnText = "Your packs:\n" + sb;
                }
            } else if (
                    messageText.equals(Buttons.EXPORT_PACK.innerText) ||
                            messageText.equals(Buttons.CREATE_WORDS.innerText) ||
                            messageText.equals(Buttons.DELETE_PACK.innerText)
            ) {
                List<File> allPackFiles = dataController.getAllNamesFileSameType(Long.toString(chatId), Config.SHEET_FILE_FORMAT);
                if (allPackFiles.isEmpty()) {
                    returnText = "You haven't any pack";
                } else {
                    List<List<String>> buttonsNamesInRows = new ArrayList<>();
                    List<String> buttonsNames = new ArrayList<>();
                    for (File pack : allPackFiles) {
                        buttonsNames.add(removeFileExtension(
                                pack.getName(), Config.SHEET_FILE_FORMAT
                        ));
                        if (buttonsNames.size() >= COUNT_BUTTONS_IN_ROW) {
                            buttonsNamesInRows.add(buttonsNames);
                            buttonsNames = new ArrayList<>();
                        }
                    }
                    if (!buttonsNames.isEmpty()) {
                        buttonsNamesInRows.add(buttonsNames);
                    }

                    buttonsNamesInRows.add(List.of(Buttons.MAIN_MENU.innerText));
                    returnKeyboardMarkup = createReplyKeyboardMarkup(buttonsNamesInRows, true);
                    returnText = "Choose a pack";
                    stateAfterRequest = UserState.CHOOSING_PACK;
                }
            } else {
                if (contextHistory.getSizeOfHistory() > 0) {
                    RequestContext prevRequest = contextHistory.getContext(chatId, 0);
                    UserState prevUserState = prevRequest.getState();
                    Message prevMessage = prevRequest.getUpdate().getMessage();
                    String prevText = prevMessage.getText();

                    if (prevUserState == UserState.WRITING_PACK_NAME) {
                        try {
                            sheetsController.createExcelFromFile(
                                    dataController.createFile(getPathToFile(
                                            Long.toString(chatId), messageText + "." + Config.SHEET_FILE_FORMAT
                                    ))
                            );
                            returnText = messageText + " pack have been successful created.";
                        } catch (IllegalArgumentException e) {
                            returnText = messageText + " pack already exists";
                        } catch (IOException e) {
                            returnText = messageText + " pack has incorrect name";
                        }
                    } else if (prevUserState == UserState.CHOOSING_PACK) {
                        if (prevText.equals(Buttons.DELETE_PACK.innerText)) {
                            try {
                                dataController.removeFile(getPathToFile(Long.toString(chatId), messageText + "." + Config.SHEET_FILE_FORMAT));
                                returnText = messageText + " pack will be deleted soon.";
                            } catch (IllegalArgumentException | IOException e) {
                                returnText = "Something went wrong.";
                                SimpleLog.err("Error while deleting a file: " + messageText + "." + Config.SHEET_FILE_FORMAT + " Error: " + e);
                            }
                        }
                        if (prevText.equals(Buttons.EXPORT_PACK.innerText)) {
                            try {
                                File excel = dataController.openFile(getPathToFile(
                                        Long.toString(chatId), messageText + "." + Config.SHEET_FILE_FORMAT
                                ));

                                SendDocument returnExcel = new SendDocument(Long.toString(chatId), new InputFile(excel));
                                contextHistory.addContext(update, UserState.NOTHING);
                                execute(returnExcel);
                                return;
                            } catch (TelegramApiException | IllegalArgumentException e) {
                                returnText = "Something went wrong, sorry.\nRepeat an action later";
                                SimpleLog.err("Error while creating Excel file: " + messageText + "." + Config.SHEET_FILE_FORMAT + " Error: " + e);
                            }
                        } else if (prevText.equals(Buttons.CREATE_WORDS.innerText)) {
                            returnText = """
                                    Write words as "word | phrase : translation" without quotes.
                                    You can write several words. 
                                    Example: 
                                        dog : dog's translate,
                                        cat : cat's translate,
                                        hedgehog : hedgehog's translate
                                    """;
                            stateAfterRequest = UserState.WRITING_WORDS;
                            returnKeyboardMarkup = preparedKeyboardMarkups.get("return-to-main");
                        }
                    } else if (prevUserState == UserState.WRITING_WORDS) {
                        try {
                            Scanner scan = new Scanner(messageText);
                            StringBuilder sb = new StringBuilder("Your words:\n");

                            File excel = dataController.openFile(getPathToFile(Long.toString(chatId), prevText + "." + Config.SHEET_FILE_FORMAT));
                            sheetsController.openExcelFile(excel);
                            int curRow = sheetsController.getCountRowInSheet(SHEET_NAME);

                            while (scan.hasNextLine()) {
                                String line = scan.nextLine();
                                String[] parsedLine = line.split(":");

                                if (parsedLine.length != 2) {
                                    sb.append("\t line " + line + " has incorrect format\n");
                                } else {
                                    String word = parsedLine[0].trim();
                                    String translation = parsedLine[1].trim();

                                    if (word.isBlank() || translation.isBlank()) {
                                        sb.append("\t" + line + " has incorrect format\n");
                                    } else {
                                        sheetsController.writeOneCell(SHEET_NAME, curRow, 0, word);
                                        sheetsController.writeOneCell(SHEET_NAME, curRow, 1, translation);
                                        ++curRow;

                                        sb.append("\t" + word + " has been successful added\n");
                                    }
                                }
                            }

                            sheetsController.writeToFile(excel);
                            sheetsController.close();
                            returnText = sb.toString();
                        } catch (IOException e) {
                            returnText = "Something went wrong.";
                            SimpleLog.err("Error while writing words to excel file. Error: " + e);
                        }
                    }
                }
            }

            returnMessage.setChatId(chatId);
            returnMessage.setText(returnText);
            returnMessage.setReplyMarkup(returnKeyboardMarkup);
            contextHistory.addContext(update, stateAfterRequest);
            try {
                execute(returnMessage);
            } catch (TelegramApiException e) {
                SimpleLog.err(e.toString());
            }
        } else {
            SimpleLog.err("Update hasn't a message " + update);
        }
    }

    private ReplyKeyboardMarkup createReplyKeyboardMarkup(List<List<String>> rowsWithButtonNames, boolean isResize) {
        List<KeyboardRow> rows = new ArrayList<>();
        for (List<String> buttonNames : rowsWithButtonNames) {
            KeyboardRow row = new KeyboardRow();
            for (String buttonName : buttonNames) {
                row.add(buttonName);
            }
            rows.add(row);
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(rows);
        keyboardMarkup.setResizeKeyboard(isResize);
        return keyboardMarkup;
    }

    private String getPathToFile(String pathToFolder, String fileName) {
        return new File(new File(pathToFolder), fileName).getPath();
    }

    private String removeFileExtension(String fileName, String extension) {
        return fileName.substring(0, fileName.length() - 1 - extension.length());
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    private enum Buttons {
        MAIN_MENU("Main Menu"),
        CREATE_PACK("Create Pack"),
        REMOVE_PACK("Remove Pack"),
        SHOW_PACKS("Show Packs"),
        CREATE_WORDS("Add Words"),
        EXPORT_PACK("Export Pack"),
        DELETE_PACK("Delete Pack");

        private String innerText;

        Buttons(String innerText) {
            this.innerText = innerText;
        }
    }

    private enum UserState {
        NOTHING,
        WRITING_PACK_NAME, CHOOSING_PACK,
        WRITING_WORDS
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
        private final int maxSizeOfHistory;

        public int getSizeOfHistory() {
            return userContextHistory.size();
        }


        public RequestContextHistory(int maxSizeOfHistory) {
            this.maxSizeOfHistory = maxSizeOfHistory;
        }

        public void addContext(Update update, UserState state) {
            Long chatId = update.getMessage().getChatId();
            LinkedList<RequestContext> history = userContextHistory.getOrDefault(chatId, new LinkedList<>());
            history.add(new RequestContext(update, state));

            if (history.size() > maxSizeOfHistory) {
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
