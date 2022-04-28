package yanan.zhang;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class DeadLinkRecords {

    /**
     * ID
     */
    private Long id;

    /**
     * categories: events, materials, elearning
     */
    private String category;

    /**
     * page number
     */
    private Integer page;

    /**
     * http status code
     */
    private Integer statusCode;

    /**
     * http reason phrase
     */
    private String reasonPhrase;

    /**
     * type: major, minor
     */
    private String type;

    /**
     * url of the broken link
     */
    private String deadLink;

    /**
     * title of the broken link
     */
    private String deadLinkTitle;

    /**
     * parent url
     */
    private String parentUrl;

    /**
     * broken domain URL
     */
    private String domainUrl;

    /**
     * starting time
     */
    private String start;

    /**
     * ending time
     */
    private String end;

    /**
     * period (day)
     */
    private String duration;

    /**
     * create time
     */
    private Date createTime;

}