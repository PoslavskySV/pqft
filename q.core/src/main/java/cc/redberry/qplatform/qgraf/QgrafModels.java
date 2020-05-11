package cc.redberry.qplatform.qgraf;

import java.util.List;

/** Sample models for QGRAF tests */
public class QgrafModels {

    public static final class Gravity {
        static final QgrafParticle
                tetrad = new QgrafParticle(),
                connection = new QgrafParticle();
        static final QgrafPropagator
                tetradProp = new QgrafPropagator(),
                connectionProp = new QgrafPropagator();
        static final QgrafVertex
                ththth = new QgrafVertex(tetrad, tetrad, tetrad),
                ththom = new QgrafVertex(tetrad, tetrad, connection),
                thomom = new QgrafVertex(tetrad, connection, connection);

        static final QgrafModelDescription model = new QgrafModelDescription();

        static {
            tetrad.name = "th";
            tetrad.isFermion = false;

            connection.name = "om";
            connection.isFermion = false;

            tetradProp.particle = tetrad;
            tetradProp.antiparticle = tetrad;
            tetradProp.functionName = "thP";
            tetradProp.mass = "mth";

            connectionProp.particle = connection;
            connectionProp.antiparticle = connection;
            connectionProp.functionName = "omP";
            connectionProp.mass = "mom";

            model.name = "tetradGravity";
            model.propagators = List.of(tetradProp, connectionProp);
            model.vertices = List.of(ththth, ththom, thomom);
        }
    }

    public static QgrafModelDescription gravity() {
        return Gravity.model;
    }

    public static final class Qed {
        static final QgrafParticle electron = new QgrafParticle();
        static final QgrafParticle positron = new QgrafParticle();
        static final QgrafParticle photon = new QgrafParticle();
        static final QgrafPropagator
                electronProp = new QgrafPropagator(),
                photonProp = new QgrafPropagator();
        static final QgrafVertex vertex = new QgrafVertex(positron, electron, photon);

        static final QgrafModelDescription model = new QgrafModelDescription();

        static {
            electron.name = "em";
            electron.isFermion = true;

            positron.name = "ep";
            positron.isFermion = true;

            photon.name = "ga";
            photon.isFermion = false;

            electronProp.particle = electron;
            electronProp.antiparticle = positron;
            electronProp.mass = "me";
            electronProp.functionName = "emP";

            photonProp.particle = photon;
            photonProp.antiparticle = photon;
            photonProp.mass = "mga";
            photonProp.functionName = "gaP";

            model.name = "qed";
            model.propagators = List.of(photonProp, electronProp);
            model.vertices = List.of(vertex);
        }
    }

    public static QgrafModelDescription qed() {
        return Qed.model;
    }

    public static QgrafModelDescription gravityQed() {
        return QgrafModelDescription.merge(gravity(), qed()
                , new QgrafVertex(Qed.positron, Qed.electron, Gravity.connection)
                , new QgrafVertex(Qed.positron, Qed.electron, Gravity.tetrad)
                , new QgrafVertex(Qed.positron, Qed.electron, Gravity.connection, Gravity.tetrad)
                , new QgrafVertex(Qed.positron, Qed.electron, Gravity.tetrad, Gravity.tetrad)
                , new QgrafVertex(Qed.positron, Qed.electron, Gravity.connection, Gravity.tetrad, Gravity.tetrad)
                , new QgrafVertex(Qed.positron, Qed.electron, Gravity.tetrad, Gravity.tetrad, Gravity.tetrad));
    }
}
