package dog.giraffe.gui;

import dog.giraffe.gui.model.HalfPlane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

class MaskPanel {
    private class ListModel extends AbstractListModel<String> {
        private static final long serialVersionUID=0L;

        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }

        @Override
        public String getElementAt(int index) {
            HalfPlane halfPlane=gui.model.mask.get(index);
            return String.format(
                    "(%1$.1f,%2$.1f)->(%3$.1f,%4$.1f)",
                    halfPlane.x1, halfPlane.y1,
                    halfPlane.x2, halfPlane.y2);
        }

        @Override
        public int getSize() {
            return gui.model.mask.size();
        }
    }

    private final GUI gui;
    private final JList<String> list;
    private final ListModel listModel=new ListModel();
    private final JPanel maskPanel;
    private final JTextField[] textFields=new JTextField[4];

    MaskPanel(GUI gui) {
        this.gui=gui;
        maskPanel=new JPanel(new BorderLayout());

        JPanel topPanel=new JPanel(new BorderLayout());
        maskPanel.add(topPanel, BorderLayout.NORTH);

        JPanel topLabelPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(topLabelPanel, BorderLayout.NORTH);
        JLabel topLabel=new JLabel("Mask:");
        topLabelPanel.add(topLabel);

        JPanel topButtonsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(topButtonsPanel, BorderLayout.SOUTH);
        JButton addButton=new JButton("Add");
        addButton.addActionListener(this::addHalfPlane);
        topButtonsPanel.add(addButton);
        JButton removeButton=new JButton("Remove");
        removeButton.addActionListener(this::removeHalfPlane);
        topButtonsPanel.add(removeButton);

        list=new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(this::selectionChanged);
        list.setFixedCellWidth(
                list.getFontMetrics(list.getFont()).stringWidth("(9999.9,9999.9)->(9999.9,9999.9)9999"));
        maskPanel.add(new JScrollPane(list), BorderLayout.WEST);

        JPanel formPanel=new JPanel(new GridBagLayout());
        maskPanel.add(formPanel, BorderLayout.CENTER);

        for (int ii=0; 4>ii; ++ii) {
            JLabel label=new JLabel(((0==ii%2)?"x":"y")+((ii/2)+1)+":");
            formPanel.add(
                    label,
                    new GBC()
                            .gridX(0)
                            .gridY(ii)
                            .insets(new Insets(5, 7, 5, 5))
                            .constraints());
            JTextField textField=new JTextField();
            textFields[ii]=textField;
            textField.setColumns(10);
            textField.setHorizontalAlignment(JTextField.RIGHT);
            formPanel.add(
                    textField,
                    new GBC()
                            .gridX(1)
                            .gridY(ii)
                            .constraints());
        }

        JButton setButton=new JButton("Set");
        setButton.addActionListener(this::setHalfPlane);
        formPanel.add(
                setButton,
                new GBC()
                        .anchorLineStart()
                        .gridX(0)
                        .gridY(4)
                        .gridWidthRemainder()
                        .insets(new Insets(3, 7, 0, 0))
                        .constraints());

        formPanel.add(
                new JPanel(),
                new GBC()
                        .gridX(2)
                        .gridY(5)
                        .gridWidthRemainder()
                        .gridHeightRemainder()
                        .weightX(1.0)
                        .weightY(1.0)
                        .constraints());
    }

    private void addHalfPlane(ActionEvent event) {
        HalfPlane halfPlane=new HalfPlane();
        halfPlane.x1=0.0;
        halfPlane.y1=0.0;
        halfPlane.x2=0.0;
        halfPlane.y2=1.0+gui.model.mask.size();
        int index=gui.model.mask.size();
        gui.model.mask.add(halfPlane);
        listModel.fireContentsChanged(listModel, index, index);
        list.setSelectedIndex(index);
    }

    JComponent component() {
        return maskPanel;
    }

    void modelChanged() {
        listModel.fireContentsChanged(listModel, 0, Integer.MAX_VALUE);
        if (!gui.model.mask.isEmpty()) {
            list.setSelectedIndex(0);
            selectionChanged(null);
        }
    }

    private void removeHalfPlane(ActionEvent event) {
        int index=list.getSelectedIndex();
        if ((0<=index)
                && (gui.model.mask.size()>index)) {
            gui.model.mask.remove(index);
            listModel.fireContentsChanged(listModel, index, gui.model.mask.size());
            if (!gui.model.mask.isEmpty()) {
                list.setSelectedIndex(
                        (gui.model.mask.size()>index)
                                ?index
                                :(index-1));
                selectionChanged(null);
            }
            if ((0<index)
                    && (gui.model.mask.size()<=index)) {
                list.setSelectedIndex(index-1);
            }
        }
    }

    private void selectionChanged(ListSelectionEvent event) {
        int index=list.getSelectedIndex();
        if ((0<=index)
                && (gui.model.mask.size()>index)) {
            HalfPlane halfPlane=gui.model.mask.get(index);
            textFields[0].setText(Double.toString(halfPlane.x1));
            textFields[1].setText(Double.toString(halfPlane.y1));
            textFields[2].setText(Double.toString(halfPlane.x2));
            textFields[3].setText(Double.toString(halfPlane.y2));
        }
        else {
            for (JTextField textField: textFields) {
                textField.setText("");
            }
        }
    }

    private void setHalfPlane(ActionEvent event) {
        int index=list.getSelectedIndex();
        if ((0>index)
                || (gui.model.mask.size()<=index)) {
            return;
        }
        HalfPlane halfPlane=new HalfPlane();
        try {
            halfPlane.x1=Double.parseDouble(textFields[0].getText().trim());
            halfPlane.y1=Double.parseDouble(textFields[1].getText().trim());
            halfPlane.x2=Double.parseDouble(textFields[2].getText().trim());
            halfPlane.y2=Double.parseDouble(textFields[3].getText().trim());
        }
        catch (NumberFormatException ex) {
            gui.log(ex);
            return;
        }
        gui.model.mask.set(index, halfPlane);
        listModel.fireContentsChanged(listModel, index, index);
    }
}
