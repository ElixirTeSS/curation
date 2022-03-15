package yanan.zhang;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class BlackListDomain {

    /**
     * 主键
     */
    private Long id;

    /**
     * 主域名
     */
    private String domainUrl;

    /**
     * 添加时间
     */
    private Date createTime;
}
