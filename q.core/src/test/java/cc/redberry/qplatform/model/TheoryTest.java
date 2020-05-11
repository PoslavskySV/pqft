package cc.redberry.qplatform.model;

import cc.redberry.core.tensor.Tensors;
import cc.redberry.qplatform.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class TheoryTest {
    @Test
    void test1() throws JsonProcessingException {
        var th = Theories.qed();
        var me = new MatrixElementDescription(2,
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, true, null, Tensors.parse("k1_i")),
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, true, null, Tensors.parse("k2_i")),
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, false, null, Tensors.parse("k3_i")),
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, false, null, Tensors.parse("k4_i"))
        );

        var req = new RestApi.DiagramsCalculationRequest(th, me);

        System.out.println(JsonUtil.JsonMapper.writeValueAsString(req));
    }
}
