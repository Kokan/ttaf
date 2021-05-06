package dog.giraffe.gui;

import dog.giraffe.gui.model.Transform;
import javax.swing.JPanel;

public class TransformCardEmpty extends TransformCard<Transform> {
    public TransformCardEmpty(OutputPanel outputPanel) {
        super(new JPanel(), outputPanel, Transform.class);
    }

    @Override
    void modelChanged() {
    }
}
