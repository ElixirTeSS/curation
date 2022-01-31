package yanan.zhang;

import lombok.Data;

import java.util.List;

/**
 * @author Yanan Zhang
 **/
@Data
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
     * categories: events, materials, elearning
     */
    private String category;
    /**
     * minor
     */
    private List<SingleUrlResult> minorList;
}
