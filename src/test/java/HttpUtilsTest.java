import org.junit.Test;
import yanan.zhang.HttpResult;
import yanan.zhang.HttpUtils;

/**
 * @author Yanan Zhang
 **/
public class HttpUtilsTest {

    @Test
    public void testGetSingleHttp() {

//		String url = "http://www.escience2011.org";
//		String url = "https://webprotege.stanford.edu/R9TrjymduaijOFxNNs4OmtVg";
        String url = "https://tess.elixir-europe.org/";

        HttpResult result = HttpUtils.getSingleHttp(url);

        System.out.println(result.isSuccess());
        System.out.println(result.getReasonPhrase());
        System.out.println(result.getHttpStatus());
        System.out.println(result.getHttpContent());

    }

}