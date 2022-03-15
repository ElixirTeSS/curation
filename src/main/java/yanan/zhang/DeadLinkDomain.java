package yanan.zhang;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class DeadLinkDomain {

    /**
     * Key
     */
    private Long id;

    /**
     * http status
     */
    private Integer statusCode;

    /**
     * http status phrase
     */
    private String reasonPhrase;

    /**
     * domain
     */
    private String domainUrl;

    /**
     * related link number
     */
    private Integer linkNumber;

    /**
     * creat time
     */
    private Date createTime;

    /**
     * page number
     */
    private Integer page;

    /**
     * detail url
     */
    private String detailLink;

    /**
     * dead link title
     */
    private String deadLinkTitle;

}
