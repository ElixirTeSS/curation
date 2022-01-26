import org.junit.Test;
import yanan.zhang.MainService;
import yanan.zhang.SingleUrlResult;

import java.util.List;

/**
 * @author Yanan Zhang
 **/
public class MainServiceTest {

    private MainService mainService = new MainService();

    @Test
    public void testGetFromList() {

        List<SingleUrlResult> list = mainService.getFromList(1);
        System.out.println(list==null);

        if (list!=null) {
            for (SingleUrlResult result:list) {
                System.out.println(result);
            }
        }

    }

    @Test
    public void testRefillDetail() {

        List<SingleUrlResult> list = mainService.getFromList(1);

        SingleUrlResult result = list.get(0);

        System.out.println(result);
        mainService.refillDetail(result);
        System.out.println(result);

    }

    @Test
    public void testExecute() {

        List<SingleUrlResult> list = mainService.execute();

        int index = 1;

        for (SingleUrlResult result : list) {
            System.out.println((index++) + "," + result);
        }

        System.out.println("======================");

        index = 1;

        for (SingleUrlResult result : list) {
            if (result.getDetailTargetStatus() != 200) {
                System.out.println((index++) + "," + result);
            }
        }
    }

    @Test
    public void testExecuteMultithreading() {

        List<SingleUrlResult> list = mainService.executeMultithreading();

        int index = 1;

        for (SingleUrlResult result : list) {
            System.out.println((index++) + "," + result);
        }

        System.out.println("======================");

        index = 1;

        for (SingleUrlResult result : list) {
            if (result.getDetailTargetStatus() != 200) {
                System.out.println((index++) + "," + result);
            }
        }
    }
}
