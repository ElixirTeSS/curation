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
     * 这条记录所在的list页面的索引
     */
    private int listIndex;
    /**
     * 这条记录在list页面上的title
     */
    private String listTitle;
    /**
     * detail页面的url
     */
    private String detailUrl;
    /**
     * 访问detail页面的时候的httpStatus
     */
    private int detailHttpStatus;
    /**
     * detail页面上的目标连接title
     */
    private String detailTargetTitle;
    /**
     * detail页面上的目标连接url
     */
    private String detailTargetUrl;
    /**
     * detail页面上的目标连接url的httpStatus
     */
    private int detailTargetStatus;

    /**
     * detail页面上的目标连接url的httpReasonPhrase
     */
    private String detailTargetReasonPhrase;

    /**
     * 种类: events, materials, elearning
     */
    private String category;

    /**
     * minor
     */
    private List<SingleUrlResult> minorList;

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
}