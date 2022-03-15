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
     * 从第几页开始爬
     */
    private static final int PAGE_START = 1;
    /**
     * 一共爬多少页
     */
    private static final int TOTAL_PAGE = 1000;
    /**
     * 每条线程处理数量
     */
    private static final int PROCESSED_PER_THREAD = 200;
    /**
     * 重试次数
     */
    private static final int RETRY_TIMES = 3;
    public static final String CSS_QUERY_EVENTS = "div.page-header > p > a";
    public static final String CSS_QUERY_MATERIALS_AND_E_LEARNING = "div.text-justify a";
    public static final String CSS_QUERY_WORKFLOWS = "div#workflow-diagram-sidebar a";
    /**
     * 正则表达式：过滤主域名
     *
     * 授妹妹以鱼不如授妹妹以渔，正则表达式在线生成工具：https://tool.chinaz.com/tools/regexgenerate
     */
    public static final String MAIN_URL_REGEX = "^((https|http|ftp|rtsp|mms)?:\\/\\/)[^\\/]+";
    /**
     * 记录主域名是死链的缓存
     */
    public static Map<String, CacheInfo> MAIN_URL_DEAD_MAP = new HashMap<>();

    /**
     * 黑名单http status code
     */
    private static final int BLACK_LIST_CODE = 1;

    /**
     * 黑名单reason phrase
     */
    private static final String BLACK_LIST_MSG = "on the black list";

    /**
     * 黑名单http status code
     */
    private static final int ILLEGAL_URL_CODE = 2;

    /**
     * 黑名单reason phrase
     */
    private static final String ILLEGAL_URL_MSG = "illegal url";

    private List<BlackListDomain> blackListDomains = null;

    private List<String> legalSuffixList = null;

    int totalEvents = 0;
    int totalEventsDead = 0;
    int totalMaterials = 0;
    int totalMaterialsDead = 0;
    int totalElearning = 0;
    int totalElearningDead = 0;
    int totalWorkflows = 0;
    int totalWorkflowsDead = 0;
    int totalDomainDead = 0;

    /**
     * 单线程方式运行
     *
     * @return
     */
    public List<SingleUrlResult> execute() {
        MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
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
        jdbc.dropTable();
        jdbc.createTable();
        this.saveDeadLink2DB(jdbc, resultList);
        //保存主域名死链数据
        if (MAIN_URL_DEAD_MAP.size() > 0) {
            jdbc.dropTableDomain();
            jdbc.createTableDomain();
            this.saveDeadLinkDomain2DB(jdbc, new ArrayList<>(MAIN_URL_DEAD_MAP.values()));
            totalDomainDead = MAIN_URL_DEAD_MAP.size();
        }
        //保存汇总数据
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
     * 多线程方式运行
     *
     * @return
     */
    @Deprecated
    public List<SingleUrlResult> executeMultithreading() {
        MySqlJDBCImpl jdbc = new MySqlJDBCImpl();
        List<SingleUrlResult> resultList = new ArrayList<>();
        List<SingleUrlResult> totalPageList = this.crawlData();
        // 多线程处理
        // 开始时间
        long start = System.currentTimeMillis();
        // 每50条数据开启一条线程
        int threadSize = PROCESSED_PER_THREAD;
        // 总数据条数
        int dataSize = totalPageList.size();
        // 线程数
        int threadNum = dataSize / threadSize + 1;
        // 定义标记,过滤threadNum为整数
        boolean special = dataSize % threadSize == 0;

        // 创建一个线程池
        ExecutorService exec = Executors.newFixedThreadPool(threadNum);
        // 定义一个任务集合
        List<Callable<List<SingleUrlResult>>> tasks = new ArrayList<>();
        Callable<List<SingleUrlResult>> task;
        List<SingleUrlResult> currentList;

        // 确定每条线程的数据
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                currentList = totalPageList.subList(threadSize * i, dataSize);
            } else {
                currentList = totalPageList.subList(threadSize * i, threadSize * (i + 1));
            }
            // System.out.println("第" + (i + 1) + "组：" + currentList.toString());
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
            // 这里提交的任务容器列表和返回的Future列表存在顺序对应的关系
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
            // 关闭线程池
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
                    if (major.getDetailTargetStatus() != BLACK_LIST_CODE) {
                        //只统计不是黑名单域名的死链数
                        this.collectCategoryDeadNumber(major);
                    }
                    jdbc.saveDeadLinkRecord(this.buildDeadLinkRecords(major, "major"));
                }
                if (major.getMinorList() != null && major.getMinorList().size() > 0) {
                    for (SingleUrlResult minor : major.getMinorList()) {
                        if (minor.getDetailTargetStatus() != 200 && minor.getDetailTargetUrl() != null) {
                            if (minor.getDetailTargetStatus() != BLACK_LIST_CODE) {
                                //只统计不是黑名单域名的死链数
                                this.collectCategoryDeadNumber(minor);
                            }
                            jdbc.saveDeadLinkRecord(this.buildDeadLinkRecords(minor, "minor"));
                        }
                    }
                }
            }
        }
    }

    /**
     * 统计各类型死链数
     *
     * @param result
     */
    private void collectCategoryDeadNumber(SingleUrlResult result) {
        if (result.getCategory().equals(CategoryEnum.EVENTS.getName())) {
            totalEventsDead += 1;
        } else if (result.getCategory().equals(CategoryEnum.MATERIALS.getName())) {
            totalMaterialsDead += 1;
        } else if (result.getCategory().equals(CategoryEnum.E_LEARNING.getName())) {
            totalElearningDead += 1;
        } else if (result.getCategory().equals(CategoryEnum.WORKFLOWS.getName())) {
            totalWorkflowsDead += 1;
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
        model.setPage(cache.getPage());
        model.setDetailLink(cache.getDetailLink());
        model.setDeadLinkTitle(cache.getDeadLinkTitle());
        model.setCreateTime(new Date());
        return model;
    }

    /**
     * 保存汇总数据
     *
     * @param jdbc
     */
    private void saveCollectInfo(MySqlJDBCImpl jdbc) {
        CollectInfo collectInfo = new CollectInfo();
        collectInfo.setEvents(totalEvents);
        collectInfo.setEventsDead(totalEventsDead);
        collectInfo.setMaterials(totalMaterials);
        collectInfo.setMaterialsDead(totalMaterialsDead);
        collectInfo.setElearning(totalElearning);
        collectInfo.setElearningDead(totalElearningDead);
        collectInfo.setWorkflows(totalWorkflows);
        collectInfo.setWorkflowsDead(totalWorkflowsDead);
        collectInfo.setCreateDate(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE));
        jdbc.saveCollectInfo(collectInfo);
    }

    /**
     * 爬取所有数据
     *
     * @return
     */
    private List<SingleUrlResult> crawlData() {
        List<SingleUrlResult> totalPageList = new ArrayList<>();
        boolean eventsContinue = true;
        boolean materialsContinue = true;
        boolean eLearningContinue = true;
        // 先爬完所有数据
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
                logger.info("***********爬到Events数据{}条, 当前第{}页***********", singlePageListEvents.size(), i);
            } else {
                eventsContinue = false;
            }
            //Materials
            if (singlePageListMaterials != null && singlePageListMaterials.size() > 0) {
                totalPageList.addAll(singlePageListMaterials);
                totalMaterials = totalMaterials + singlePageListMaterials.size();
                logger.info("***********爬到Materials数据{}条, 当前第{}页***********", singlePageListMaterials.size(), i);
            } else {
                materialsContinue = false;
            }
            //E-Learning
            if (singlePageListELearning != null && singlePageListELearning.size() > 0) {
                totalPageList.addAll(singlePageListELearning);
                totalElearning = totalElearning + singlePageListELearning.size();
                logger.info("***********爬到E-Learning数据{}条, 当前第{}页***********", singlePageListELearning.size(), i);
            } else {
                eLearningContinue = false;
            }
            //全都爬不到就跳出循环
            if (!eventsContinue && !materialsContinue && !eLearningContinue) {
                break;
            }
        }
        List<SingleUrlResult> singlePageListWorkflows = this.getWorkflowsList();
        //Workflows
        if (singlePageListWorkflows.size() > 0) {
            totalPageList.addAll(singlePageListWorkflows);
            totalWorkflows = singlePageListWorkflows.size();
            logger.info("***********爬到Workflows数据{}条***********", singlePageListWorkflows.size());
        }
        logger.info("***********共计爬取到数据{}条, Events{}条, Materials{}条, ELearning{}条, Wrokflows{}条***********", totalPageList.size(), totalEvents, totalMaterials, totalElearning, totalWorkflows);
        return totalPageList;
    }

    /**
     * 获取Events页面的数据
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
        //下面这行字符串是整个代码的最难点
        //这就是jQuery选择器的语法，并不是jsoup独创的，我猜python可以拿过去用
        //前面什么都没有，如div，ul，li就是标签名
        //前面是#，是标签的id
        //前面是.，是标签的class，注意：一个标签可以有多个class，我可以只引用一个
        //【>】代表标签向下，可以跨级
        //因此，下面这一行，可以分析为，<div id="home">下的<ul class="media-grid">下的<li class="media-item">
        //注意：这种代码非常脆弱，如果人家的H5代码一改，你马上就爬不到了，但是也没办法。
        //还有另一种是不爬H5，爬api；不过你这里不涉及，就别多说了
        Elements elements = doc.select("div#home > ul.media-grid > li.media-item");
        for (Element element : elements) {
//			System.out.println(element.select("#event-title").text());
//			System.out.println(element.select("a.media-view").attr("href"));
//			System.out.println("*******************");
            //对于同一个详情页，要获取它的title和url，不在同一个标签上，这两个标签的父又是<li>；因此，循环多个详情页，循环只能写到<li>，后面的就要在循环里处理了
            //这里的element就是<li>
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
        //访问detail页面
        String detailUrl = singleUrlResult.getDetailUrl();
        HttpResult detailHttpResult = HttpUtils.getSingleHttpWithRetry(detailUrl, RETRY_TIMES);
        singleUrlResult.setDetailHttpStatus(detailHttpResult.getHttpStatus());
        if ((!detailHttpResult.isSuccess()) || detailHttpResult.getHttpStatus() != 200) {
            logger.error("Detail页面都特么进不去, singleUrlResult={}", singleUrlResult);
//            singleUrlResult.setDetailTargetTitle();
            singleUrlResult.setDetailTargetUrl(detailUrl);
            singleUrlResult.setDetailTargetStatus(detailHttpResult.getHttpStatus());
            singleUrlResult.setDetailTargetReasonPhrase("internal error");
            return;
        }
        //分析detail页面，获得target页面
        String detailHttpContent = detailHttpResult.getHttpContent();
        Document doc = Jsoup.parse(detailHttpContent);
        Elements elements = doc.select(cssQuery);
        boolean major = true;
        List<SingleUrlResult> minorList = new ArrayList<>();
        for (Element a : elements) {
            if (a != null) {
                if (major) {
                    //只有第一个a标签是major
                    major = false;
                    this.refillMajor(singleUrlResult, a);
                } else {
                    //其余都是minor
                    this.refillMinor(singleUrlResult, minorList, a);
                }
            }
        }
        if (minorList.size() > 0) {
            singleUrlResult.setMinorList(minorList);
        }

        //处理Events的时间周期
        if (singleUrlResult.getCategory().equals(CategoryEnum.EVENTS.getName())) {
            this.refillStartAndEnd(singleUrlResult, doc);
        }
    }

    /**
     * 填充Major数据
     *
     * @param singleUrlResult
     * @param a
     */
    private void refillMajor(SingleUrlResult singleUrlResult, Element a) {
        singleUrlResult.setDetailTargetTitle(a.text());
        singleUrlResult.setDetailTargetUrl(a.attr("href"));
        String majorUrl = singleUrlResult.getDetailTargetUrl();
        //获取目标url的主域名
        String mainUrl = this.getMainUrl(majorUrl);
//        logger.info("refillMajor, majorUrl====={}, mainUrl===={}", majorUrl, mainUrl);
        //如果在黑名单就不请求，直接设置个固定code和message
        if (this.isOnTheBlacklist(mainUrl)) {
            singleUrlResult.setDetailTargetStatus(BLACK_LIST_CODE);
            singleUrlResult.setDetailTargetReasonPhrase(BLACK_LIST_MSG);
        }
//        else if (mainUrl != null && !this.isLegalSuffix(mainUrl)) {
//            //如果主域名不合法，直接设置个固定code和message
//            singleUrlResult.setDetailTargetStatus(ILLEGAL_URL_CODE);
//            singleUrlResult.setDetailTargetReasonPhrase(ILLEGAL_URL_MSG);
//            MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, ILLEGAL_URL_CODE, ILLEGAL_URL_MSG,
//                    singleUrlResult.getListIndex(), singleUrlResult.getDetailUrl(), singleUrlResult.getDetailTargetTitle()));
//            logger.info("major非法域名, 缓存MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
//        }
        else {
            CacheInfo mainUrlCache = MAIN_URL_DEAD_MAP.get(mainUrl);
            if (mainUrlCache == null) {
                //主域名没在缓存map中就正常请求获取结果
                HttpResult majorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(majorUrl, RETRY_TIMES);
                singleUrlResult.setDetailTargetStatus(majorUrlHttpResult.getHttpStatus());
                singleUrlResult.setDetailTargetReasonPhrase(majorUrlHttpResult.getReasonPhrase());
                //如果请求不成功，则单独请求主域名
                if (mainUrl != null && majorUrlHttpResult.getHttpStatus() != 200) {
                    HttpResult mainUrlResult = HttpUtils.getSingleHttpWithRetry(mainUrl, RETRY_TIMES);
                    if (mainUrlResult.getHttpStatus() != 200) {
                        //如果主域名都访问不成功，而且主域名在MAIN_URL_DEAD_MAP中不存在就放进去
                        MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, mainUrlResult.getHttpStatus(), mainUrlResult.getReasonPhrase(),
                                singleUrlResult.getListIndex(), singleUrlResult.getDetailUrl(), singleUrlResult.getDetailTargetTitle()));
                        logger.info("major缓存MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
                    }
                }
            } else {
                //主域名在缓存map中就把主域名的statusCode放进去
                singleUrlResult.setDetailTargetStatus(mainUrlCache.getHttpStatus());
                singleUrlResult.setDetailTargetReasonPhrase(mainUrlCache.getReasonPhrase());
            }
        }
    }

    /**
     * 填充Minor数据
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
        //获取目标url的主域名
        String mainUrl = this.getMainUrl(minorUrl);
//        logger.info("refillMinor, majorUrl====={}, mainUrl===={}", minorUrl, mainUrl);
        //如果在黑名单就不请求，直接设置个固定code和message
        if (this.isOnTheBlacklist(mainUrl)) {
            minor.setDetailTargetStatus(BLACK_LIST_CODE);
            minor.setDetailTargetReasonPhrase(BLACK_LIST_MSG);
        }
//        else if (mainUrl != null && !this.isLegalSuffix(mainUrl)) {
//            //如果主域名不合法，直接设置个固定code和message
//            singleUrlResult.setDetailTargetStatus(ILLEGAL_URL_CODE);
//            singleUrlResult.setDetailTargetReasonPhrase(ILLEGAL_URL_MSG);
//            MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, ILLEGAL_URL_CODE, ILLEGAL_URL_MSG,
//                    singleUrlResult.getListIndex(), singleUrlResult.getDetailUrl(), singleUrlResult.getDetailTargetTitle()));
//            logger.info("minor非法域名, 缓存MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
//        }
        else {
            CacheInfo mainUrlCache = MAIN_URL_DEAD_MAP.get(mainUrl);
            if (mainUrlCache == null) {
                //主域名没在缓存map中就正常请求获取结果
                HttpResult minorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(minorUrl, RETRY_TIMES);
                minor.setDetailTargetStatus(minorUrlHttpResult.getHttpStatus());
                minor.setDetailTargetReasonPhrase(minorUrlHttpResult.getReasonPhrase());
                //如果请求不成功，则单独请求主域名
                if (mainUrl != null && minorUrlHttpResult.getHttpStatus() != 200) {
                    HttpResult mainUrlResult = HttpUtils.getSingleHttpWithRetry(mainUrl, RETRY_TIMES);
                    if (mainUrlResult.getHttpStatus() != 200) {
                        //如果主域名都访问不成功，而且主域名在MAIN_URL_DEAD_MAP中不存在就放进去
                        MAIN_URL_DEAD_MAP.computeIfAbsent(mainUrl, k -> new CacheInfo(mainUrl, mainUrlResult.getHttpStatus(), mainUrlResult.getReasonPhrase(),
                                singleUrlResult.getListIndex(), singleUrlResult.getDetailUrl(), minor.getDetailTargetTitle()));
                        logger.info("minor缓存MAIN_URL_DEAD_MAP={}", MAIN_URL_DEAD_MAP);
                    }
                }
            } else {
                //主域名在缓存map中就把主域名的statusCode放进去
                minor.setDetailTargetStatus(mainUrlCache.getHttpStatus());
                minor.setDetailTargetReasonPhrase(mainUrlCache.getReasonPhrase());
            }
        }
        minorList.add(minor);
    }

    /**
     * 填充起始和结束时间数据
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
     * 是否在黑名单
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

//    /**
//     * url是否为合法后缀
//     *
//     * @param domainUrl
//     * @return
//     */
//    private boolean isLegalSuffix(String domainUrl) {
//        if (legalSuffixList == null) {
//            String str = "ac,uk,ad,asia,arts,ba,ca,cm,cn,com,cs,edu,eu,firm,gov,gp,hk,info,int,io,jobs,jp,top,museum,name,nato,net,ng,mil,om,org,post,pro,ps,cc,vip,club,win,pub,site,xyz,store,td,tel,tl,tn,travel,ug,us,web";
//            legalSuffixList = Arrays.asList(str.split(","));
//        }
//        String[] split = domainUrl.split("\\.");
//        String suffix = split[split.length - 1];
//        return legalSuffixList.contains(suffix);
//    }

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
     * 获取Materials或E-Learning页面的数据
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
     * 获取Workflows或Providers页面的数据
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
