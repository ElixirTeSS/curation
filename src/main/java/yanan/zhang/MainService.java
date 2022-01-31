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
//    private static final String WEB_SITE = "http://test-tess.its.manchester.ac.uk";
    private static final int TOTAL_PAGE = 1;

    public List<SingleUrlResult> execute() {

        List<SingleUrlResult> retList = new ArrayList<>();

        for (int i = 1; i <= TOTAL_PAGE; i++) {
            List<SingleUrlResult> singlePageList = getFromList(i);
            if (singlePageList == null || singlePageList.isEmpty()) {
                break;
            }
            for (SingleUrlResult result : singlePageList) {
//                this.refillDetail(result);
                this.refillDetailMaterials(result);
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
            List<SingleUrlResult> singlePageList = this.getFromList(i);
            if (singlePageList == null || singlePageList.isEmpty()) {
                break;
            }
            totalPageList.addAll(singlePageList);
        }
        System.out.println("*********** Total data: " + totalPageList.size() + "***********");
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
                        refillDetail(result);
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
                //把结果存库
                if (singleUrlResults != null && singleUrlResults.size() > 0) {
                    for (SingleUrlResult result : singleUrlResults) {
                        if (result.getDetailTargetStatus() != 200) {
                            DeadLinkRecords model = new DeadLinkRecords();
                            model.setDeadLink(result.getDetailTargetUrl());
                            model.setStatusCode(result.getDetailHttpStatus());
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
     * @param index
     * @return
     */
    public List<SingleUrlResult> getFromList(int index) {

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

    public void refillDetail(SingleUrlResult singleUrlResult) {

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
        Element element = doc.select("div.page-header > p > a").first();
        Elements elements = doc.select("div.page-header > p > a");
        boolean major = true;
        List<SingleUrlResult> minorList = new ArrayList<>();
        for (Element a : elements) {
            if (a != null) {
                if (major) {
                    // first a: major
                    major = false;
                    singleUrlResult.setDetailTargetTitle(a.text());
                    singleUrlResult.setDetailTargetUrl(a.attr("href"));
                    String detailTargetUrl = singleUrlResult.getDetailTargetUrl();
                    HttpResult detailTargetHttpResult = HttpUtils.getSingleHttpWithRetry(detailTargetUrl, 3);
                    singleUrlResult.setDetailTargetStatus(detailTargetHttpResult.getHttpStatus());
                } else {
                    // other a: minor
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
     * * Visit a separate page (material）
     * @param index
     * @return
     */
    public List<SingleUrlResult> getMaterialsList(int index) {
        String url = WEB_SITE + "/materials?page=" + index;

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
            singleUrlResult.setListIndex(index);
            singleUrlResult.setListTitle(element.select("a.list-card-heading").text());
            singleUrlResult.setDetailUrl(WEB_SITE + element.select("a.list-card-heading").attr("href"));
            retList.add(singleUrlResult);
        }
        return retList;
    }

    public void refillDetailMaterials(SingleUrlResult singleUrlResult) {

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
        Elements elements = doc.select("div.text-justify a");
        boolean major = true;
        List<SingleUrlResult> minorList = new ArrayList<>();
        for (Element a : elements) {
            if (a != null) {
                if (major) {
                    // first a: major
                    major = false;
                    singleUrlResult.setDetailTargetTitle(a.text());
                    singleUrlResult.setDetailTargetUrl(a.attr("href"));
                    String detailTargetUrl = singleUrlResult.getDetailTargetUrl();
                    HttpResult detailTargetHttpResult = HttpUtils.getSingleHttpWithRetry(detailTargetUrl, 3);
                    singleUrlResult.setDetailTargetStatus(detailTargetHttpResult.getHttpStatus());
                } else {
                    // other a: minor
                    SingleUrlResult minor = new SingleUrlResult();
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

}
