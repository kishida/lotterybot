package kis.lotterybot;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author naoki
 */
public class MainTest {
    @Test
    public void createPairsTest() throws Exception {
        List<String> data = Arrays.asList("1", "2", "3", "---", "a", "b", "c");
        List<String> pairs = Main.createPairs(data);
        assertThat(pairs.size(), is(2));
    }
}
