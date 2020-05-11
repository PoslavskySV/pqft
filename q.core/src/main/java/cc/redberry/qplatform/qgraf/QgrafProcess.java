package cc.redberry.qplatform.qgraf;

import cc.redberry.core.tensor.SimpleTensor;
import cc.redberry.qplatform.model.MatrixElementDescription;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QgrafProcess {
    public QgrafModelDescription model;
    public List<QgrafParticle> in, out;
    public List<String> inMomentums, outMomentums;
    public int nLoops;
    public String loopMomentumPrefix;
    public List<String> options;

    public QgrafProcess(QgrafModel model, MatrixElementDescription me) {
        this.model = model.qgrafModel;
        this.in = new ArrayList<>();
        this.out = new ArrayList<>();
        this.nLoops = me.nLoops;
        this.inMomentums = new ArrayList<>();
        this.outMomentums = new ArrayList<>();
        this.loopMomentumPrefix = "l";
        this.options = new ArrayList<>();

        for (MatrixElementDescription.ExternalPoint p : me.points) {
            List<QgrafParticle> particles;
            List<String> momentums;
            if (p.isIncoming) {
                particles = in;
                momentums = inMomentums;
            } else {
                particles = out;
                momentums = outMomentums;
            }

            particles.add(p.qgrafParticle());
            momentums.add(((SimpleTensor) p.momentum).getStringName());
        }
    }

    @Override
    public String toString() {
        return ("% config= nolist, lf, verbose ;\n\n" +

                "output= 'diags.list' ;\n\n" +

                "style= 'qgrafSty.sty' ;\n\n" +

                "model= ' qgrafModel';\n\n" +

                "in= inParticles ;\n\n" +

                "out= outParticles ;\n\n" +

                "loops= nLoops;\n\n" +

                "loop_momentum=loopMomentumPrefix;\n\n" +

                "options= optionsList ;\n" )
                .replace("qgrafModel", model.name)
                .replace("inParticles", IntStream.range(0, in.size()).mapToObj(i -> in.get(i).name + "[" + inMomentums.get(i) + "]").collect(Collectors.joining(",")))
                .replace("nLoops", Integer.toString(nLoops))
                .replace("loopMomentumPrefix", loopMomentumPrefix)
                .replace("outParticles", IntStream.range(0, out.size()).mapToObj(i -> out.get(i).name + "[" + outMomentums.get(i) + "]").collect(Collectors.joining(",")))
                .replace("optionsList", String.join(",", options));
    }
}
