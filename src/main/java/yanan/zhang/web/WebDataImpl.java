package yanan.zhang.web;

import yanan.zhang.*;
import yanan.zhang.web.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Yanan Zhang
 **/
public class WebDataImpl {

    private final MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
    private final Date current = new Date();
    private final String currentDateStr = DateUtils.format(current, DateUtils.FORMATTER_DATE);
    private final List<CollectInfo> collectInfos = jdbc.selectCollectInfoByDate(this.get7DayList());
    private final List<DeadLinkRecords> currentData = jdbc.selectDeadLinkRecords(DateUtils.format(current, DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));


    public Overview getOverview() {
        Overview overview = new Overview();
        if (collectInfos != null && collectInfos.size() > 0) {
            List<CollectInfo> temp = collectInfos.stream().filter(x -> x.getCreateDate().equals(currentDateStr)).collect(Collectors.toList());
            if (temp.size() > 0) {
                CollectInfo collectInfo = collectInfos.get(0);
                overview.setTotalLinks(collectInfo.getEvents() + collectInfo.getMaterials() + collectInfo.getElearning() + collectInfo.getWorkflows());
                overview.setTotalDeadLinks(collectInfo.getEventsDead() + collectInfo.getMaterialsDead() + collectInfo.getElearningDead() + collectInfo.getWorkflowsDead());
                overview.setTotalDeadDomains(collectInfo.getDomainDead());
            }
        }

        return overview;
    }

    public Detail getDetail() {
        Detail detail = new Detail();
        if (collectInfos != null && collectInfos.size() > 0) {
            List<CollectInfo> temp = collectInfos.stream().filter(x -> x.getCreateDate().equals(currentDateStr)).collect(Collectors.toList());
            if (temp.size() > 0) {
                CollectInfo collectInfo = collectInfos.get(0);
                detail.setEvents(collectInfo.getEvents());
                detail.setEventsDead(collectInfo.getEventsDead());
                detail.setMaterials(collectInfo.getMaterials());
                detail.setMaterialsDead(collectInfo.getMaterialsDead());
                detail.setElearning(collectInfo.getElearning());
                detail.setElearningDead(collectInfo.getElearningDead());
                detail.setWorkflows(collectInfo.getWorkflows());
                detail.setWorkflowsDead(collectInfo.getWorkflowsDead());
            }
        }

        return detail;
    }

    public PieChartInfo getPieChart() {
        PieChartInfo result = new PieChartInfo();
        List<PieChart> list = new ArrayList<>();

        Map<Integer, Long> map = currentData.stream().collect(Collectors.groupingBy(DeadLinkRecords::getStatusCode, Collectors.counting()));
        Iterator<Integer> it = map.keySet().iterator();
        while (it.hasNext()) {
            PieChart pieChart = new PieChart();
            Integer key = it.next();
            pieChart.setType(key.toString());
            pieChart.setValue(map.get(key));
            list.add(pieChart);
        }
        result.setData(list);
        return result;
    }

    public StackedChartInfo getStackedChart() {
        StackedChartInfo result = new StackedChartInfo();
        List<StackedChart> list = new ArrayList<>();
        List<DeadLinkRecords> yesterdayData = jdbc.selectDeadLinkRecords(DateUtils.format(DateUtils.minusDays(current, 1), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
        Map<String, Long> todayMap = currentData.stream().collect(Collectors.groupingBy(DeadLinkRecords::getCategory, Collectors.counting()));
        Map<String, Long> yesterdayMap = yesterdayData.stream().collect(Collectors.groupingBy(DeadLinkRecords::getCategory, Collectors.counting()));
        List<DeadLinkDomain> todayDomainList = jdbc.selectDeadLinkDomain(DateUtils.format(current, DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
        List<DeadLinkDomain> yesterdayDomainList = jdbc.selectDeadLinkDomain(DateUtils.format(DateUtils.minusDays(current, 1), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
        StackedChart yesterdayDomain = new StackedChart();
        yesterdayDomain.setName("Yesterday");
        yesterdayDomain.setCategory("Domain");
        if (yesterdayDomainList != null) {
            yesterdayDomain.setCount(yesterdayDomainList.size());
        } else {
            yesterdayDomain.setCount(0);
        }
        list.add(yesterdayDomain);
        StackedChart todayDomain = new StackedChart();
        todayDomain.setName("Today");
        todayDomain.setCategory("Domain");
        if (todayDomainList != null) {
            todayDomain.setCount(todayDomainList.size());
        } else {
            todayDomain.setCount(0);
        }
        list.add(todayDomain);

        for (int i = 0; i < CategoryEnum.values().length; i++) {
            String category = CategoryEnum.values()[i].getName();
            StackedChart yesterday = new StackedChart();
            yesterday.setName("Yesterday");
            yesterday.setCategory(category);
            if (yesterdayMap != null && yesterdayMap.get(category) != null) {
                yesterday.setCount(yesterdayMap.get(category));
            } else {
                yesterday.setCount(0);
            }
            list.add(yesterday);

            StackedChart today = new StackedChart();
            today.setName("Today");
            today.setCategory(category);
            if (todayMap != null && todayMap.get(category) != null) {
                today.setCount(todayMap.get(category));
            } else {
                today.setCount(0);
            }
            list.add(today);
        }
        result.setData(list);

        return result;
    }

    public StackedChartInfo getLineChart() {
        StackedChartInfo result = new StackedChartInfo();
        List<StackedChart> list = new ArrayList<>();
        List<String> dayList = this.get7DayList();
        for (String date : dayList) {
            CollectInfo temp = null;
            List<CollectInfo> collect = collectInfos.stream().filter(x -> x.getCreateDate().equals(date)).collect(Collectors.toList());
            if (collect.size() > 0) {
                temp = collect.get(0);
            }
            StackedChart domain = new StackedChart();
            domain.setName(date);
            domain.setCategory("Dead domains");
            if (temp != null) {
                domain.setCount(temp.getDomainDead());
            } else {
                domain.setCount(0);
            }
            list.add(domain);

            StackedChart record = new StackedChart();
            record.setName(date);
            record.setCategory("Dead links");
            if (temp != null) {
                record.setCount(temp.getEventsDead() + temp.getMaterialsDead() + temp.getElearningDead() + temp.getWorkflowsDead());
            } else {
                record.setCount(0);
            }
            list.add(record);
        }
        result.setData(list);

        return result;
    }



    private List<String> get7DayList() {
        List<String> dateList = new ArrayList<>();
        dateList.add(DateUtils.format(DateUtils.minusDays(new Date(), 6), DateUtils.FORMATTER_DATE));
        dateList.add(DateUtils.format(DateUtils.minusDays(new Date(), 5), DateUtils.FORMATTER_DATE));
        dateList.add(DateUtils.format(DateUtils.minusDays(new Date(), 4), DateUtils.FORMATTER_DATE));
        dateList.add(DateUtils.format(DateUtils.minusDays(new Date(), 3), DateUtils.FORMATTER_DATE));
        dateList.add(DateUtils.format(DateUtils.minusDays(new Date(), 2), DateUtils.FORMATTER_DATE));
        dateList.add(DateUtils.format(DateUtils.minusDays(new Date(), 1), DateUtils.FORMATTER_DATE));
        dateList.add(currentDateStr);
        return dateList;
    }

}
