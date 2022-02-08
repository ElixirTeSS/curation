package yanan.zhang;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Yanan Zhang
 **/
public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private static final RequestConfig CONFIG = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000).build();

    private HttpUtils() {

    }

    /**
     * if failed, then retry n times
     * if failed n times, then return the latest result
     *
     * @param url
     * @param retry
     * @return
     */
    public static HttpResult getSingleHttpWithRetry(String url, int retry) {

        HttpResult result = null;

        for (int i = 0; i < retry; i++) {
            result = getSingleHttp(url);
            if (result.isSuccess() && result.getHttpStatus() == 200) {
                return result;
            }
        }

        return result;
    }

    public static HttpResult getSingleHttp(String url) {

        HttpResult result = new HttpResult();

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(CONFIG);

        CloseableHttpResponse httpResponse = null;

        try {
            //use httpGet and return a response
            httpResponse = httpClient.execute(httpGet);

            logger.info("httpStatus={},url={}", httpResponse.getStatusLine(), url);

            result.setHttpStatus(httpResponse.getStatusLine().getStatusCode());
            //get the response status
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                result.setHttpContent(EntityUtils.toString(httpResponse.getEntity(), "gbk"));
                result.setSuccess(true);
            } else {
                result.setSuccess(false);
            }

        } catch (Exception e) {
            result.setSuccess(false);
//            logger.error("httpInvoke!url={}", url, e);
            logger.error("httpInvoke!url={}", url);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    logger.error("httpResponseCloseError!url={}", url, e);
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("httpClientCloseError!url={}", url, e);
                }
            }
        }

        return result;
    }

}
