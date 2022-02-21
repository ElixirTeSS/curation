import org.junit.Test;
import yanan.zhang.PoiExportImpl;

import java.io.IOException;

/**
 * @author Yanan Zhang
 **/
public class PoiTest {

    private PoiExportImpl poi = new PoiExportImpl();

    @Test
    public void exportExcelTest() throws IOException {
        poi.exportExcel();
    }

}
