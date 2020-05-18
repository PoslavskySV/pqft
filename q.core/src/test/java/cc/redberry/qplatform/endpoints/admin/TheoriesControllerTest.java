package cc.redberry.qplatform.endpoints.admin;

import cc.redberry.core.tensor.Tensors;
import cc.redberry.qplatform.endpoints.kafka.TheoriesStore;
import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.RestApi;
import cc.redberry.qplatform.model.Theories;
import cc.redberry.qplatform.model.Theory;
import org.junit.jupiter.api.Test;

import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;

/**
 *
 */
class TheoriesControllerTest {

    @Test
    void test1() {
        System.out.println(Arrays.toString(TheoriesController.listTheories().block()));
        Theory th = TheoriesStore.requestTheory("sdsds").block();
        System.out.println(th);
    }

    @Test
    void test2() {
// theory
var th = Theories.qed();

// description of matrix element to process
var me = new MatrixElementDescription(2,
        new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, true, null, Tensors.parse("k1_i")),
        new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, true, null, Tensors.parse("k2_i")),
        new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, false, null, Tensors.parse("k3_i")),
        new MatrixElementDescription.ExternalPoint(Theories.QED.electron.fieldType, false, false, false, null, Tensors.parse("k4_i"))
);

// process request
var req = new RestApi.DiagramsCalculationRequest(th, me);

// submit to cluster and wait 200
TheoriesController.run(req).block();
    }

    @Test
    void test3() {
        System.out.println(PosixFilePermissions.fromString("rwxrwxrwx"));
    }
}
