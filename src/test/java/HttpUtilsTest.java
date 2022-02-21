import org.junit.Test;
import yanan.zhang.HttpResult;
import yanan.zhang.HttpUtils;

/**
 * @author Yanan Zhang
 **/
public class HttpUtilsTest {

    @Test
    public void testGetSingleHttp() {

//		String url = "https://tess.elixir-europe.org/events?page=1";
//		String url = "http://www.biotech-calendar.com/showinfo/northeast2012.html#midNE#__1";
//		String url = "http://www.genomicsnetwork.ac.uk/forum/events/conferences/title\\,26253\\,en.html";
//		String url = "https://www.scilifelab.se/events/rnaseqvt17/";
//		String url = "http://bio.informatics.iupui.edu";
        String url = "http://biocomp.vbcf.ac.at";

//		HttpResult result = HttpUtils.getSingleHttp(url.replaceAll("#", "%23").replaceAll("\\\\", ""));
        HttpResult result = HttpUtils.getSingleHttp(url);

        System.out.println(result.isSuccess());
        System.out.println(result.getReasonPhrase());
        System.out.println(result.getHttpStatus());
        System.out.println(result.getHttpContent());

    }

}