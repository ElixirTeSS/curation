import org.junit.Test;
import yanan.zhang.web.WebDataImpl;
import yanan.zhang.web.model.PieChartInfo;
import yanan.zhang.web.model.StackedChartInfo;

/**
 * @author Yanan Zhang
 **/
public class WebDataTest {

    private WebDataImpl web = new WebDataImpl();

    @Test
    public void getPieChartTest() {
        PieChartInfo pieChart = web.getPieChart();
        System.out.println(pieChart);
        System.out.println(pieChart.getData().size());
    }

    @Test
    public void getStackedChartTest() {
        StackedChartInfo stackedChart = web.getStackedChart();
        System.out.println(stackedChart);
    }

    @Test
    public void getLineChartTest() {
        StackedChartInfo lineChart = web.getLineChart();
        System.out.println(lineChart);
    }

}
