package yanan.zhang;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Real service can be divided into two steps:
 * 1. Get all the details on a list page
 * 2. Visit a detail page to get the URL and whether the URL is valid
 * @author Yanan Zhang
 **/
public class MainService {

    private static final String WEB_SITE = "https://tess.elixir-europe.org";
    //	private static final String WEB_SITE = "http://test-tess.its.manchester.ac.uk";
    private static final int TOTAL_PAGE = 415;
    private static final int PROCESSED_PER_THREAD = 40;
    public static final String CSS_QUERY_EVENTS = "div.page-header > p > a";
    public static final String CSS_QUERY_MATERIALS_AND_E_LEARNING = "div.text-justify a";
    public static final String CSS_QUERY_WORKFLOWS = "div#workflow-diagram-sidebar a";

    public List<SingleUrlResult> execute() {

        List<SingleUrlResult> retList = new ArrayList<>();

        for (int i = 1; i <= TOTAL_PAGE; i++) {
            List<SingleUrlResult> singlePageList = getEventsList(i);
            if (singlePageList == null || singlePageList.isEmpty()) {
                break;
            }
            for (SingleUrlResult result : singlePageList) {
                this.refillDetail(result, CSS_QUERY_EVENTS);
                retList.add(result);
            }
        }

        return retList;
    }

    public List<SingleUrlResult> executeMultithreading() {

        List<SingleUrlResult> resultList = new ArrayList<>();
        List<SingleUrlResult> totalPageList = new ArrayList<>();
        // craw all data
        for (int i = 1; i <= TOTAL_PAGE; i++) {
            List<SingleUrlResult> singlePageListEvents = this.getEventsList(i);
            List<SingleUrlResult> singlePageListMaterials = this.getMaterialsOrELearningList(i, CategoryEnum.MATERIALS);
            List<SingleUrlResult> singlePageListELearning = this.getMaterialsOrELearningList(i, CategoryEnum.E_LEARNING);

            //Events
            if (singlePageListEvents != null && singlePageListEvents.size() > 0) {
                totalPageList.addAll(singlePageListEvents);
                System.out.println("***********爬到Events数据" + singlePageListEvents.size() + "条***********");
            }
            //Materials
            if (singlePageListMaterials != null && singlePageListMaterials.size() > 0) {
                totalPageList.addAll(singlePageListMaterials);
                System.out.println("***********爬到Materials数据" + singlePageListMaterials.size() + "条***********");
            }
            //E-Learning
            if (singlePageListELearning != null && singlePageListELearning.size() > 0) {
                totalPageList.addAll(singlePageListELearning);
                System.out.println("***********爬到E-Learning数据" + singlePageListELearning.size() + "条***********");
            }
        }
        List<SingleUrlResult> singlePageListWorkflows = this.getWorkflowsList();
        //Workflows
        if (singlePageListWorkflows != null && singlePageListWorkflows.size() > 0) {
            totalPageList.addAll(singlePageListWorkflows);
            System.out.println("***********爬到Workflows数据" + singlePageListWorkflows.size() + "条***********");
        }
        System.out.println("***********共计爬取到数据" + totalPageList.size() + "条***********");

        // multithreading
        // the start time
        long start = System.currentTimeMillis();
        // a new thread for every 50 data
        int threadSize = 2;
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
                //store the results
                if (singleUrlResults != null && singleUrlResults.size() > 0) {
                    for (SingleUrlResult result : singleUrlResults) {
                        if (result.getDetailTargetStatus() != 200 && result.getDetailTargetUrl() != null) {
                            DeadLinkRecords model = new DeadLinkRecords();
                            model.setDeadLink(result.getDetailTargetUrl());
                            model.setStatusCode(result.getDetailTargetStatus());
                            model.setPage(result.getListIndex());
                            model.setParentUrl(result.getDetailUrl());
                            model.setCategory(result.getCategory());
                            model.setCreateTime(new Date());
                            jdbc.saveDeadLinkRecord(model);
                        }
                    }
                }
                System.out.println(future.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // close the thread pool
            exec.shutdown();
            System.out.println("End the multithreading execution");
            System.err.println("Time consuming: " + (System.currentTimeMillis() - start) / 1000 + "second");
        }

        return resultList;
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
                    //major
                    major = false;
                    singleUrlResult.setDetailTargetTitle(a.text());
                    singleUrlResult.setDetailTargetUrl(a.attr("href"));
                    String detailTargetUrl = singleUrlResult.getDetailTargetUrl();
                    HttpResult detailTargetHttpResult = HttpUtils.getSingleHttpWithRetry(detailTargetUrl, 3);
                    singleUrlResult.setDetailTargetStatus(detailTargetHttpResult.getHttpStatus());
                } else {
                    //minor
                    SingleUrlResult minor = new SingleUrlResult();
                    minor.setCategory(singleUrlResult.getCategory());
                    minor.setListIndex(singleUrlResult.getListIndex());
                    minor.setListTitle(singleUrlResult.getListTitle());
                    minor.setDetailUrl(singleUrlResult.getDetailUrl());
                    minor.setDetailHttpStatus(singleUrlResult.getDetailHttpStatus());
                    minor.setDetailTargetTitle(a.text());
                    minor.setDetailTargetUrl(a.attr("href"));
                    String minorUrl = minor.getDetailTargetUrl();
                    HttpResult minorUrlHttpResult = HttpUtils.getSingleHttpWithRetry(minorUrl, 3);
                    minor.setDetailTargetStatus(minorUrlHttpResult.getHttpStatus());
                    minorList.add(minor);
                }
            }
        }
        if (minorList.size() > 0) {
            singleUrlResult.setMinorList(minorList);
        }
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


