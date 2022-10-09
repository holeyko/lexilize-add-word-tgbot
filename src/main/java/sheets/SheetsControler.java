package sheets;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import org.telegram.telegrambots.meta.generics.Webhook;
import org.telegram.telegrambots.meta.generics.WebhookBot;

import java.io.File;
import java.io.IOException;

public class SheetsControler {
    private WritableWorkbook sheetsWriter;
    private Workbook sheetsReader;

    public void openToWrite(String pathToFile, String sheetName) throws IllegalArgumentException, FileIsNotExcelException {
        if (isExcelFile(pathToFile)) {
            throw new FileIsNotExcelException(pathToFile);
        }

        File excel = new File(pathToFile);
        if (!excel.exists()) {
            throw new IllegalArgumentException(String.format("File %s doesn't exist", pathToFile));
        }

        try {
            sheetsWriter = Workbook.createWorkbook(excel);
            if (sheetsWriter.getNumberOfSheets() == 0) {
                sheetsWriter.createSheet(sheetName, 0);
            }
        } catch (IOException e) {
            closeWrite();
            //TODO: write err log
        }
    }

    public void write(String sheetName, int row, int column, String value) throws IOException, WriteException {
        if (sheetsWriter == null) {
            throw new IOException("Sheet Writer is close");
        }
        closeRead();
        try {
            WritableSheet sheet = sheetsWriter.getSheet(sheetName);
            Label label = new Label(row, column, value);
            sheet.addCell(label);
        } catch (WriteException e) {
            throw e;
        }
    }

    public int countWrittenRows(String pathToFile, String sheetName) throws FileIsNotExcelException {
        if (isExcelFile(pathToFile)) {
            throw new FileIsNotExcelException(pathToFile);
        }
        closeWrite();
        File excel = new File(pathToFile);
        if (!excel.exists()) {
            throw new IllegalArgumentException(String.format("File %s doesn't exist"));
        }

        int countRows = 0;
        try {
            sheetsReader = Workbook.getWorkbook(excel);
            Sheet sheet = sheetsReader.getSheet(sheetName);

            for (int i = 0;;++i) {
                Cell cell = sheet.getCell(i, 0);
                if (cell.getContents() == null) {
                    countRows = i;
                    break;
                }
            }
        } catch (BiffException e) {
            throw new RuntimeException(e); //TODO: write a err log
        } catch (IOException e) {
            throw new RuntimeException(e); //TODO: write a err log
        }

        closeWrite();
        return countRows;
    }

    public void closeWrite() {
        if (sheetsWriter != null) {
            try {
                sheetsWriter.close();
            } catch (WriteException | IOException e) {
                //TODO: write err log
            }
        }
    }

    public void closeRead() {
        if (sheetsReader != null) {
            try {
                sheetsWriter.close();
            } catch (WriteException | IOException e) {
                //TODO: write err log
            }
        }
    }

    private boolean isExcelFile(String pathToFile) {
        return pathToFile.endsWith(".xlsx") || pathToFile.endsWith(".xls");
    }
}
