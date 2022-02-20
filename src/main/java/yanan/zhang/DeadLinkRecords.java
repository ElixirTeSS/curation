package yanan.zhang;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class DeadLinkRecords {

    /**
     * key
     */
    private Long id;

    /**
     * category: events, materials, elearning_materials
     */
    private String category;

    /**
     * page
     */
    private Integer page;

    /**
     * http status
     */
    private Integer statusCode;

    /**
     * http status phrase
     */
    private String reasonPhrase;

    /**
     * major, minor
     */
    private String type;

    /**
     * dead link url
     */
    private String deadLink;

    /**
     * parent url
     */
    private String parentUrl;

    /**
     * domain
     */
    private String domainUrl;


    /**
     * Time
     */
    private Date createTime;

}
