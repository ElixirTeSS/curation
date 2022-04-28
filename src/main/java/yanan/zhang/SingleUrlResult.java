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
     * page number
     */
    private int listIndex;
    /**
     * title of the detail page
     */
    private String listTitle;
    /**
     * detail page url
     */
    private String detailUrl;
    /**
     * httpStatus of the detail page
     */
    private int detailHttpStatus;
    /**
     * target title
     */
    private String detailTargetTitle;
    /**
     * target URL
     */
    private String detailTargetUrl;
    /**
     * httpStatus of the target URL
     */
    private int detailTargetStatus;

    /**
     * httpReasonPhrase of the target URL
     */
    private String detailTargetReasonPhrase;

    /**
     * Categories: events, materials, elearning
     */
    private String category;

    /**
     * minor
     */
    private List<SingleUrlResult> minorList;

    /**
     * Starting time
     */
    private String start;

    /**
     * Ending time
     */
    private String end;

    /**
     * Period
     */
    private String duration;
}