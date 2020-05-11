package cc.redberry.qplatform.endpoints.admin;

import cc.redberry.core.tensor.Tensors;
import cc.redberry.qplatform.endpoints.kafka.TheoriesStore;
import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.RestApi;
import cc.redberry.qplatform.model.Theories;
import cc.redberry.qplatform.model.Theory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 *
 */
class TheoriesStoreControllerTest {

    @Test
    void test1() {
        System.out.println(Arrays.toString(TheoriesController.listTheories().block()));
        Theory th = TheoriesStore.requestTheory("sdsds").block();
        System.out.println(th);
    }

    @Test
    void test2() {
        var th = Theories.qed();
        var me = new MatrixElementDescription(2,
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, true, null, Tensors.parse("k1_i")),
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, true, null, Tensors.parse("k2_i")),
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, false, null, Tensors.parse("k3_i")),
                new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, false, null, Tensors.parse("k4_i"))
        );

        var req = new RestApi.DiagramsCalculationRequest(th, me);

        TheoriesController.run(req).block();
    }

    @Test
    void test3() {

    }
}
