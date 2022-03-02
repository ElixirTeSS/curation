package yanan.zhang;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yanan Zhang
 **/
public class PoiExportImpl {

    private final MySqlJDBCImpl jdbc = new MySqlJDBCImpl();

//    public static void main(String args[]) {
//        System.out.println("Hello World!");
//        FileSystemView fsv = FileSystemView.getFileSystemView();
//        String desktop = fsv.getHomeDirectory().getPath();
//        System.out.println(desktop);
//        String filePath = desktop + "/Result.csv";
//    }

    public void exportExcel() throws IOException {
        // 获取桌面路径
        FileSystemView fsv = FileSystemView.getFileSystemView();
        String desktop = fsv.getHomeDirectory().getPath();
        String filePath = desktop + "/Result.xls";

        File file = new File(filePath);
        OutputStream outputStream = new FileOutputStream(file);
        HSSFWorkbook workbook = new HSSFWorkbook();
        this.createDomainSheet(workbook);
        this.createCategorySheet(workbook, CategoryEnum.EVENTS);
        this.createCategorySheet(workbook, CategoryEnum.MATERIALS);
        this.createCategorySheet(workbook, CategoryEnum.E_LEARNING);
        this.createCategorySheet(workbook, CategoryEnum.WORKFLOWS);

        workbook.setActiveSheet(0);
        workbook.write(outputStream);
        outputStream.close();
    }

    /**
     * 添加主域名Sheet
     *
     * @param workbook
     */
    private void createDomainSheet(HSSFWorkbook workbook) {
        int rowNum = 0;
        HSSFSheet sheet = workbook.createSheet("Domain");
        HSSFRow header = sheet.createRow(rowNum);
        rowNum++;
        header.createCell(0).setCellValue("Domain url");
        header.createCell(1).setCellValue("Http status code");
        header.createCell(2).setCellValue("Http reason phrase");
        header.createCell(3).setCellValue("Number of detected links");
        header.createCell(4).setCellValue("Detection time");
        // 设置行的高度
        header.setHeightInPoints(30);

        // 设置列的宽度
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 20 * 256);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 拿到主域名数据
        List<DeadLinkDomain> domainList = jdbc.selectDeadLinkDomain();
        if (domainList != null && domainList.size() > 0) {
            // 排序
            domainList = domainList.stream().sorted(Comparator.comparing(DeadLinkDomain::getStatusCode).reversed()).collect(Collectors.toList());
            for (DeadLinkDomain domain : domainList) {
                HSSFRow row = sheet.createRow(rowNum);
                rowNum++;
                row.createCell(0).setCellValue(domain.getDomainUrl());
                row.createCell(1).setCellValue(domain.getStatusCode());
                row.createCell(2).setCellValue(domain.getReasonPhrase());
                row.createCell(3).setCellValue(domain.getLinkNumber());
                row.createCell(4).setCellValue(sdf.format(domain.getCreateTime()));
            }
        }
    }

    /**
     * 添加各种类Sheet
     *
     * @param workbook
     */
    private void createCategorySheet(HSSFWorkbook workbook, CategoryEnum categoryEnum) {
        int rowNum = 0;
        HSSFSheet sheet = workbook.createSheet(categoryEnum.getName());
        HSSFRow header = sheet.createRow(rowNum);
        rowNum++;
        header.createCell(0).setCellValue("Category");
        header.createCell(1).setCellValue("Page number");
        header.createCell(2).setCellValue("Http status code");
        header.createCell(3).setCellValue("Http reason phrase");
        header.createCell(4).setCellValue("Type");
        header.createCell(5).setCellValue("Dead link");
        header.createCell(6).setCellValue("Parent url");
        header.createCell(7).setCellValue("Detection time");
        // 设置行的高度
        header.setHeightInPoints(30);

        // 设置列的宽度
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 20 * 256);
        sheet.setColumnWidth(5, 20 * 512);
        sheet.setColumnWidth(6, 20 * 512);
        sheet.setColumnWidth(7, 20 * 256);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 拿到主域名数据
        List<DeadLinkRecords> recordList = jdbc.selectDeadLinkRecordsByCategory(categoryEnum.getName());
        if (recordList != null && recordList.size() > 0) {
            // 排序
            recordList = recordList.stream().sorted(Comparator.comparing(DeadLinkRecords::getStatusCode).reversed()).collect(Collectors.toList());
            for (DeadLinkRecords record : recordList) {
                HSSFRow row = sheet.createRow(rowNum);
                rowNum++;
                row.createCell(0).setCellValue(record.getCategory());
                row.createCell(1).setCellValue(record.getPage());
                row.createCell(2).setCellValue(record.getStatusCode());
                row.createCell(3).setCellValue(record.getReasonPhrase());
                row.createCell(4).setCellValue(record.getType());
                row.createCell(5).setCellValue(record.getDeadLink());
                row.createCell(6).setCellValue(record.getParentUrl());
                row.createCell(7).setCellValue(sdf.format(record.getCreateTime()));
            }
        }
    }

}


