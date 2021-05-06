package dog.giraffe.gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;

public class GBC {
    private final GridBagConstraints constraints=new GridBagConstraints();

    public GBC anchorLineEnd() {
        constraints.anchor=GridBagConstraints.LINE_END;
        return this;
    }

    public GBC anchorLineStart() {
        constraints.anchor=GridBagConstraints.LINE_START;
        return this;
    }

    public GridBagConstraints constraints() {
        return constraints;
    }

    public GBC fillHorizontal() {
        constraints.fill=GridBagConstraints.HORIZONTAL;
        return this;
    }

    public GBC fillVertical() {
        constraints.fill=GridBagConstraints.VERTICAL;
        return this;
    }

    public GBC gridHeightRemainder() {
        constraints.gridheight=GridBagConstraints.REMAINDER;
        return this;
    }

    public GBC gridWidthRemainder() {
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        return this;
    }

    public GBC gridX(int gridX) {
        constraints.gridx=gridX;
        return this;
    }

    public GBC gridY(int gridY) {
        constraints.gridy=gridY;
        return this;
    }

    public GBC insets(Insets insets) {
        constraints.insets=insets;
        return this;
    }

    public GBC iPadX(int iPadX) {
        constraints.ipadx=iPadX;
        return this;
    }

    public GBC weightX(double weightX) {
        constraints.weightx=weightX;
        return this;
    }

    public GBC weightY(double weightY) {
        constraints.weighty=weightY;
        return this;
    }
}
