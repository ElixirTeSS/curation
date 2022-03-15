package yanan.zhang;

import lombok.Data;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
@Data
public class DeadLinkRecords {

    /**
     * 主键
     */
    private Long id;

    /**
     * 种类: events, materials, elearning
     */
    private String category;

    /**
     * 页数
     */
    private Integer page;

    /**
     * http状态码
     */
    private Integer statusCode;

    /**
     * 状态码短语
     */
    private String reasonPhrase;

    /**
     * 类型: major, minor
     */
    private String type;

    /**
     * 死链url
     */
    private String deadLink;

    /**
     * 死链标题
     */
    private String deadLinkTitle;

    /**
     * 父级url
     */
    private String parentUrl;

    /**
     * 死链主域名
     */
    private String domainUrl;

    /**
     * 起始时间
     */
    private String start;

    /**
     * 结束时间
     */
    private String end;

    /**
     * 周期(天)
     */
    private String duration;

    /**
     * 添加时间
     */
    private Date createTime;

}