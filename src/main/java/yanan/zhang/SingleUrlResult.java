package yanan.zhang;

import lombok.Data;
import lombok.ToString;

import java.util.List;


/**
 * save single result
 * @author Yanan Zhang
 **/
@Data
@ToString
public class SingleUrlResult {

    /**
     * the index of the list page
     */
    private int listIndex;
    /**
     * the title of the list page
     */
    private String listTitle;
    /**
     * url for the detail page
     */
    private String detailUrl;
    /**
     * the http status of the detail page
     */
    private int detailHttpStatus;
    /**
     * the title of the target link
     */
    private String detailTargetTitle;
    /**
     * the url of the target link
     */
    private String detailTargetUrl;
    /**
     * the http status of the target link
     */
    private int detailTargetStatus;
    /**
     * httpReasonPhrase of the target link
     */
    private String detailTargetReasonPhrase;
    /**
     * categories: events, materials, elearning
     */
    private String category;
    /**
     * minor
     */
    private List<SingleUrlResult> minorList;
}
