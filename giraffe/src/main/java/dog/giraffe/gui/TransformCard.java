package dog.giraffe.gui;

import dog.giraffe.gui.model.Transform;
import javax.swing.JComponent;
import javax.swing.JPanel;

public abstract class TransformCard<T extends Transform> {
    protected final JPanel cardPanel;
    protected final OutputPanel outputPanel;
    protected T transform;
    protected final Class<T> type;

    public TransformCard(JPanel cardPanel, OutputPanel outputPanel, Class<T> type) {
        this.cardPanel=cardPanel;
        this.outputPanel=outputPanel;
        this.type=type;
    }

    public JComponent component() {
        return cardPanel;
    }

    abstract void modelChanged();

    void setTransform(Transform transform) {
        this.transform=type.cast(transform);
        modelChanged();
    }
}
