package cc.redberry.qplatform.qgraf;

import cc.redberry.qplatform.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

/**
 *
 */
public class QgrafModelDescriptionTest {

    @Test
    public void test1() throws JsonProcessingException {
        QgrafModelDescription model = QgrafModels.gravity();

        System.out.println(JsonUtil.JsonMapperPretty.writeValueAsString(model));

        System.out.println(model.toString());
    }
}
