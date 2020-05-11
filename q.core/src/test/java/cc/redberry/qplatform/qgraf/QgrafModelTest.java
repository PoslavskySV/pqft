package cc.redberry.qplatform.qgraf;

import cc.redberry.core.tensor.Tensors;
import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.MatrixElementDescription.ExternalPoint;
import cc.redberry.qplatform.model.Theories;
import cc.redberry.qplatform.model.Theories.QED;
import cc.redberry.qplatform.model.diagram.Diagram;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

/**
 *
 */
public class QgrafModelTest {

    @Test
    public void test1() throws Exception {
        QgrafModel model = new QgrafModel(Theories.qed());

        MatrixElementDescription me = new MatrixElementDescription(2,
                new ExternalPoint(QED.electron.fieldType, false, false, true, null, Tensors.parse("k1_i")),
                new ExternalPoint(QED.electron.fieldType, false, false, true, null, Tensors.parse("k2_i")),
                new ExternalPoint(QED.electron.fieldType, false, false, false, null, Tensors.parse("k3_i")),
                new ExternalPoint(QED.electron.fieldType, false, false, false, null, Tensors.parse("k4_i"))
        );

        List<Diagram> ll = model.generateDiagrams(Path.of("/Users/poslavskysv/Downloads/trash"), me).readAll();
        System.out.println(ll.size());
        ll.forEach(d -> System.out.println(d.applyRules(Theories.qed().feynmanRules)));
    }
}
