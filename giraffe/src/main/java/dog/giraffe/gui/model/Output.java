package dog.giraffe.gui.model;

import java.util.List;

public class Output {
    public List<Transform> transforms;

    public static Output create() {
        Output output=new Output();
        output.fix();
        return output;
    }

    void fix() {
        transforms=Model.fix(transforms);
        transforms.forEach(Transform::fix);
    }
}
