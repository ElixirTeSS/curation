import org.junit.Test;
import yanan.zhang.HttpResult;
import yanan.zhang.HttpUtils;

/**
 * @author Yanan Zhang
 **/
public class HttpUtilsTest {

    @Test
    public void testGetSingleHttp() {

        String url = "https://tess.elixir-europe.org/events?page=1";

        HttpResult result = HttpUtils.getSingleHttp(url);

        System.out.println(result.isSuccess());
        System.out.println(result.getHttpStatus());
        System.out.println(result.getHttpContent());

    }
}
