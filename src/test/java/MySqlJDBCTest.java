import yanan.zhang.*;

import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yanan Zhang
 **/
public class MySqlJDBCTest {

    private final MySqlJDBCImpl jdbc = new MySqlJDBCImpl();

    @Test
    public void countDeadLinkRecordsByDomainTest() {
        jdbc.countDeadLinkRecordsByDomain("");
    }

    @Test
    public void saveDeadLinkRecordTest() {
        jdbc.dropTable();
        jdbc.createTable();
        DeadLinkRecords model = new DeadLinkRecords();
        model.setCategory("test cat");
        model.setPage(1);
        model.setType("test type");
        model.setDeadLink("abc");
        model.setStatusCode(200);
        model.setReasonPhrase("OK");
        model.setParentUrl("test OK");
        model.setDomainUrl("test domain");
        jdbc.saveDeadLinkRecord(model);
    }

    @Test
    public void selectDeadLinkDomainTest() {
        Set<String> suffixSet = new HashSet<>();
        List<DeadLinkDomain> deadLinkDomains = jdbc.selectDeadLinkDomain(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE_WITHOUT_SYMBOL));
        deadLinkDomains.forEach(x -> suffixSet.add(x.getDomainUrl().substring(x.getDomainUrl().lastIndexOf(".") + 1)));
        System.out.println(suffixSet);
        System.out.println("size====" + suffixSet.size());
    }

    @Test
    public void saveCollectInfoTest() {
        CollectInfo collectInfo = new CollectInfo();
        collectInfo.setEvents(10);
        collectInfo.setEventsDead(2);
        collectInfo.setMaterials(10);
        collectInfo.setMaterialsDead(2);
        collectInfo.setElearning(10);
        collectInfo.setElearningDead(2);
        collectInfo.setWorkflows(10);
        collectInfo.setWorkflowsDead(2);
        collectInfo.setDomainDead(10);
        collectInfo.setCreateDate("2022-03-13");
        jdbc.saveCollectInfo(collectInfo);
    }

    @Test
    public void deleteCollectInfoTest() {
        jdbc.deleteCollectInfo(DateUtils.format(new Date(), DateUtils.FORMATTER_DATE));
    }

}