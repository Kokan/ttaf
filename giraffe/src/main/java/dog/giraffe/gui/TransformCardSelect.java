package dog.giraffe.gui;

import dog.giraffe.gui.model.Transform;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransformCardSelect extends TransformCard<Transform.Select> {
    private final JTextField selectedChannels;

    public TransformCardSelect(OutputPanel outputPanel) {
        super(new JPanel(new BorderLayout()), outputPanel, Transform.Select.class);
        JPanel selectedChannelsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardPanel.add(selectedChannelsPanel, BorderLayout.NORTH);
        JLabel label=new JLabel("Selected channels:");
        selectedChannelsPanel.add(label);
        selectedChannels=new JTextField();
        selectedChannels.setColumns(10);
        selectedChannels.setHorizontalAlignment(JTextField.RIGHT);
        selectedChannelsPanel.add(selectedChannels);

        JPanel setPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardPanel.add(setPanel, BorderLayout.CENTER);
        JButton set=new JButton("Set");
        set.addActionListener(this::set);
        setPanel.add(set);
    }

    @Override
    void modelChanged() {
        StringBuilder sb=new StringBuilder();
        for (int ii=0; transform.selectedChannels.size()>ii; ++ii) {
            if (0<ii) {
                sb.append(",");
            }
            sb.append(transform.selectedChannels.get(ii));
        }
        selectedChannels.setText(sb.toString());
    }

    private void set(ActionEvent event) {
        try {
            Transform.Select newTransform=new Transform.Select();
            newTransform.selectedChannels=new ArrayList<>();
            for (String channel: selectedChannels.getText().split(",")) {
                newTransform.selectedChannels.add(Integer.parseInt(channel.trim()));
            }
            outputPanel.transformChanged(newTransform);
        }
        catch (Throwable throwable) {
            outputPanel.gui.log(throwable);
        }
    }
}
