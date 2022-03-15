package yanan.zhang;

import com.alibaba.fastjson.JSON;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yanan.zhang.web.WebDataImpl;
import yanan.zhang.web.model.Detail;
import yanan.zhang.web.model.Overview;
import yanan.zhang.web.model.PieChartInfo;
import yanan.zhang.web.model.StackedChartInfo;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * @author Yanan Zhang
 **/
public class PoiExportImpl {

    private static final Logger logger = LoggerFactory.getLogger(PoiExportImpl.class);
    private final MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
    private final WebDataImpl webData = new WebDataImpl();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy '@' HH:mm", Locale.UK);

    public void exportExcel() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        // 获取桌面路径
        FileSystemView fsv = FileSystemView.getFileSystemView();
        String desktop = fsv.getHomeDirectory().getPath();
        String filePath = desktop + "/Result" + dateStr + ".xls";

        File file = new File(filePath);
        OutputStream outputStream = new FileOutputStream(file);
        HSSFWorkbook workbook = new HSSFWorkbook();
        this.createDomainSheet(workbook);
        this.createCategorySheet(workbook, CategoryEnum.EVENTS);
        this.createCategorySheet(workbook, CategoryEnum.MATERIALS);
        this.createCategorySheet(workbook, CategoryEnum.E_LEARNING);
        this.createCategorySheet(workbook, CategoryEnum.WORKFLOWS);
        this.createWebDataSheet(workbook);

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
        header.createCell(4).setCellValue("page");
        header.createCell(5).setCellValue("detail link");
        header.createCell(6).setCellValue("dead link title");
        header.createCell(7).setCellValue("Detection time");
        // 设置行的高度
        header.setHeightInPoints(30);

        // 设置列的宽度
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 20 * 128);
        sheet.setColumnWidth(5, 20 * 256);
        sheet.setColumnWidth(6, 20 * 256);
        sheet.setColumnWidth(7, 20 * 256);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 拿到主域名数据
        List<DeadLinkDomain> domainList = jdbc.selectDeadLinkDomain(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
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
                row.createCell(4).setCellValue(domain.getPage());
                row.createCell(5).setCellValue(domain.getDetailLink());
                row.createCell(6).setCellValue(domain.getDeadLinkTitle());
                row.createCell(7).setCellValue(sdf.format(domain.getCreateTime()));
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
        header.createCell(6).setCellValue("Dead link title");
        header.createCell(7).setCellValue("Parent url");
        header.createCell(8).setCellValue("Start time");
        header.createCell(9).setCellValue("End time");
        header.createCell(10).setCellValue("Duration(days)");
        header.createCell(11).setCellValue("Detection time");
        if (categoryEnum.equals(CategoryEnum.EVENTS)) {
            header.createCell(12).setCellValue("Status");
        }
        // 设置行的高度
        header.setHeightInPoints(30);

        // 设置列的宽度
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 20 * 256);
        sheet.setColumnWidth(5, 20 * 512);
        sheet.setColumnWidth(6, 20 * 256);
        sheet.setColumnWidth(7, 20 * 512);
        sheet.setColumnWidth(8, 20 * 256);
        sheet.setColumnWidth(9, 20 * 256);
        sheet.setColumnWidth(10, 20 * 256);
        sheet.setColumnWidth(11, 20 * 256);
        if (categoryEnum.equals(CategoryEnum.EVENTS)) {
            sheet.setColumnWidth(12, 20 * 128);
        }

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
                row.createCell(6).setCellValue(record.getDeadLinkTitle());
                row.createCell(7).setCellValue(record.getParentUrl());
                if (record.getStart() != null) {
                    row.createCell(8).setCellValue(record.getStart());
                } else {
                    row.createCell(8).setCellValue("Not given");
                }
                if (record.getEnd() != null) {
                    row.createCell(9).setCellValue(record.getEnd());
                } else {
                    row.createCell(9).setCellValue("Not given");
                }
                if (record.getDuration() != null) {
                    row.createCell(10).setCellValue(record.getDuration());
                } else {
                    row.createCell(10).setCellValue(0);
                }
                row.createCell(11).setCellValue(sdf.format(record.getCreateTime()));
                if (categoryEnum.equals(CategoryEnum.EVENTS)) {
                    try {
                        if (record.getDuration() == null || record.getDuration().trim().length() == 0) {
                            row.createCell(12).setCellValue("undefined");
                        } else if (record.getEnd() != null && DateUtils.dateInterval(simpleDateFormat.parse(record.getEnd()), new Date()) < 1) {
                            // endDate早于今天就是past
                            row.createCell(12).setCellValue("past");
                        } else {
                            row.createCell(12).setCellValue("current");
                        }
                    } catch (ParseException e) {
                        logger.error("parse end date error! record.getEnd={}", record.getEnd(), e);
                    }
                }
            }
        }
    }

    private void createWebDataSheet(HSSFWorkbook workbook) {
        int rowNum = 0;
        HSSFSheet sheet = workbook.createSheet("WebData");
        HSSFRow header = sheet.createRow(rowNum);
        rowNum++;
        header.createCell(0).setCellValue("Overview");
        header.createCell(1).setCellValue("PieChart");
        header.createCell(2).setCellValue("Detail");
        header.createCell(3).setCellValue("StackedChart");
        header.createCell(4).setCellValue("LineChart");
        // 设置行的高度
        header.setHeightInPoints(30);
        // 设置列的宽度
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 512);
        sheet.setColumnWidth(2, 20 * 512);
        sheet.setColumnWidth(3, 20 * 512);
        sheet.setColumnWidth(4, 20 * 512);

        // 获取overview
        Overview overview = webData.getOverview();
        HSSFRow row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(JSON.toJSONString(overview));

        // 获取饼图数据
        PieChartInfo pieChart = webData.getPieChart();
        row.createCell(1).setCellValue(JSON.toJSONString(pieChart));

        // 获取Detail
        Detail detail = webData.getDetail();
        row.createCell(2).setCellValue(JSON.toJSONString(detail));

        // 获取柱状图
        StackedChartInfo stackedChart = webData.getStackedChart();
        row.createCell(3).setCellValue(JSON.toJSONString(stackedChart));

        // 获取折线图
        StackedChartInfo lineChart = webData.getLineChart();
        row.createCell(4).setCellValue(JSON.toJSONString(lineChart));
    }

}