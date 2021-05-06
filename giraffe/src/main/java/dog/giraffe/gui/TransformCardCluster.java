package dog.giraffe.gui;

import dog.giraffe.gui.model.Transform;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransformCardCluster extends TransformCard<Transform.Cluster> {
    private final JComboBox<Transform.Cluster.Algorithm> algorithm;
    private final JTextField bins;
    private final JTextField errorLimit;
    private final JCheckBox initialCentersFarthest;
    private final JCheckBox initialCentersKDTree;
    private final JTextField initialCentersRandom;
    private final JTextField maxClusters;
    private final JTextField maxIterations;
    private final JTextField minClusters;
    private final JCheckBox replaceEmptyCentersFarthest;
    private final JTextField replaceEmptyCentersRandom;
    private final JComboBox<Transform.Cluster.Type> type;

    public TransformCardCluster(OutputPanel outputPanel) {
        super(new JPanel(new GridBagLayout()), outputPanel, Transform.Cluster.class);
        cardPanel.add(
                new JLabel("Type:"),
                new GBC()
                        .gridX(0)
                        .gridY(0)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        type=new JComboBox<>(Transform.Cluster.Type.values());
        type.setEditable(false);
        cardPanel.add(
                type,
                new GBC()
                        .gridX(1)
                        .gridY(0)
                        .gridWidthRemainder()
                        .anchorLineStart()
                        .constraints());

        cardPanel.add(
                new JLabel("Algorithm:"),
                new GBC()
                        .gridX(0)
                        .gridY(1)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        algorithm=new JComboBox<>(Transform.Cluster.Algorithm.values());
        algorithm.setEditable(false);
        cardPanel.add(
                algorithm,
                new GBC()
                        .gridX(1)
                        .gridY(1)
                        .gridWidthRemainder()
                        .anchorLineStart()
                        .insets(new Insets(5, 0, 5, 0))
                        .constraints());

        cardPanel.add(
                new JLabel("Clusters: "),
                new GBC()
                        .gridX(0)
                        .gridY(2)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        minClusters=new JTextField();
        minClusters.setColumns(10);
        minClusters.setHorizontalAlignment(JTextField.RIGHT);
        cardPanel.add(
                minClusters,
                new GBC()
                        .gridX(1)
                        .gridY(2)
                        .constraints());
        cardPanel.add(
                new JLabel("-"),
                new GBC()
                        .gridX(2)
                        .gridY(2)
                        .constraints());
        maxClusters=new JTextField();
        maxClusters.setColumns(10);
        maxClusters.setHorizontalAlignment(JTextField.RIGHT);
        cardPanel.add(
                maxClusters,
                new GBC()
                        .gridX(3)
                        .gridY(2)
                        .constraints());

        cardPanel.add(
                new JLabel("Max. iterations:"),
                new GBC()
                        .gridX(0)
                        .gridY(3)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        maxIterations=new JTextField();
        maxIterations.setColumns(10);
        maxIterations.setHorizontalAlignment(JTextField.RIGHT);
        cardPanel.add(
                maxIterations,
                new GBC()
                        .gridX(1)
                        .gridY(3)
                        .insets(new Insets(5, 0, 5, 0))
                        .constraints());

        cardPanel.add(
                new JLabel("Error limit:"),
                new GBC()
                        .gridX(0)
                        .gridY(4)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        errorLimit=new JTextField();
        errorLimit.setColumns(10);
        errorLimit.setHorizontalAlignment(JTextField.RIGHT);
        cardPanel.add(
                errorLimit,
                new GBC()
                        .gridX(1)
                        .gridY(4)
                        .constraints());

        cardPanel.add(
                new JLabel("Bins:"),
                new GBC()
                        .gridX(0)
                        .gridY(5)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        bins=new JTextField();
        bins.setColumns(10);
        bins.setHorizontalAlignment(JTextField.RIGHT);
        cardPanel.add(
                bins,
                new GBC()
                        .gridX(1)
                        .gridY(5)
                        .insets(new Insets(5, 0, 5, 0))
                        .constraints());

        cardPanel.add(
                new JLabel("Initial centers:"),
                new GBC()
                        .gridX(0)
                        .gridY(6)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        JPanel initialCentersPanel=new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        cardPanel.add(
                initialCentersPanel,
                new GBC()
                        .gridX(1)
                        .gridY(6)
                        .gridWidthRemainder()
                        .anchorLineStart()
                        .constraints());
        initialCentersFarthest=new JCheckBox("farthest");
        initialCentersPanel.add(initialCentersFarthest);
        initialCentersPanel.add(new JLabel("random:"));
        initialCentersRandom=new JTextField();
        initialCentersRandom.setColumns(10);
        initialCentersRandom.setHorizontalAlignment(JTextField.RIGHT);
        initialCentersPanel.add(initialCentersRandom);
        initialCentersKDTree=new JCheckBox("kd-tree");
        initialCentersPanel.add(initialCentersKDTree);

        cardPanel.add(
                new JLabel("Replace empty centers:"),
                new GBC()
                        .gridX(0)
                        .gridY(7)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        JPanel replaceEmptyCentersPanel=new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        cardPanel.add(
                replaceEmptyCentersPanel,
                new GBC()
                        .gridX(1)
                        .gridY(7)
                        .gridWidthRemainder()
                        .anchorLineStart()
                        .insets(new Insets(5, 0, 5, 0))
                        .constraints());
        replaceEmptyCentersFarthest=new JCheckBox("farthest");
        replaceEmptyCentersPanel.add(replaceEmptyCentersFarthest);
        replaceEmptyCentersPanel.add(new JLabel("random:"));
        replaceEmptyCentersRandom=new JTextField();
        replaceEmptyCentersRandom.setColumns(10);
        replaceEmptyCentersRandom.setHorizontalAlignment(JTextField.RIGHT);
        replaceEmptyCentersPanel.add(replaceEmptyCentersRandom);

        JButton set=new JButton("Set");
        set.addActionListener(this::set);
        cardPanel.add(
                set,
                new GBC()
                        .gridX(0)
                        .gridY(8)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());

        cardPanel.add(
                new JPanel(),
                new GBC()
                        .gridX(4)
                        .gridY(9)
                        .weightX(1.0)
                        .weightY(1.0)
                        .constraints());
    }

    @Override
    void modelChanged() {
        algorithm.setSelectedItem(transform.algorithm);
        bins.setText(Integer.toString(transform.bins));
        errorLimit.setText(Double.toString(transform.errorLimit));
        initialCentersFarthest.setSelected(transform.initialCentersMeanAndFarthest);
        initialCentersKDTree.setSelected(transform.initialCentersKDTree);
        initialCentersRandom.setText(Integer.toString(transform.initialCentersRandom));
        maxClusters.setText(Integer.toString(transform.maxClusters));
        maxIterations.setText(Integer.toString(transform.maxIterations));
        minClusters.setText(Integer.toString(transform.minClusters));
        replaceEmptyCentersFarthest.setSelected(transform.replaceEmptyClustersFarthest);
        replaceEmptyCentersRandom.setText(Integer.toString(transform.replaceEmptyClustersRandom));
        type.setSelectedItem(transform.type);
    }

    private void set(ActionEvent event) {
        try {
            Transform.Cluster newTransform=new Transform.Cluster();
            newTransform.algorithm=(Transform.Cluster.Algorithm)algorithm.getSelectedItem();
            newTransform.bins=Integer.parseInt(bins.getText().trim());
            newTransform.errorLimit=Double.parseDouble(errorLimit.getText().trim());
            newTransform.initialCentersKDTree=initialCentersKDTree.isSelected();
            newTransform.initialCentersMeanAndFarthest=initialCentersFarthest.isSelected();
            newTransform.initialCentersRandom=Integer.parseInt(initialCentersRandom.getText().trim());
            newTransform.maxClusters=Integer.parseInt(maxClusters.getText().trim());
            newTransform.maxIterations=Integer.parseInt(maxIterations.getText().trim());
            newTransform.minClusters=Integer.parseInt(minClusters.getText().trim());
            newTransform.replaceEmptyClustersFarthest=replaceEmptyCentersFarthest.isSelected();
            newTransform.replaceEmptyClustersRandom=Integer.parseInt(replaceEmptyCentersRandom.getText().trim());
            newTransform.type=(Transform.Cluster.Type)type.getSelectedItem();
            outputPanel.transformChanged(newTransform);
        }
        catch (Throwable throwable) {
            outputPanel.gui.log(throwable);
        }
    }
}
