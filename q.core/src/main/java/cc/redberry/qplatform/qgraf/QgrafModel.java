package cc.redberry.qplatform.qgraf;

import cc.redberry.core.indexgenerator.IndexGenerator;
import cc.redberry.core.indexgenerator.IndexGeneratorImpl;
import cc.redberry.core.indexmapping.Mapping;
import cc.redberry.core.indices.IndexType;
import cc.redberry.core.indices.IndicesUtils;
import cc.redberry.core.tensor.ProductBuilder;
import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.TensorField;
import cc.redberry.core.tensor.Tensors;
import cc.redberry.core.utils.IntArrayList;
import cc.redberry.qplatform.model.*;
import cc.redberry.qplatform.model.diagram.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** QGRAF model that corresponds to the given Theory */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class QgrafModel {
    public static final Logger logger = LoggerFactory.getLogger(QgrafModel.class);
    @JsonProperty("theory")
    public final Theory theory;

    Map<String, FieldRules> particlesByName = new HashMap<>();
    Map<String, FieldRules> propagatorsByName = new HashMap<>();
    Map<String, VertexRule> vertexesByName = new HashMap<>();

    /** Model description */
    public final QgrafModelDescription qgrafModel;

    @JsonCreator
    public QgrafModel(@JsonProperty("theory") Theory theory) {
        this.theory = theory;
        this.qgrafModel = new QgrafModelDescription(theory.id, new ArrayList<>(), new ArrayList<>());
        var feynmanRules = theory.feynmanRules;
        var frMapping = feynmanRules.getParticleRulesMap();

        for (FieldRules particle : feynmanRules.particles) {
            var fieldType = particle.fieldType;
            var pName = fieldType.particleName();

            particlesByName.put(pName, particle);
            QgrafParticle qgrafParticle = fieldType.qgrafParticle();
            QgrafParticle qgrafAntiparticle = fieldType.qgrafAntiparticle();

            particlesByName.put(qgrafParticle.name, particle);
            particlesByName.put(qgrafAntiparticle.name, particle);

            var qgrafProp = new QgrafPropagator();
            qgrafProp.particle = qgrafParticle;
            qgrafProp.antiparticle = qgrafAntiparticle;
            var propagatorName = qgrafParticle.name + qgrafAntiparticle.name;
            qgrafProp.functionName = propagatorName;
            qgrafProp.mass = particle.mass;

            propagatorsByName.put(propagatorName, particle);
            qgrafModel.propagators.add(qgrafProp);
        }

        for (VertexRule vertex : feynmanRules.interactions) {
            var qgrafVertex = new QgrafVertex();
            qgrafVertex.particles = new ArrayList<>();
            for (int i = 0; i < vertex.particles.size(); i++) {
                var particle = frMapping.get(vertex.particles.get(i).fieldType);
                var fieldType = particle.fieldType;
                qgrafVertex.particles.add(vertex.conjugates[i] ? fieldType.qgrafAntiparticle() : fieldType.qgrafParticle());
            }
            qgrafVertex.name = qgrafVertex.particles.stream().map(s -> s.name).collect(Collectors.joining());
            qgrafModel.vertices.add(qgrafVertex);
            vertexesByName.put(qgrafVertex.name, vertex);
        }
    }

    //////

    public DiagramReader generateDiagrams(MatrixElementDescription matrixElementDescription)
            throws InterruptedException, IOException, URISyntaxException {
        Path tmpdir = Files.createTempDirectory("qgraf-" + matrixElementDescription.hashCode());
        return generateDiagrams(tmpdir, matrixElementDescription);
    }

    public DiagramReader generateDiagrams(Path dir, MatrixElementDescription matrixElementDescription)
            throws IOException, URISyntaxException, InterruptedException {
        if (matrixElementDescription.isTruncated())
            throw new UnsupportedOperationException("truncated ME not supported yet");

        QgrafProcess process = new QgrafProcess(this, matrixElementDescription);

        Path sty = dir.resolve("qgrafSty.sty");
        Path exe = dir.resolve("qgraf");

        Files.copy(Paths.get(getClass().getResource("qgrafSty.sty").toURI()), sty, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(getClass().getResource("qgraf").toURI()), exe, StandardCopyOption.REPLACE_EXISTING);

        Path model = dir.resolve(process.model.name);
        Files.writeString(model, process.model.toString());

        Path processFile = dir.resolve("qgraf.dat");
        Files.writeString(processFile, process.toString());

        Path output = dir.resolve("diags.list");
        Files.deleteIfExists(output);
        Process proc = new ProcessBuilder(exe.toAbsolutePath().toString()).directory(dir.toFile()).start();
        logger.info("succesfully started Qgraf process");
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null)
            logger.info(line);

        proc.waitFor();

        return mkReader(output);
    }


    /** Create parser from QGRAF output file */
    public DiagramReader mkReader(Path file) throws IOException {
        return new DiagramReader(file);
    }

    /** Diagrams parser */
    public final class DiagramReader implements Cloneable, AutoCloseable {
        final Path file;
        final BufferedReader reader;

        public DiagramReader(Path file) throws IOException {
            this.file = file;
            this.reader = Files.newBufferedReader(file);
        }

        private final AtomicInteger diagramIndex = new AtomicInteger(0);

        /** read all to list */
        public List<Diagram> readAll() throws Exception {
            logger.info("reading all diagrams");
            try {
                List<Diagram> all = new ArrayList<>();
                Diagram d;
                while ((d = next()) != null)
                    all.add(d);
                return all;
            } finally {
                close();
            }
        }

        public Diagram next() throws IOException {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.equals("ENDDIAGRAM"))
                    break;
                lines.add(line);
            }
            if (line == null)
                return null;
            return parseDiagram(lines, diagramIndex.getAndIncrement());
        }

        @Override
        public void close() throws Exception {
            reader.close();
        }
    }

    public Diagram parseDiagram(List<String> lines, int diagramIndex) {
        List<Leg> legs = new ArrayList<>();
        List<Propagator> propagators = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>();
        ProductBuilder pb = new ProductBuilder();
        Map<Integer, DiagramElement> parsedElements = new HashMap<>();

        IndexGenerator ig = new IndexGeneratorImpl();
        for (String line : lines) {
            line = line.strip();

            if (line.isEmpty())
                continue;

            if (line.startsWith("polin") || line.startsWith("polout")) {

                var pp = parseParticles(line, line.indexOf('(') + 1).get(0);
                var particleRules = particlesByName.get(pp.particle);
                var isAntiparticle = pp.particle.startsWith(FieldType.AntiparticlePrefix);

                var legType = isAntiparticle
                        ? (line.startsWith("polin") ? LegType.AntiparticleIn : LegType.AntiparticleOut)
                        : (line.startsWith("polin") ? LegType.ParticleIn : LegType.ParticleOut);

                var legRule = particleRules.legs.get(legType);
                var momenta = GroupType.momenta(pp.momenta);

                var legFunc = resetIndices(ig, legRule.getFunc(momenta));
                var leg = new Leg(legFunc, particleRules.fieldType, legType, pp.index, momenta);
                legs.add(leg);
                parsedElements.put(pp.index, leg);

                continue;
            }

            String func = line.substring(0, line.indexOf('('));

            FieldRules fieldRules = propagatorsByName.get(func);
            if (fieldRules != null) {
                // propagator

                var pps = parseParticles(line, func.length() + 1);
                var in = pps.get(0);
                var out = pps.get(1);

                assert parsedElements.get(in.index) == null;
                assert parsedElements.get(out.index) == null;

                PropagatorRule propagatorRule;
                if (in.particle.startsWith(FieldType.AntiparticlePrefix))
                    propagatorRule = fieldRules.antiPropagator;
                else
                    propagatorRule = fieldRules.propagator;


                var propagatorFunc = resetIndices(ig, propagatorRule.getFunc(GroupType.momenta(in.momenta)));

                var propagator = new Propagator(propagatorFunc, fieldRules.fieldType, in.index, out.index, propagatorFunc.get(0));
                parsedElements.put(in.index, propagator);
                parsedElements.put(out.index, propagator);
                propagators.add(propagator);

                continue;
            }

            VertexRule vertexRule = vertexesByName.get(func);
            if (vertexRule != null) {
                // vertex

                var pps = parseParticles(line, func.length() + 1);
                TensorField vertexFunc = vertexRule.getFunc(pps.stream().map(p -> GroupType.momenta(p.momenta)).toArray(Tensor[]::new));

                for (int i = 0; i < pps.size(); i++) {
                    ParsedParticle pp = pps.get(i);
                    int particleIndex = pp.index;
                    DiagramElement parsed = parsedElements.get(particleIndex);
                    if (parsed == null)
                        continue;

                    vertexFunc = setVertexIndices(vertexFunc, vertexRule, parsed, i, pp.index);
                }


                var vertex = new Vertex(vertexFunc, vertexRule.particles.stream().map(p -> p.fieldType).collect(Collectors.toList()), pps.stream().mapToInt(p -> p.index).toArray(), List.of(vertexFunc.toArray()));
                for (ParsedParticle p : pps)
                    parsedElements.put(p.index, vertex);

                continue;
            }

            // scalar
            pb.put(Tensors.parse(line));
        }

        if (parsedElements.isEmpty())
            return null;
        return new Diagram(pb.build(), legs, propagators, vertices, diagramIndex, 0);
    }

    private static <T extends Tensor> T resetIndices(IndexGenerator ig, T tensor) {
        int[] from = tensor.getIndices().toArray();
        int[] to = new int[from.length];
        for (int i = 0; i < from.length; ++i) {
            int index = ig.generate(IndicesUtils.getType(from[i]));
            index = IndicesUtils.setState(IndicesUtils.getState(from[i]), index);
            to[i] = index;
        }
        return (T) new Mapping(from, to).transform(tensor);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Tensor> T setVertexIndices(Tensor vertexFunc,
                                                         VertexRule vertexRule,
                                                         DiagramElement connection,
                                                         int iRay,
                                                         int particleIndex) {
        var sourceIndices = connection.expression.getIndices();
        var targetIndices = vertexFunc.getIndices();

        var from = new IntArrayList();
        var to = new IntArrayList();

        boolean toPropagator = false;
        int iSource;
        if (connection instanceof Leg) {
            iSource = 0;
        } else if (connection instanceof Propagator) {
            toPropagator = true;
            iSource = ((Propagator) connection).particleIndex1 == particleIndex ? 0 : 1;
        } else {
            throw new IllegalArgumentException();
        }

        Map<IndexType, Integer> offsets = new EnumMap<>(IndexType.class);
        for (int i = 0; i < iRay; ++i)
            for (Map.Entry<GroupType, Integer> e : vertexRule.particles.get(i).fieldType.representations.representations.entrySet()) {
                offsets.compute(e.getKey().indexType, (k, v) -> (v == null ? 0 : v) + e.getValue());
            }

        for (IndexType it : IndexType.values()) {
            var ofType = sourceIndices.getOfType(it);
            int size = ofType.size() / (toPropagator ? 2 : 1);
            for (int j = 0; j < size; ++j) {
                from.add(targetIndices.get(it, offsets.getOrDefault(it, 0) + j));
                to.add(IndicesUtils.inverseIndexState(ofType.get(iSource * size + j)));
            }
        }

        return (T) new Mapping(from.toArray(), to.toArray()).transform(vertexFunc);
    }

    private static List<ParsedParticle> parseParticles(String str, int from) {
        List<ParsedParticle> particles = new ArrayList<>();
        parseParticles(str, from, particles);
        return particles;
    }

    private static void parseParticles(String str, int from, List<ParsedParticle> particles) {
        int brOpen = str.indexOf('(', from);
        if (brOpen < 0)
            return;
        int brClose = str.indexOf(')', from);

        var name = str.substring(from, brOpen);
        if (name.matches(".*[+-_^].*]"))
            throw new IllegalArgumentException();
        var comma = str.indexOf(',', brOpen);
        var index = Integer.parseInt(str.substring(brOpen + 1, comma));
        var momenta = str.substring(comma + 1, brClose);
        if (momenta.matches(".*[+-_^].*]"))
            throw new IllegalArgumentException();


        particles.add(new ParsedParticle(index, name, momenta));
        if (brClose < str.length() - 1 && str.charAt(brClose + 1) == ',')
            brClose += 1;
        parseParticles(str, brClose + 1, particles);
    }

    private static final class ParsedParticle {
        final int index;
        final String particle, momenta;

        public ParsedParticle(int index, String particle, String momenta) {
            this.index = index;
            this.particle = particle;
            this.momenta = momenta;
        }
    }
}

