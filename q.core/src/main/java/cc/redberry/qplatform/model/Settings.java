package cc.redberry.qplatform.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 *
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Settings {
    public String GammaMatrixName = "GA";
    public String LeviCivitaName = "eps";
    public String UnitaryMatrixName = "T";
    public String UnitarySymmetricStructureConstantName = "d";
    public String UnitaryAntisymmetricStructureConstantName = "f";
}
