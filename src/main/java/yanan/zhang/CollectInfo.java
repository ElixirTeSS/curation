package yanan.zhang;

import lombok.Data;

/**
 * @author Yanan Zhang
 **/
@Data
public class CollectInfo {

    private Long id;
    private int events;
    private int eventsDead;
    private int materials;
    private int materialsDead;
    private int elearning;
    private int elearningDead;
    private int workflows;
    private int workflowsDead;
    private int domainDead;
    private String createDate;

}
