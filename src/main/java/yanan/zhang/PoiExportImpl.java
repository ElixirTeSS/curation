package yanan.zhang;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.CollectionUtils;
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
        // get the path
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
        this.createBlackListSheet(workbook);
        this.createWhiteListSheet(workbook);

        workbook.setActiveSheet(0);
        workbook.write(outputStream);
        outputStream.close();
    }

    /**
     * Broken domain Sheet
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
        header.createCell(8).setCellValue("Color");
        // set the height
        header.setHeightInPoints(30);

        // set the width
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 20 * 128);
        sheet.setColumnWidth(5, 20 * 256);
        sheet.setColumnWidth(6, 20 * 256);
        sheet.setColumnWidth(7, 20 * 256);
        sheet.setColumnWidth(8, 20 * 128);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // get broken domain data
        List<DeadLinkDomain> todayList = jdbc.selectDeadLinkDomain(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
        List<DeadLinkDomain> yesterdayList = jdbc.selectDeadLinkDomain(DateUtils.format(DateUtils.minusDays(new Date(), 1), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
        CollectionDiffResult<DeadLinkDomain> diffResult = CollectionDiffUtils.diff(yesterdayList, todayList, DeadLinkDomain::getDomainUrl);
        List<String> insertList = null;
        if (CollectionUtils.isNotEmpty(diffResult.getInsertList())) {
            insertList = diffResult.getInsertList().stream().map(DeadLinkDomain::getDomainUrl).collect(Collectors.toList());
        }
        List<String> deleteList = null;
        if (CollectionUtils.isNotEmpty(diffResult.getDeleteList())) {
            deleteList = diffResult.getDeleteList().stream().map(DeadLinkDomain::getDomainUrl).collect(Collectors.toList());
        }
        if (todayList != null && todayList.size() > 0) {
            // sorting
            todayList = todayList.stream().sorted(Comparator.comparing(DeadLinkDomain::getLinkNumber).reversed()).collect(Collectors.toList());
            for (DeadLinkDomain domain : todayList) {
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
                if (CollectionUtils.isNotEmpty(insertList) && insertList.contains(domain.getDomainUrl())) {
                    // yesterday has but today does not have - red
                    row.createCell(8).setCellValue("red");
                } else if (CollectionUtils.isNotEmpty(deleteList) && deleteList.contains(domain.getDomainUrl())) {
                    // yesterday does not have but today has - green
                    row.createCell(8).setCellValue("green");
                } else {
                    // both have - white
                    row.createCell(8).setCellValue("white");
                }
            }
        }
    }

    /**
     * Each Categories Sheet
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
        header.createCell(12).setCellValue("Color");
        if (categoryEnum.equals(CategoryEnum.EVENTS)) {
            header.createCell(13).setCellValue("Status");
        }
        // set the height
        header.setHeightInPoints(30);

        // set the width
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
        sheet.setColumnWidth(12, 20 * 128);
        if (categoryEnum.equals(CategoryEnum.EVENTS)) {
            sheet.setColumnWidth(13, 20 * 128);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // get data
        List<DeadLinkRecords> todayList = jdbc.selectDeadLinkRecordsByCategory(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL), categoryEnum.getName());
        List<DeadLinkRecords> yesterdayList = jdbc.selectDeadLinkRecordsByCategory(DateUtils.format(DateUtils.minusDays(new Date(), 1), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL), categoryEnum.getName());
        CollectionDiffResult<DeadLinkRecords> diffResult = CollectionDiffUtils.diff(yesterdayList, todayList,
                x -> x.getCategory() + "|" + x.getType() + "|" + x.getDeadLink().trim() + "|" + x.getParentUrl().trim());
        List<String> insertList = null;
        if (CollectionUtils.isNotEmpty(diffResult.getInsertList())) {
            insertList = diffResult.getInsertList().stream().map(DeadLinkRecords::getDeadLink).collect(Collectors.toList());
        }
        List<String> deleteList = null;
        if (CollectionUtils.isNotEmpty(diffResult.getDeleteList())) {
            deleteList = diffResult.getDeleteList().stream().map(DeadLinkRecords::getDeadLink).collect(Collectors.toList());
        }
        if (todayList != null && todayList.size() > 0) {
            // sorting
            todayList = todayList.stream().sorted(Comparator.comparing(DeadLinkRecords::getStatusCode).reversed()).collect(Collectors.toList());
            for (DeadLinkRecords record : todayList) {
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

                if (CollectionUtils.isNotEmpty(insertList) && insertList.contains(record.getDeadLink())) {
                    // yesterday has but today does not have - red
                    row.createCell(12).setCellValue("red");
                } else if (CollectionUtils.isNotEmpty(deleteList) && deleteList.contains(record.getDeadLink())) {
                    // yesterday does not have but today has - green
                    row.createCell(12).setCellValue("green");
                } else {
                    // both have - white
                    row.createCell(12).setCellValue("white");
                }

                if (categoryEnum.equals(CategoryEnum.EVENTS)) {
                    try {
                        if (record.getDuration() == null || record.getDuration().trim().length() == 0) {
                            row.createCell(13).setCellValue("undefined");
                        } else if (record.getEnd() != null && DateUtils.dateInterval(simpleDateFormat.parse(record.getEnd()), new Date()) > 0) {
                            // endDate is earlier than current date - past
                            row.createCell(13).setCellValue("past");
                        } else {
                            row.createCell(13).setCellValue("current");
                        }
                    } catch (ParseException e) {
                        logger.error("parse end date error! record.getEnd={}", record.getEnd(), e);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy '@' HH:mm", Locale.UK);
        System.out.println(DateUtils.dateInterval(simpleDateFormat.parse("Friday, 18 January 2013 @ 00:00"), new Date()));
        System.out.println(DateUtils.dateInterval(simpleDateFormat.parse("Thursday, 17 March 2022 @ 00:00"), new Date()));
        System.out.println(DateUtils.dateInterval(simpleDateFormat.parse("Friday, 18 March 2022 @ 00:00"), new Date()));
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
        header.createCell(5).setCellValue("CodeJson");
        // set the height
        header.setHeightInPoints(30);
        // set the width
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 512);
        sheet.setColumnWidth(2, 20 * 512);
        sheet.setColumnWidth(3, 20 * 512);
        sheet.setColumnWidth(4, 20 * 512);
        sheet.setColumnWidth(5, 20 * 512);

        // get overview data
        Overview overview = webData.getOverview();
        HSSFRow row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(JSON.toJSONString(overview));

        // get data for pir chart
        PieChartInfo pieChart = webData.getPieChart();
        row.createCell(1).setCellValue(JSON.toJSONString(pieChart));

        // get Detail data
        Detail detail = webData.getDetail();
        row.createCell(2).setCellValue(JSON.toJSONString(detail));

        // get data for stacked chart
        StackedChartInfo stackedChart = webData.getStackedChart();
        row.createCell(3).setCellValue(JSON.toJSONString(stackedChart));

        // get data for line chart
        StackedChartInfo lineChart = webData.getLineChart();
        row.createCell(4).setCellValue(JSON.toJSONString(lineChart));

        // get codeJson
        String codeJson = webData.getCodeJson();
        row.createCell(5).setCellValue(codeJson);
    }

    private void createBlackListSheet(HSSFWorkbook workbook) {
        int rowNum = 0;
        HSSFSheet sheet = workbook.createSheet("BlackList");
        HSSFRow header = sheet.createRow(rowNum);
        rowNum++;
        header.createCell(0).setCellValue("Domain url");
        header.createCell(1).setCellValue("Number of detected links");
        header.createCell(2).setCellValue("Detection time");
        // set the height
        header.setHeightInPoints(30);

        // set the width
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // get black list data
        List<BlackListDomain> domainList = jdbc.selectBlackListDomain();
        if (domainList != null && domainList.size() > 0) {
            // sorting
            for (BlackListDomain domain : domainList) {
                HSSFRow row = sheet.createRow(rowNum);
                rowNum++;
                row.createCell(0).setCellValue(domain.getDomainUrl());
                int count = jdbc.countDeadLinkRecordsByDomain(domain.getDomainUrl());
                row.createCell(1).setCellValue(count);
                row.createCell(2).setCellValue(sdf.format(domain.getCreateTime()));
            }
        }
    }

    private void createWhiteListSheet(HSSFWorkbook workbook) {
        int rowNum = 0;
        HSSFSheet sheet = workbook.createSheet("WhiteList");
        HSSFRow header = sheet.createRow(rowNum);
        rowNum++;
        header.createCell(0).setCellValue("Domain url");
        header.createCell(1).setCellValue("Number of detected links");
        header.createCell(2).setCellValue("Detection time");
        // set height
        header.setHeightInPoints(30);

        // set width
        sheet.setColumnWidth(0, 20 * 512);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // get white list data
        List<WhiteListDomain> domainList = jdbc.selectWhiteListDomain();
        if (domainList != null && domainList.size() > 0) {
            // sorting
            for (WhiteListDomain domain : domainList) {
                HSSFRow row = sheet.createRow(rowNum);
                rowNum++;
                row.createCell(0).setCellValue(domain.getDomainUrl());
                int count = jdbc.countDeadLinkRecordsByDomain(domain.getDomainUrl());
                row.createCell(1).setCellValue(count);
                row.createCell(2).setCellValue(sdf.format(domain.getCreateTime()));
            }
        }
    }

}