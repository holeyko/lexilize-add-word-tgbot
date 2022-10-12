package sheets;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;


public class SheetsController {
    private Workbook workbook;

    public void openExcelFile(File file) throws IOException {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            workbook = WorkbookFactory.create(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            throw new IOException("Error while opening excel file " + file.getPath());
        }
    }

    public void createExcelFromFile(File excel) throws IOException {
        workbook = new XSSFWorkbook();
        workbook.write(new FileOutputStream(excel));
    }

    public void writeOneCell(String sheetName, int rowIndex, int columnIndex, String value) throws IOException {
        valid();

        Sheet sheet;
        if ((sheet = workbook.getSheet(sheetName)) == null) {
            sheet = workbook.createSheet(sheetName);
        }

        Row row = sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
        Cell cell = row.getCell(columnIndex) == null ? row.createCell(columnIndex) : row.getCell(columnIndex);
        cell.setCellValue(value);
    }

    public void writeToFile(File excel) throws IOException {
        valid();

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

        Sheet sheet = workbook.getSheet(sheetName);
        return sheet == null ? 0 : sheet.getLastRowNum() + 1;
    }

    public void valid() throws IOException {
        if (workbook == null) {
            throw new IOException("SheetController hasn't open file");
        }
    }

}
