import org.junit.Test;
import yanan.zhang.HttpResult;
import yanan.zhang.HttpUtils;

/**
 * @author Yanan Zhang
 **/
public class HttpUtilsTest {

    @Test
    public void testGetSingleHttp() {

        String url = "http://www.genomicsnetwork.ac.uk/forum/events/conferences/title\\,26253\\,en.html";

        HttpResult result = HttpUtils.getSingleHttp(url);

        System.out.println(result.isSuccess());
        System.out.println(result.getHttpStatus());
        System.out.println(result.getHttpContent());

    }
}
