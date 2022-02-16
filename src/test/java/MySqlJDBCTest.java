import yanan.zhang.DeadLinkRecords;
import yanan.zhang.MySqlJDBCImpl;

import org.junit.Test;

import java.util.Date;

/**
 * @author Yanan Zhang
 **/
public class MySqlJDBCTest {

    private MySqlJDBCImpl jdbc = new MySqlJDBCImpl();

    @Test
    public void countDeadLinkRecordsByDomainTest() {
        jdbc.countDeadLinkRecordsByDomain("");
    }

    @Test
    public void saveDeadLinkRecordTest() {
        jdbc.dropTable();
        jdbc.createTable();
        DeadLinkRecords model = new DeadLinkRecords();
        model.setCategory("1");
        model.setPage(1);
        model.setDeadLink("abc");
        model.setStatusCode(200);
        model.setReasonPhrase("OK");
        model.setParentUrl("ddd");
        model.setDomainUrl("domain");
        jdbc.saveDeadLinkRecord(model);
    }

}
