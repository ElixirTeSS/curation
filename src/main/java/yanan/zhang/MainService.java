package yanan.zhang;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real service can be divided into two steps:
 * 1. Get all the details on a list page
 * 2. Visit a detail page to get the URL and whether the URL is valid
 * @author Yanan Zhang
 **/
public class MainService {

    private static final Logger logger = LoggerFactory.getLogger(MainService.class);

    private static final String WEB_SITE = "https://tess.elixir-europe.org";
    //	private static final String WEB_SITE = "http://test-tess.its.manchester.ac.uk";
    /**
     * starting page for crawling
     */
    private static final int PAGE_START = 1;
    /**
     * maximum page for crawling
     */
    private static final int TOTAL_PAGE = 2;
    /**
     * Multi-thread: number of link per thread
     */
    private static final int PROCESSED_PER_THREAD = 200;
    /**
     * retry
     */
    private static final int RETRY_TIMES = 3;

    public static final String CSS_QUERY_EVENTS = "div.page-header > p > a";
    public static final String CSS_QUERY_MATERIALS_AND_E_LEARNING = "div.text-justify a";
    public static final String CSS_QUERY_WORKFLOWS = "div#workflow-diagram-sidebar a";
    /**
     * regular expression: find out domain
     */
    public static final String MAIN_URL_REGEX = "^((https|http|ftp|rtsp|mms)?:\\/\\/)[^\\/]+";
    /**
     * map for recording the broken domain
     */
    public static Map<String, CacheInfo> MAIN_URL_DEAD_MAP = new HashMap<>();

    /**
     * Black list: http status code
     */
    private static final int BLACK_LIST_CODE = 1;

    /**
     * Black list: reason phrase
     */
    private static final String BLACK_LIST_MSG = "on the black list";

    /**
     * Black list: http status code
     */
    private static final int ILLEGAL_URL_CODE = 2;

    /**
     * Black list: reason phrase
     */
    private static final String ILLEGAL_URL_MSG = "illegal url";

    private List<BlackListDomain> blackListDomains = null;

    private List<String> legalSuffixList = null;

    int totalEvents = 0;
    int totalMaterials = 0;
    int totalElearning = 0;
    int totalWorkflows = 0;
    int totalDomainDead = 0;

    /**
     * Single-thread crawler
     *
     * @return
     */
    public List<SingleUrlResult> execute() {
        MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
        List<SingleUrlResult> resultList = new ArrayList<>();
        List<SingleUrlResult> totalPageList = this.crawlData();
        DecimalFormat df = new DecimalFormat("#.00");
        for (int i = 0; i < totalPageList.size(); i++) {
            SingleUrlResult obj = totalPageList.get(i);
            if ((i + 1) % 50 == 0) {
                String progress = df.format(Double.parseDouble(i + "") / Double.parseDouble(totalPageList.size() + "") * 100.0);
                logger.info("******************** Progress {}%. ********************", progress);
            }
            if (obj.getCategory().equals(CategoryEnum.EVENTS.getName())) {
                refillDetail(obj, CSS_QUERY_EVENTS);
            } else if (obj.getCategory().equals(CategoryEnum.MATERIALS.getName()) || obj.getCategory().equals(CategoryEnum.E_LEARNING.getName())) {
                refillDetail(obj, CSS_QUERY_MATERIALS_AND_E_LEARNING);
            } else if (obj.getCategory().equals(CategoryEnum.WORKFLOWS.getName())) {
                refillDetail(obj, CSS_QUERY_WORKFLOWS);
            }
            resultList.add(obj);
        }
        jdbc.dropTable();
        jdbc.createTable();
        this.saveDeadLink2DB(jdbc, resultList);
        // record broken domain
        if (MAIN_URL_DEAD_MAP.size() > 0) {
            jdbc.dropTableDomain();
            jdbc.createTableDomain();
            this.saveDeadLinkDomain2DB(jdbc, new ArrayList<>(MAIN_URL_DEAD_MAP.values()));
            totalDomainDead = MAIN_URL_DEAD_MAP.size();
        }
        // record overall data
        this.saveCollectInfo(jdbc);
        PoiExportImpl poi = new PoiExportImpl();
        try {
            poi.exportExcel();
        } catch (IOException e) {
            logger.error("export excel error!", e);
        }
        return resultList;
    }

    /**
     * Multi-thread crawler
     *
     * @return
     */
    @Deprecated
    public List<SingleUrlResult> executeMultithreading() {
        MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
        List<SingleUrlResult> resultList = new ArrayList<>();
        List<SingleUrlResult> totalPageList = this.crawlData();
        //processing
        //start time
        long start = System.currentTimeMillis();
        int threadSize = PROCESSED_PER_THREAD;
        //total amount of pages
        int dataSize = totalPageList.size();
        //number of threads
        int threadNum = dataSize / threadSize + 1;
        //filter out integer threadNum
        boolean special = dataSize % threadSize == 0;

        //set a thread pool
        ExecutorService exec = Executors.newFixedThreadPool(threadNum);
        //set a task set
        List<Callable<List<SingleUrlResult>>> tasks = new ArrayList<>();
        Callable<List<SingleUrlResult>> task;
        List<SingleUrlResult> currentList;

        //define the amount of pages for each thread
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                currentList = totalPageList.subList(threadSize * i, dataSize);
            } else {
                currentList = totalPageList.subList(threadSize * i, threadSize * (i + 1));
            }
            final List<SingleUrlResult> list = currentList;
            task = new Callable<List<SingleUrlResult>>() {

                @Override
                public List<SingleUrlResult> call() throws Exception {
                    for (SingleUrlResult result : list) {
                        if (result.getCategory().equals(CategoryEnum.EVENTS.getName())) {
                            refillDetail(result, CSS_QUERY_EVENTS);
                        } else if (result.getCategory().equals(CategoryEnum.MATERIALS.getName()) || result.getCategory().equals(CategoryEnum.E_LEARNING.getName())) {
                            refillDetail(result, CSS_QUERY_MATERIALS_AND_E_LEARNING);
                        } else if (result.getCategory().equals(CategoryEnum.WORKFLOWS.getName())) {
                            refillDetail(result, CSS_QUERY_WORKFLOWS);
                        }
                        resultList.add(result);
                    }
                    System.out.println(Thread.currentThread().getName() + "线程：" + list);
                    return list;
                }
            };
            tasks.add(task);
        }

        try {
            jdbc.dropTable();
            jdbc.createTable();
            List<Future<List<SingleUrlResult>>> results = exec.invokeAll(tasks);
            for (Future<List<SingleUrlResult>> future : results) {
                List<SingleUrlResult> singleUrlResults = future.get();
                this.saveDeadLink2DB(jdbc, singleUrlResults);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //close the thread pool
            exec.shutdown();
            System.out.println("End of The Task");
            System.err.println("Time consuming: " + (System.currentTimeMillis() - start) / 1000 + "second");
        }

        return resultList;
    }

    /**
     * Save broken links to Database
     *
     * @param jdbc
     * @param singleUrlResults
     */
    private void saveDeadLink2DB(MySqlJDBCImpl jdbc, List<SingleUrlResult> singleUrlResults) {
        if (singleUrlResults != null && singleUrlResults.size() > 0) {
            for (SingleUrlResult major : singleUrlResults) {
                if (major.getDetailTargetStatus() != 200 && major.getDetailTargetUrl() != null) {
                    jdbc.saveDeadLinkRecord(this.buildDeadLinkRecords(major, "major"));
                }
                if (major.getMinorList() != null && major.getMinorList().size() > 0) {
                    for (SingleUrlResult minor : major.getMinorList()) {
                        if (minor.getDetailTargetStatus() != 200 && minor.getDetailTargetUrl() != null) {
                            jdbc.saveDeadLinkRecord(this.buildDeadLinkRecords(minor, "minor"));
                        }
                    }
                }
            }
        }
    }

    /**
     * Save broken domain to Database
     *
     * @param jdbc
     * @param infoList
     */
    private void saveDeadLinkDomain2DB(MySqlJDBCImpl jdbc, List<CacheInfo> infoList) {
        if (infoList != null && infoList.size() > 0) {
            for (CacheInfo obj : infoList) {
                jdbc.saveDeadLinkDomain(this.buildDeadLinkDomain(jdbc, obj));
            }
        }
    }

    /**
     * Build DeadLinkRecords
     *
     * @param result
     * @return
     */
    private DeadLinkRecords buildDeadLinkRecords(SingleUrlResult result, String type) {
        DeadLinkRecords model = new DeadLinkRecords();
        model.setDeadLink(result.getDetailTargetUrl());
        model.setDeadLinkTitle(result.getDetailTargetTitle());
        model.setStatusCode(result.getDetailTargetStatus());
        model.setReasonPhrase(result.getDetailTargetReasonPhrase());
        model.setPage(result.getListIndex());
        model.setParentUrl(result.getDetailUrl());
        model.setCategory(result.getCategory());
        model.setDomainUrl(this.getMainUrl(model.getDeadLink()));
        model.setType(type);
        model.setStart(result.getStart());
        model.setEnd(result.getEnd());
        model.setDuration(result.getDuration());
        model.setCreateTime(new Date());
        return model;
    }

    /**
     * Build DeadLinkDomain
     *
     * @param jdbc
     * @param cache
     * @return
     */
    private DeadLinkDomain buildDeadLinkDomain(MySqlJDBCImpl jdbc, CacheInfo cache) {
        DeadLinkDomain model = new DeadLinkDomain();
        model.setDomainUrl(cache.getUrl());
        model.setStatusCode(cache.getHttpStatus());
        model.setReasonPhrase(cache.getReasonPhrase());
        model.setLinkNumber(jdbc.countDeadLinkRecordsByDomain(cache.getUrl()));
        model.setPage(cache.getPage());
        model.setDetailLink(cache.getDetailLink());
        model.setDeadLinkTitle(cache.getDeadLinkTitle());
        model.setCreateTime(new Date());
        return model;
    }

    /**
     * Save overall data
     *
     * @param jdbc
     */
    private void saveCollectInfo(MySqlJDBCImpl jdbc) {
        jdbc.deleteCollectInfo(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE));
        CollectInfo collectInfo = new CollectInfo();
        collectInfo.setEvents(totalEvents);
        collectInfo.setEventsDead(jdbc.countDeadLinkRecordsByCategory(CategoryEnum.EVENTS.getName()));
        collectInfo.setMaterials(totalMaterials);
        collectInfo.setMaterialsDead(jdbc.countDeadLinkRecordsByCategory(CategoryEnum.MATERIALS.getName()));
        collectInfo.setElearning(totalElearning);
        collectInfo.setElearningDead(jdbc.countDeadLinkRecordsByCategory(CategoryEnum.E_LEARNING.getName()));
        collectInfo.setWorkflows(totalWorkflows);
        collectInfo.setWorkflowsDead(jdbc.countDeadLinkRecordsByCategory(CategoryEnum.WORKFLOWS.getName()));
        collectInfo.setDomainDead(totalDomainDead);
        collectInfo.setCreateDate(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE));
        jdbc.saveCollectInfo(collectInfo);
    }

    /**
     * Crawl all data
     *
     * @return
     */
    private List<SingleUrlResult> crawlData() {
        List<SingleUrlResult> totalPageList = new ArrayList<>();
        boolean eventsContinue = true;
        boolean materialsContinue = true;
        boolean eLearningContinue = true;
        //Step 1: Crawl all pages
        for (int i = PAGE_START; i <= TOTAL_PAGE; i++) {
            List<SingleUrlResult> singlePageListEvents = null;
            if (eventsContinue) {
                singlePageListEvents = this.getEventsList(i);
            }
            List<SingleUrlResult> singlePageListMaterials = null;
            if (materialsContinue) {
                singlePageListMaterials = this.getMaterialsOrELearningList(i, CategoryEnum.MATERIALS);
            }
            List<SingleUrlResult> singlePageListELearning = null;
            if (eLearningContinue) {
                singlePageListELearning = this.getMaterialsOrELearningList(i, CategoryEnum.E_LEARNING);
            }

            //Events
            if (singlePageListEvents != null && singlePageListEvents.size() > 0) {
                totalPageList.addAll(singlePageListEvents);
                totalEvents = totalEvents + singlePageListEvents.size();
                logger.info("***********Events data {}, current at page {}***********", singlePageListEvents.size(), i);
            } else {
                eventsContinue = false;
            }
            //Materials
            if (singlePageListMaterials != null && singlePageListMaterials.size() > 0) {
                totalPageList.addAll(singlePageListMaterials);
                totalMaterials = totalMaterials + singlePageListMaterials.size();
                logger.info("***********Materials data {}, current at page {}***********", singlePageListMaterials.size(), i);
            } else {
                materialsContinue = false;
            }
            //E-Learning
            if (singlePageListELearning != null && singlePageListELearning.size() > 0) {
                totalPageList.addAll(singlePageListELearning);
                totalElearning = totalElearning + singlePageListELearning.size();
                logger.info("***********E-Learning data {}, current at page {}***********", singlePageListELearning.size(), i);
            } else {
                eLearningContinue = false;
            }
            //Jump out the loop if there are no more pages
            if (!eventsContinue && !materialsContinue && !eLearningContinue) {
                break;
            }
        }
        List<SingleUrlResult> singlePageListWorkflows = this.getWorkflowsList();
        //Workflows
        if (singlePageListWorkflows.size() > 0) {
            totalPageList.addAll(singlePageListWorkflows);
            totalWorkflows = singlePageListWorkflows.size();
            logger.info("***********Workflows data {}***********", singlePageListWorkflows.size());
        }
        logger.info("***********Overall data {}, Events {}, Materials {}, ELearning {}, Wrokflows {}***********", totalPageList.size(), totalEvents, totalMaterials, totalElearning, totalWorkflows);
        return totalPageList;
    }

    /**
     * crawl data from Events page
     *
     * @param index
     * @return
     */
    public List<SingleUrlResult> getEventsList(int index) {

        String url = WEB_SITE + "/events?include_expired=true&page=" + index;

        HttpResult listHttpResult = HttpUtils.getSingleHttpWithRetry(url, RETRY_TIMES);
        if (!listHttpResult.isSuccess() || listHttpResult.getHttpStatus() != 200) {
            return null;
        }

        String httpContent = listHttpResult.getHttpContent();

        final List<SingleUrlResult> retList = new ArrayList<>();

        Document doc = Jsoup.parse(httpContent);
        //jQuery
        //label: div，ul，li
        //id: #
        //class:.
        //next level: >
        //Example: <div id="home"> to <ul class="media-grid"> to <li class="media-item">
        Elements elements = doc.select("div#home > ul.media-grid > li.media-item");
        for (Element element : elements) {

            //for a same detail page:
            //its 'title' and 'url' have different labels
            //but their parent label are all <li>
            //thus，loop multiple detail pages, loop can only arrive <li>
            //here elements is <li>
            SingleUrlResult singleUrlResult = new SingleUrlResult();
            singleUrlResult.setCategory(CategoryEnum.EVENTS.getName());
            singleUrlResult.setListIndex(index);
            singleUrlResult.setListTitle(element.select("#event-title").text());
            singleUrlResult.setDetailUrl(WEB_SITE + element.select("a.media-view").attr("href"));
            retList.add(singleUrlResult);
        }
        return retList;
    }

    public void refillDetail(SingleUrlResult singleUrlResult, String cssQuery) {
        //get detail pages
        String detailUrl = singleUrlResult.getDetailUrl();
        HttpResult detailHttpResult = HttpUtils.getSingleHttpWithRetry(detailUrl, RETRY_TIMES);
        singleUrlResult.setDetailHttpStatus(detailHttpResult.getHttpStatus());
        if ((!detailHttpResult.isSuccess()) || detailHttpResult.getHttpStatus() != 200) {
            logger.error("Detail page broken !!!, singleUrlResult={}", singleUrlResult);
            singleUrlResult.setDetailTargetUrl(detailUrl);
            singleUrlResult.setDetailTargetStatus(detailHttpResult.getHttpStatus());
            singleUrlResult.setDetailTargetReasonPhrase("internal error");
            return;
        }
        //parse detail pages, get the target url
        String detailHttpContent = detailHttpResult.getHttpContent();
        Document doc = Jsoup.parse(detailHttpContent);
        Elements elements = doc.select(cssQuery);
        boolean major = true;
        List<SingleUrlResult> minorList = new ArrayList<>();
        for (Element a : elements) {
            if (a != null) {
                if (major) {
                    //major: the first <a>
                    major = false;
                    this.refillMajor(singleUrlResult, a);
                } else {
                    //minor: remaining <a>
                    this.refillMinor(singleUrlResult, minorList, a);
                }
            }
        }
        if (minorList.size() > 0) {
            singleUrlResult.setMinorList(minorList);
        }

        //get the time for Events
        if (singleUrlResult.getCategory().equals(CategoryEnum.EVENTS.getName())) {
            this.refillStartAndEnd(singleUrlResult, doc);
        }
    }

    /**
     * refill Major data
     *
     * @param singleUrlResult
     * @param a
     */
    private void refillMajor(SingleUrlResult singleUrlResult, Element a) {
        singleUrlResult.setDetailTargetTitle(a.text());
        singleUrlResult.setDetailTargetUrl(a.attr("href"));
        String majorUrl = singleUrlResult.getDetailTargetUrl();
        //get the domain of the target url
        String mainUrl = this.getMainUrl(majorUrl);
        //if the domain is in the black list, mark it!
        if (this.isOnTheBlacklist(mainUrl)) {
            singleUrlResult.setDetailTargetStatus(BLACK_LIST_CODE);
            singleUrlResult.setDetailTargetReasonPhrase(BLACK_LIST_MSG);
        }
        else {
            CacheInfo mainUrlCache = MAIN_URL_DEAD_MAP.get(mainUrl);
            if (mainUrlCache == null) {
                //the domain is not in the broken domain map, continue to check
                HttpResult majorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(majorUrl, RETRY_TIMES);
                singleUrlResult.setDetailTargetStatus(majorUrlHttpResult.getHttpStatus());
                singleUrlResult.setDetailTargetReasonPhrase(majorUrlHttpResult.getReasonPhrase());
                //if the path failed, check its domain
                if (mainUrl != null && majorUrlHttpResult.getHttpStatus() != 200) {
                    HttpResult mainUrlResult = HttpUtils.getSingleHttpWithRetry(mainUrl, RETRY_TIMES);
                    if (mainUrlResult.getHttpStatus() != 200) {
                        //if domain failed, and it has not been recorded, then add it to the broken domain map
                        MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, mainUrlResult.getHttpStatus(), mainUrlResult.getReasonPhrase(),
                                singleUrlResult.getListIndex(), singleUrlResult.getDetailUrl(), singleUrlResult.getDetailTargetTitle()));
                        logger.info("major RECORDING MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
                    }
                }
            } else {
                //if domain is in the broken domain map, use the fixed status code
                singleUrlResult.setDetailTargetStatus(mainUrlCache.getHttpStatus());
                singleUrlResult.setDetailTargetReasonPhrase(mainUrlCache.getReasonPhrase());
            }
        }
    }

    /**
     * refill Minor data
     *
     * @param singleUrlResult
     * @param minorList
     * @param a
     */
    private void refillMinor(SingleUrlResult singleUrlResult, List<SingleUrlResult> minorList, Element a) {
        SingleUrlResult minor = new SingleUrlResult();
        minor.setCategory(singleUrlResult.getCategory());
        minor.setListIndex(singleUrlResult.getListIndex());
        minor.setListTitle(singleUrlResult.getListTitle());
        minor.setDetailUrl(singleUrlResult.getDetailUrl());
        minor.setDetailHttpStatus(singleUrlResult.getDetailHttpStatus());
        minor.setDetailTargetTitle(a.text());
        minor.setDetailTargetUrl(a.attr("href"));
        String minorUrl = minor.getDetailTargetUrl();
        //get the domain of the target url
        String mainUrl = this.getMainUrl(minorUrl);
        //if the domain is in the black list, mark it!
        if (this.isOnTheBlacklist(mainUrl)) {
            minor.setDetailTargetStatus(BLACK_LIST_CODE);
            minor.setDetailTargetReasonPhrase(BLACK_LIST_MSG);
        }
        else {
            CacheInfo mainUrlCache = MAIN_URL_DEAD_MAP.get(mainUrl);
            if (mainUrlCache == null) {
                //the domain is not in the broken domain map, continue to check
                HttpResult minorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(minorUrl, RETRY_TIMES);
                minor.setDetailTargetStatus(minorUrlHttpResult.getHttpStatus());
                minor.setDetailTargetReasonPhrase(minorUrlHttpResult.getReasonPhrase());
                //if the path failed, check its domain
                if (mainUrl != null && minorUrlHttpResult.getHttpStatus() != 200) {
                    HttpResult mainUrlResult = HttpUtils.getSingleHttpWithRetry(mainUrl, RETRY_TIMES);
                    if (mainUrlResult.getHttpStatus() != 200) {
                        //if domain failed, and it has not been recorded, then add it to the broken domain map
                        MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, mainUrlResult.getHttpStatus(), mainUrlResult.getReasonPhrase(),
                                singleUrlResult.getListIndex(), singleUrlResult.getDetailUrl(), minor.getDetailTargetTitle()));
                        logger.info("minor RECORDING    MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
                    }
                }
            } else {
                //if domain is in the broken domain map, use the fixed status code
                minor.setDetailTargetStatus(mainUrlCache.getHttpStatus());
                minor.setDetailTargetReasonPhrase(mainUrlCache.getReasonPhrase());
            }
        }
        minorList.add(minor);
    }

    /**
     *  refill the starting and ending time for Events
     *
     * @param singleUrlResult
     * @param doc
     */
    private void refillStartAndEnd(SingleUrlResult singleUrlResult, Document doc) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy '@' HH:mm", Locale.UK);
        Elements timeElement = doc.select("div.dont-break-out > p");
        Date start = null;
        Date end = null;
        for (Element p : timeElement) {
            String str = p.text();
            if (start != null && end != null) {
                break;
            }
            try {
                if (str.contains("Start")) {
                    start = format.parse(p.ownText());
                    singleUrlResult.setStart(p.ownText());
                } else if (str.contains("End")) {
                    end = format.parse(p.ownText());
                    singleUrlResult.setEnd(p.ownText());
                }
            } catch (ParseException e) {
                logger.error("ParseException", e);
            }
        }
        if (start != null && end != null) {
            int duration = DateUtils.intervalDays(start, end);
            if (duration == 0) {
                singleUrlResult.setDuration("1");
            } else {
                singleUrlResult.setDuration(Integer.toString(duration));
            }
        }
    }

    /**
     * Check if in the Black list
     *
     * @param domainUrl
     * @return
     */
    private boolean isOnTheBlacklist(String domainUrl) {
        if (blackListDomains == null) {
            MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
            blackListDomains = jdbc.selectBlackListDomain();
        }
        for (BlackListDomain obj : blackListDomains) {
            if (obj.getDomainUrl().equals(domainUrl)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get domain
     *
     * @param url
     * @return
     */
    private String getMainUrl(String url) {
        if (url == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(MAIN_URL_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Crawl data from Materials or E-Learning pages
     *
     * @param index
     * @param categoryEnum
     * @return
     */
    public List<SingleUrlResult> getMaterialsOrELearningList(int index, CategoryEnum categoryEnum) {
        String url = null;
        if (categoryEnum.equals(CategoryEnum.MATERIALS)) {
            url = WEB_SITE + "/materials?page=" + index;
        } else {
            url = WEB_SITE + "/elearning_materials?page=" + index;
        }

        HttpResult listHttpResult = HttpUtils.getSingleHttpWithRetry(url, RETRY_TIMES);
        if (!listHttpResult.isSuccess() || listHttpResult.getHttpStatus() != 200) {
            return null;
        }
        String httpContent = listHttpResult.getHttpContent();
        final List<SingleUrlResult> retList = new ArrayList<>();
        Document doc = Jsoup.parse(httpContent);
        Elements elements = doc.select("div#content > div.list-card");
        for (Element element : elements) {
            SingleUrlResult singleUrlResult = new SingleUrlResult();
            singleUrlResult.setCategory(categoryEnum.getName());
            singleUrlResult.setListIndex(index);
            singleUrlResult.setListTitle(element.select("a.list-card-heading").text());
            singleUrlResult.setDetailUrl(WEB_SITE + element.select("a.list-card-heading").attr("href"));
            retList.add(singleUrlResult);
        }
        return retList;
    }

    /**
     * Crawl data from Workflows
     *
     * @return
     */
    public List<SingleUrlResult> getWorkflowsList() {
        String url = WEB_SITE + "/workflows";

        HttpResult listHttpResult = HttpUtils.getSingleHttpWithRetry(url, RETRY_TIMES);
        if (!listHttpResult.isSuccess() || listHttpResult.getHttpStatus() != 200) {
            return null;
        }
        String httpContent = listHttpResult.getHttpContent();
        final List<SingleUrlResult> retList = new ArrayList<>();
        Document doc = Jsoup.parse(httpContent);
        Elements elements = doc.select("div#content > ul.media-grid > li.media-item");
        for (Element element : elements) {
            SingleUrlResult singleUrlResult = new SingleUrlResult();
            singleUrlResult.setCategory(CategoryEnum.WORKFLOWS.getName());
            singleUrlResult.setListIndex(1);
            singleUrlResult.setListTitle(element.select("h3.media-heading").text());
            singleUrlResult.setDetailUrl(WEB_SITE + element.select("a.media-view").attr("href"));
            retList.add(singleUrlResult);
        }
        return retList;
    }

}