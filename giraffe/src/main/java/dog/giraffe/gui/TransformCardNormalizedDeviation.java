package dog.giraffe.gui;

import dog.giraffe.gui.model.Transform;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransformCardNormalizedDeviation extends TransformCard<Transform.NormalizeDeviation> {
    private final JTextField sigma;

    public TransformCardNormalizedDeviation(OutputPanel outputPanel) {
        super(new JPanel(new BorderLayout()), outputPanel, Transform.NormalizeDeviation.class);
        JPanel sigmaPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardPanel.add(sigmaPanel, BorderLayout.NORTH);
        JLabel label=new JLabel("Sigma:");
        sigmaPanel.add(label);
        sigma=new JTextField();
        sigma.setColumns(10);
        sigma.setHorizontalAlignment(JTextField.RIGHT);
        sigmaPanel.add(sigma);

        JPanel setPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardPanel.add(setPanel, BorderLayout.CENTER);
        JButton set=new JButton("Set");
        set.addActionListener(this::set);
        setPanel.add(set);
    }

    @Override
    void modelChanged() {
        sigma.setText(Double.toString(transform.sigma));
    }

    private void set(ActionEvent event) {
        try {
            Transform.NormalizeDeviation newTransform=new Transform.NormalizeDeviation();
            newTransform.sigma=Double.parseDouble(sigma.getText().trim());
            outputPanel.transformChanged(newTransform);
        }
        catch (Throwable throwable) {
            outputPanel.gui.log(throwable);
        }
    }
}
