package yanan.zhang;

import lombok.Data;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class WhiteListDomain {

    /**
     * ID
     */
    private Long id;

    /**
     * Domain URL
     */
    private String domainUrl;

    /**
     * Creat time
     */
    private Date createTime;
}
