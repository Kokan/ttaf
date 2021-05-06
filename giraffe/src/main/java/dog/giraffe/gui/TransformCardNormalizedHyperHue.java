package dog.giraffe.gui;

import dog.giraffe.gui.model.Transform;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransformCardNormalizedHyperHue extends TransformCard<Transform.NormalizedHyperHue> {
    private final JTextField maxZero;

    public TransformCardNormalizedHyperHue(OutputPanel outputPanel) {
        super(new JPanel(new BorderLayout()), outputPanel, Transform.NormalizedHyperHue.class);
        JPanel maxZeroPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardPanel.add(maxZeroPanel, BorderLayout.NORTH);
        JLabel label=new JLabel("Max. zero:");
        maxZeroPanel.add(label);
        maxZero=new JTextField();
        maxZero.setColumns(10);
        maxZero.setHorizontalAlignment(JTextField.RIGHT);
        maxZeroPanel.add(maxZero);

        JPanel setPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardPanel.add(setPanel, BorderLayout.CENTER);
        JButton set=new JButton("Set");
        set.addActionListener(this::set);
        setPanel.add(set);
    }

    @Override
    void modelChanged() {
        maxZero.setText(Double.toString(transform.maxZero));
    }

    private void set(ActionEvent event) {
        try {
            Transform.NormalizedHyperHue newTransform=new Transform.NormalizedHyperHue();
            newTransform.maxZero=Double.parseDouble(maxZero.getText().trim());
            outputPanel.transformChanged(newTransform);
        }
        catch (Throwable throwable) {
            outputPanel.gui.log(throwable);
        }
    }
}
