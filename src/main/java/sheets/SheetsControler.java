package sheets;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import util.Config;

import java.io.*;


public class SheetsControler {
    private Workbook workbook;

    public void openExcelFile(FileInputStream fileInputStream) throws IOException {
        try {
            workbook = WorkbookFactory.create(fileInputStream);
        } catch (IOException e) {
            throw new IOException(e);  //TODO: write err log
        }
    }

    public void createExcelFromFile(File excel) throws IOException {
        workbook = new XSSFWorkbook();
        workbook.write(new FileOutputStream(excel));
    }

    public void writeOneCell(String sheetName, int rowIndex, int columnIndex, String value) throws IOException {
        if (workbook == null) {
            throw new IOException(String.format("SheetControler hasn't open file"));
        }

        Sheet sheet;
        if ((sheet = workbook.getSheet(sheetName)) == null) {
            sheet = workbook.createSheet(sheetName);
        }

        Row row;
        Cell cell;
        if ((row = sheet.getRow(rowIndex)) == null) {
            row = sheet.createRow(rowIndex);
        }
        if ((cell = row.getCell(columnIndex)) == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value);
    }

    public void writeToFile(File excel) throws IOException {
        if (workbook == null) {
            throw new IOException(String.format("SheetControler hasn't open file"));
        }

        try (OutputStream fileOutputStream = new FileOutputStream(excel)) {
            workbook.write(fileOutputStream);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.format("File %s doesn't exist", excel.getPath()));
        } catch (IOException e) {
            throw new IOException(String.format("Failed in writing to file %s", excel.getPath()));
        }
    }

    public void close() throws IOException {
        workbook.close();
    }

    public int getCountRowInSheet(String sheetName) throws IOException {
        if (workbook == null) {
            throw new IOException(String.format("SheetControler hasn't open file"));
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            workbook.createSheet(sheetName);
            return 0;
        }

        return sheet.getLastRowNum() + 1;
    }

    private boolean isExcelFile(File file) {
        return file.getPath().endsWith("." + Config.SHEET_FILE_FORMAT);
    }
}
