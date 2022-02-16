package yanan.zhang;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final int TOTAL_PAGE = 1000;
    private static final int PROCESSED_PER_THREAD = 200;
    public static final String CSS_QUERY_EVENTS = "div.page-header > p > a";
    public static final String CSS_QUERY_MATERIALS_AND_E_LEARNING = "div.text-justify a";
    public static final String CSS_QUERY_WORKFLOWS = "div#workflow-diagram-sidebar a";
    /**
     * 正则表达式：过滤主域名
     *
     * https://tool.chinaz.com/tools/regexgenerate
     */
    public static final String MAIN_URL_REGEX = "^((https|http|ftp|rtsp|mms)?:\\/\\/)[^\\/]+";
    /**
     * 记录主域名是死链的缓存
     */
    public static Map<String, CacheInfo> MAIN_URL_DEAD_MAP = new HashMap<>();



    /**
     * 单线程方式运行
     *
     * @return
     */
    public List<SingleUrlResult> execute() {
        List<SingleUrlResult> resultList = new ArrayList<>();
        List<SingleUrlResult> totalPageList = this.crawlData();
        //四舍五入保留两位小数
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
        MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
        jdbc.dropTable();
        jdbc.createTable();
        this.saveDeadLink2DB(jdbc, resultList);
        //保存主域名死链数据
        if (MAIN_URL_DEAD_MAP.size() > 0) {
            jdbc.dropTableDomain();
            jdbc.createTableDomain();
            this.saveDeadLinkDomain2DB(jdbc, new ArrayList<>(MAIN_URL_DEAD_MAP.values()));
        }

        return resultList;
    }


//    public List<SingleUrlResult> execute() {
//
//        List<SingleUrlResult> retList = new ArrayList<>();
//
//        for (int i = 1; i <= TOTAL_PAGE; i++) {
//            List<SingleUrlResult> singlePageList = getEventsList(i);
//            if (singlePageList == null || singlePageList.isEmpty()) {
//                break;
//            }
//            for (SingleUrlResult result : singlePageList) {
//                this.refillDetail(result, CSS_QUERY_EVENTS);
//                retList.add(result);
//            }
//        }
//
//        return retList;
//    }

    /**
     * 多线程方式运行
     *
     * @return
     */
    public List<SingleUrlResult> executeMultithreading() {

        List<SingleUrlResult> resultList = new ArrayList<>();
        List<SingleUrlResult> totalPageList = this.crawlData();
//        // craw all data
//        for (int i = 1; i <= TOTAL_PAGE; i++) {
//            List<SingleUrlResult> singlePageListEvents = this.getEventsList(i);
//            List<SingleUrlResult> singlePageListMaterials = this.getMaterialsOrELearningList(i, CategoryEnum.MATERIALS);
//            List<SingleUrlResult> singlePageListELearning = this.getMaterialsOrELearningList(i, CategoryEnum.E_LEARNING);
//
//            //Events
//            if (singlePageListEvents != null && singlePageListEvents.size() > 0) {
//                totalPageList.addAll(singlePageListEvents);
//                System.out.println("***********爬到Events数据" + singlePageListEvents.size() + "条***********");
//            }
//            //Materials
//            if (singlePageListMaterials != null && singlePageListMaterials.size() > 0) {
//                totalPageList.addAll(singlePageListMaterials);
//                System.out.println("***********爬到Materials数据" + singlePageListMaterials.size() + "条***********");
//            }
//            //E-Learning
//            if (singlePageListELearning != null && singlePageListELearning.size() > 0) {
//                totalPageList.addAll(singlePageListELearning);
//                System.out.println("***********爬到E-Learning数据" + singlePageListELearning.size() + "条***********");
//            }
//        }
//        List<SingleUrlResult> singlePageListWorkflows = this.getWorkflowsList();
//        //Workflows
//        if (singlePageListWorkflows != null && singlePageListWorkflows.size() > 0) {
//            totalPageList.addAll(singlePageListWorkflows);
//            System.out.println("***********爬到Workflows数据" + singlePageListWorkflows.size() + "条***********");
//        }
//        System.out.println("***********共计爬取到数据" + totalPageList.size() + "条***********");

        // multithreading
        // the start time
        long start = System.currentTimeMillis();
        // a new thread for every 50 data
        int threadSize = PROCESSED_PER_THREAD;
        // total amount of the data
        int dataSize = totalPageList.size();
        // total amount of the thread
        int threadNum = dataSize / threadSize + 1;
        // define the thread number as an integer
        boolean special = dataSize % threadSize == 0;

        // create a thread pool
        ExecutorService exec = Executors.newFixedThreadPool(threadNum);
        // define a task set
        List<Callable<List<SingleUrlResult>>> tasks = new ArrayList<>();
        Callable<List<SingleUrlResult>> task;
        List<SingleUrlResult> currentList;

        // define data for every thread
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                currentList = totalPageList.subList(threadSize * i, dataSize);
            } else {
                currentList = totalPageList.subList(threadSize * i, threadSize * (i + 1));
            }
            // System.out.println((i + 1) + "thread：" + currentList.toString());
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
            // The list of task containers submitted here and the returned Future list have an orderly correspondence
            tasks.add(task);
        }

        try {
            MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
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
            // close the thread pool
            exec.shutdown();
            System.out.println("线程任务执行结束");
            System.err.println("执行任务消耗了: " + (System.currentTimeMillis() - start) / 1000 + "秒");
        }

        return resultList;
    }

    /**
     * 把死链数据存库
     *
     * @param jdbc
     * @param singleUrlResults
     */
    private void saveDeadLink2DB(MySqlJDBCImpl jdbc, List<SingleUrlResult> singleUrlResults) {
        if (singleUrlResults != null && singleUrlResults.size() > 0) {
            for (SingleUrlResult major : singleUrlResults) {
                if (major.getDetailTargetStatus() != 200 && major.getDetailTargetUrl() != null) {
                    jdbc.saveDeadLinkRecord(this.buildDeadLinkRecords(major));
                    if (major.getMinorList() != null && major.getMinorList().size() > 0) {
                        for (SingleUrlResult minor : major.getMinorList()) {
                            if (minor.getDetailTargetStatus() != 200 && minor.getDetailTargetUrl() != null) {
                                jdbc.saveDeadLinkRecord(this.buildDeadLinkRecords(minor));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 把主域名死链数据存库
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
     * 组装DeadLinkRecords
     *
     * @param result
     * @return
     */
    private DeadLinkRecords buildDeadLinkRecords(SingleUrlResult result) {
        DeadLinkRecords model = new DeadLinkRecords();
        model.setDeadLink(result.getDetailTargetUrl());
        model.setStatusCode(result.getDetailTargetStatus());
        model.setReasonPhrase(result.getDetailTargetReasonPhrase());
        model.setPage(result.getListIndex());
        model.setParentUrl(result.getDetailUrl());
        model.setCategory(result.getCategory());
        model.setDomainUrl(this.getMainUrl(model.getDeadLink()));
        model.setCreateTime(new Date());
        return model;
    }

    /**
     * 组装DeadLinkDomain
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
        model.setCreateTime(new Date());
        return model;
    }


    /**
     * crawl all data
     *
     * @return
     */
    private List<SingleUrlResult> crawlData() {
        List<SingleUrlResult> totalPageList = new ArrayList<>();
        boolean eventsContinue = true;
        boolean materialsContinue = true;
        boolean eLearningContinue = true;
        // crawl all data
        for (int i = 1; i <= TOTAL_PAGE; i++) {
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
                logger.info("***********爬到Events数据{}条, 当前第{}页***********", singlePageListEvents.size(), i);
            } else {
                eventsContinue = false;
            }
            //Materials
            if (singlePageListMaterials != null && singlePageListMaterials.size() > 0) {
                totalPageList.addAll(singlePageListMaterials);
                logger.info("***********爬到Materials数据{}条, 当前第{}页***********", singlePageListMaterials.size(), i);
            } else {
                materialsContinue = false;
            }
            //E-Learning
            if (singlePageListELearning != null && singlePageListELearning.size() > 0) {
                totalPageList.addAll(singlePageListELearning);
                logger.info("***********爬到E-Learning数据{}条, 当前第{}页***********", singlePageListELearning.size(), i);
            } else {
                eLearningContinue = false;
            }
            // if there is no more data, jump out
            if (!eventsContinue && !materialsContinue && !eLearningContinue) {
                break;
            }
        }
        List<SingleUrlResult> singlePageListWorkflows = this.getWorkflowsList();
        //Workflows
        if (singlePageListWorkflows.size() > 0) {
            totalPageList.addAll(singlePageListWorkflows);
            logger.info("***********爬到Workflows数据{}条***********", singlePageListWorkflows.size());
        }
        logger.info("***********共计爬取到数据{}条***********", totalPageList.size());
        return totalPageList;
    }


    /**
     * Visit a separate page (event）
     *
     * @param index
     * @return
     */
    public List<SingleUrlResult> getEventsList(int index) {

        String url = WEB_SITE + "/events?include_expired=true&page=" + index;

        HttpResult listHttpResult = HttpUtils.getSingleHttpWithRetry(url, 3);
        if (!listHttpResult.isSuccess() || listHttpResult.getHttpStatus() != 200) {
            return null;
        }

        String httpContent = listHttpResult.getHttpContent();

        final List<SingleUrlResult> retList = new ArrayList<>();

        Document doc = Jsoup.parse(httpContent);
        // #: id
        // .: class
        //<div id="home"> -- <ul class="media-grid"> -- <li class="media-item">
        Elements elements = doc.select("div#home > ul.media-grid > li.media-item");
        for (Element element : elements) {

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

        //visit the detail page
        String detailUrl = singleUrlResult.getDetailUrl();

        HttpResult detailHttpResult = HttpUtils.getSingleHttpWithRetry(detailUrl, 3);
        singleUrlResult.setDetailHttpStatus(detailHttpResult.getHttpStatus());
        if ((!detailHttpResult.isSuccess()) || detailHttpResult.getHttpStatus() != 200) {
            return;
        }

        //parse the detail page and get the target link
        String detailHttpContent = detailHttpResult.getHttpContent();
        Document doc = Jsoup.parse(detailHttpContent);

        //find 'a' which is the target link
        Elements elements = doc.select(cssQuery);
        boolean major = true;
        List<SingleUrlResult> minorList = new ArrayList<>();
        for (Element a : elements) {
            if (a != null) {
                if (major) {
                    //只有第一个a标签是major
                    major = false;
                    singleUrlResult.setDetailTargetTitle(a.text());
                    singleUrlResult.setDetailTargetUrl(a.attr("href"));
                    String majorUrl = singleUrlResult.getDetailTargetUrl();
                    //获取目标url的主域名
                    String mainUrl = this.getMainUrl(majorUrl);
                    CacheInfo mainUrlCache = MAIN_URL_DEAD_MAP.get(mainUrl);
                    if (mainUrlCache == null) {
                        //主域名没在缓存map中就正常请求获取结果
                        HttpResult majorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(majorUrl, 3);
                        singleUrlResult.setDetailTargetStatus(majorUrlHttpResult.getHttpStatus());
                        singleUrlResult.setDetailTargetReasonPhrase(majorUrlHttpResult.getReasonPhrase());
                        //如果请求不成功，则单独请求主域名
                        if (mainUrl != null && majorUrlHttpResult.getHttpStatus() != 200) {
                            HttpResult mainUrlResult = HttpUtils.getSingleHttpWithRetry(mainUrl, 3);
                            if (mainUrlResult.getHttpStatus() != 200) {
                                //如果主域名都访问不成功，而且主域名在MAIN_URL_DEAD_MAP中不存在就放进去
                                MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, mainUrlResult.getHttpStatus(), mainUrlResult.getReasonPhrase()));
                                logger.info("缓存MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
                            }
                        }
                    } else {
                        //主域名在缓存map中就把主域名的statusCode放进去
                        singleUrlResult.setDetailTargetStatus(mainUrlCache.getHttpStatus());
                        singleUrlResult.setDetailTargetReasonPhrase(mainUrlCache.getReasonPhrase());
                    }
                } else {
                    //其余都是minor
                    SingleUrlResult minor = new SingleUrlResult();
                    minor.setCategory(singleUrlResult.getCategory());
                    minor.setListIndex(singleUrlResult.getListIndex());
                    minor.setListTitle(singleUrlResult.getListTitle());
                    minor.setDetailUrl(singleUrlResult.getDetailUrl());
                    minor.setDetailHttpStatus(singleUrlResult.getDetailHttpStatus());
                    minor.setDetailTargetTitle(a.text());
                    minor.setDetailTargetUrl(a.attr("href"));
                    String minorUrl = minor.getDetailTargetUrl();
                    //获取目标url的主域名
                    String mainUrl = this.getMainUrl(minorUrl);
                    CacheInfo mainUrlCache = MAIN_URL_DEAD_MAP.get(mainUrl);
                    if (mainUrlCache == null) {
                        //主域名没在缓存map中就正常请求获取结果
                        HttpResult minorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(minorUrl, 3);
                        minor.setDetailTargetStatus(minorUrlHttpResult.getHttpStatus());
                        //如果请求不成功，则单独请求主域名
                        if (mainUrl != null && minorUrlHttpResult.getHttpStatus() != 200) {
                            HttpResult mainUrlResult = HttpUtils.getSingleHttpWithRetry(mainUrl, 3);
                            if (mainUrlResult.getHttpStatus() != 200) {
                                //如果主域名都访问不成功，而且主域名在MAIN_URL_DEAD_MAP中不存在就放进去
                                MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, mainUrlResult.getHttpStatus(), mainUrlResult.getReasonPhrase()));
                            }
                        }
                    } else {
                        //主域名在缓存map中就把主域名的statusCode放进去
                        minor.setDetailTargetStatus(mainUrlCache.getHttpStatus());
                        minor.setDetailTargetReasonPhrase(mainUrlCache.getReasonPhrase());
                    }
                    minorList.add(minor);
                }
            }
        }
        if (minorList.size() > 0) {
            singleUrlResult.setMinorList(minorList);
        }
    }


    /**
     * 获取主域名
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
     * Visit a separate page (material & e-learning）
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

        HttpResult listHttpResult = HttpUtils.getSingleHttpWithRetry(url, 3);
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
     * Visit a separate page (workflows）
     *
     * @return
     */
    public List<SingleUrlResult> getWorkflowsList() {
        String url = WEB_SITE + "/workflows";

        HttpResult listHttpResult = HttpUtils.getSingleHttpWithRetry(url, 3);
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

