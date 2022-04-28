package yanan.zhang;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class BlackListDomain {

    /**
     * Key
     */
    private Long id;

    /**
     * domain URL
     */
    private String domainUrl;

    /**
     * create time
     */
    private Date createTime;
}
