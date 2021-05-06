package dog.giraffe.gui;

import dog.giraffe.Pair;
import dog.giraffe.gui.model.Output;
import dog.giraffe.gui.model.Transform;
import dog.giraffe.image.BufferedImageReader;
import dog.giraffe.image.BufferedImageWriter;
import dog.giraffe.image.Image;
import dog.giraffe.image.ImageReader;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.image.transform.Mask;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import dog.giraffe.threads.Supplier;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

class OutputPanel {
    private static class LogModel extends AbstractTableModel {
        private static final long serialVersionUID=0L;

        private final List<Map.Entry<String, Object>> rows=new ArrayList<>();

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return (0==column)
                    ?"Log"
                    :"";
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Map.Entry<String, Object> entry=rows.get(rowIndex);
            return (0==columnIndex)
                    ?entry.getKey()
                    :String.valueOf(entry.getValue());
        }

        private void setLog(Map<String, Object> log) {
            rows.clear();
            rows.addAll(log.entrySet());
            fireTableDataChanged();
        }
    }

    private class TransformsModel extends AbstractListModel<String> {
        private static final long serialVersionUID=0L;

        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }

        @Override
        public String getElementAt(int index) {
            return modelOutput.transforms.get(index).visit(Outputs.TRANSFORM_TO_STRING);
        }

        @Override
        public int getSize() {
            return modelOutput.transforms.size();
        }
    }

    final GUI gui;
    List<BufferedImage> images;
    private final LogModel logModel=new LogModel();
    final Output modelOutput;
    private final JPanel outputPanel;
    private final ImageComponent preview;
    private final Map<String, TransformCard<?>> transformCards;
    private final CardLayout transformCardsLayout=new CardLayout();
    private final JPanel transformCardsPanel;
    private final JList<String> transforms;
    private final TransformsModel transformsModel=new TransformsModel();

    public OutputPanel(GUI gui, Output modelOutput) {
        this.gui=gui;
        this.modelOutput=modelOutput;
        outputPanel=new JPanel(new BorderLayout());

        JPanel topButtonsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputPanel.add(topButtonsPanel, BorderLayout.NORTH);
        JButton removeOutput=new JButton("Remove output");
        removeOutput.addActionListener(this::removeOutput);
        topButtonsPanel.add(removeOutput);

        JPanel transformsPanel=new JPanel(new BorderLayout());
        outputPanel.add(transformsPanel, BorderLayout.CENTER);

        JPanel transformsLabelPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        transformsPanel.add(transformsLabelPanel, BorderLayout.NORTH);
        transformsLabelPanel.add(new JLabel("Transforms:"));

        JPanel transformsLeftPanel=new JPanel(new BorderLayout());
        transformsPanel.add(transformsLeftPanel, BorderLayout.WEST);
        JPanel transformsLeftButtonPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        transformsLeftPanel.add(transformsLeftButtonPanel, BorderLayout.NORTH);
        JButton addTransform=new JButton("Add");
        addTransform.addActionListener(this::addTransform);
        transformsLeftButtonPanel.add(addTransform);
        JButton removeTransform=new JButton("Remove");
        removeTransform.addActionListener(this::removeTransform);
        transformsLeftButtonPanel.add(removeTransform);
        JButton upTransform=new JButton("Up");
        upTransform.addActionListener(this::upTransform);
        transformsLeftButtonPanel.add(upTransform);
        JButton downTransform=new JButton("Down");
        downTransform.addActionListener(this::downTransform);
        transformsLeftButtonPanel.add(downTransform);

        transforms=new JList<>(transformsModel);
        transforms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transforms.addListSelectionListener(this::transformSelectionChanged);
        transformsLeftPanel.add(new JScrollPane(transforms), BorderLayout.CENTER);

        transformCards=Map.of(
                Outputs.CARD_CLUSTER, new TransformCardCluster(this),
                Outputs.CARD_EMPTY, new TransformCardEmpty(this),
                Outputs.CARD_NORMALIZE_DEVIATION, new TransformCardNormalizedDeviation(this),
                Outputs.CARD_NORMALIZED_HYPER_HUE, new TransformCardNormalizedHyperHue(this),
                Outputs.CARD_SELECT, new TransformCardSelect(this));
        transformCardsPanel=new JPanel(transformCardsLayout);
        transformCards.forEach((key, value)->transformCardsPanel.add(value.component(), key));
        transformCardsLayout.show(transformCardsPanel, Outputs.CARD_EMPTY);
        transformsPanel.add(transformCardsPanel, BorderLayout.CENTER);

        JPanel renderPanel=new JPanel(new BorderLayout(5, 5));
        outputPanel.add(renderPanel, BorderLayout.SOUTH);

        JPanel renderLeftPanel=new JPanel(new BorderLayout());
        renderPanel.add(renderLeftPanel, BorderLayout.WEST);

        JPanel renderLeftButtonsPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        renderLeftPanel.add(renderLeftButtonsPanel, BorderLayout.NORTH);
        JButton renderOutput=new JButton("Render");
        renderOutput.addActionListener(this::renderOutput);
        renderLeftButtonsPanel.add(renderOutput);
        JButton saveOutput=new JButton("Save");
        saveOutput.addActionListener(this::saveOutput);
        renderLeftButtonsPanel.add(saveOutput);

        preview=new ImageComponent();
        preview.setPreferredSize(new Dimension(480, 360));
        preview.setImages(Images.NO_IMAGE);
        renderLeftPanel.add(preview, BorderLayout.CENTER);

        JTable logTable=new JTable(logModel);
        renderPanel.add(new JScrollPane(logTable), BorderLayout.CENTER);
    }

    private void addTransform(ActionEvent event) {
        JPopupMenu menu=new JPopupMenu();
        Outputs.TRANSFORM_FACTORIES.forEach((key, value)->{
            JMenuItem item=new JMenuItem(key);
            item.addActionListener((event2)->addTransform(value));
            menu.add(item);
        });
        JComponent component=(JComponent)event.getSource();
        menu.show(component, component.getX(), component.getY());
    }

    private void addTransform(Supplier<Transform> transformFactory) {
        try {
            Transform transform=transformFactory.get();
            int index=modelOutput.transforms.size();
            modelOutput.transforms.add(transform);
            transformsModel.fireContentsChanged(transformsModel, index, index);
            transforms.setSelectedIndex(index);
            transformSelectionChanged(null);
        }
        catch (Throwable throwable) {
            gui.log(throwable);
        }
    }

    JComponent component() {
        return outputPanel;
    }

    private void downTransform(ActionEvent event) {
        int index=transforms.getSelectedIndex();
        if (modelOutput.transforms.size()-1>index) {
            modelOutput.transforms.set(
                    index,
                    modelOutput.transforms.set(
                            index+1,
                            modelOutput.transforms.get(index)));
            transformsModel.fireContentsChanged(transformsModel, index, index+1);
            transforms.setSelectedIndex(index+1);
        }
    }

    void modelChanged() {
        if (!modelOutput.transforms.isEmpty()) {
            transforms.setSelectedIndex(0);
            transformSelectionChanged(null);
        }
    }

    private void removeOutput(ActionEvent event) {
        gui.removeOutput(this);
    }

    private void removeTransform(ActionEvent event) {
        int index=transforms.getSelectedIndex();
        if ((0<=index)
                && (modelOutput.transforms.size()>index)) {
            modelOutput.transforms.remove(index);
            transformsModel.fireContentsChanged(transformsModel, index, modelOutput.transforms.size());
            if (!modelOutput.transforms.isEmpty()) {
                transforms.setSelectedIndex(
                        (modelOutput.transforms.size()>index)
                                ?index
                                :(index-1));
                transformSelectionChanged(null);
            }
            if ((0<index)
                    && (modelOutput.transforms.size()<=index)) {
                transforms.setSelectedIndex(index-1);
            }
        }
        transformSelectionChanged(null);
    }

    private void renderOutput(ActionEvent event) {
        logModel.setLog(Collections.emptyMap());
        images=null;
        preview.setImages(Images.NO_IMAGE);
        gui.<Pair<List<BufferedImage>, Map<String, Object>>>run(
                "render",
                (context, continuation)->{
                    if (null==gui.model.inputFile) {
                        throw new RuntimeException("no input file selected");
                    }
                    Mask mask=Outputs.mask(gui.model.mask);
                    Function<Image, Image> imageMap=Function.identity();
                    for (Transform transform: modelOutput.transforms) {
                        imageMap=Outputs.transformToImageMap(mask, transform).compose(imageMap);
                    }
                    Supplier<ImageReader> imageReader=BufferedImageReader.factory(gui.model.inputFile);
                    AtomicReference<BufferedImage> imageResult=new AtomicReference<>();
                    AtomicReference<Map<String, Object>> logResult=new AtomicReference<>();
                    ImageWriter.Factory imageWriter=BufferedImageWriter.factory(imageResult::set);
                    ImageWriter.write(
                            context,
                            imageMap,
                            imageReader,
                            imageWriter,
                            logResult::set,
                            Continuations.map(
                                    (input, continuation2)->
                                        continuation2.completed(new Pair<>(
                                                Images.resize(context::checkStopped, imageResult.get()),
                                                logResult.get())),
                                    continuation));
                },
                new Continuation<>() {
                    @Override
                    public void completed(Pair<List<BufferedImage>, Map<String, Object>> result) {
                        images=result.first;
                        preview.setImages(images);
                        logModel.setLog(result.second);
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        gui.log(throwable);
                    }
                });
    }

    private void saveOutput(ActionEvent event) {
        BufferedImage image=null;
        if (null!=images) {
            for (BufferedImage image2: images) {
                if ((null==image)
                        || (image2.getWidth()*image2.getHeight()>image.getWidth()*image.getHeight())) {
                    image=image2;
                }
            }
        }
        if (null==image) {
            gui.log(new RuntimeException("no image rendered"));
            return;
        }
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Output file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        String userDir=System.getProperty("user.dir");
        if (null!=userDir) {
            chooser.setCurrentDirectory(Paths.get(userDir).toFile());
        }
        if (null!=gui.model.inputFile) {
            Path path=gui.model.inputFile.getParent();
            if (null!=path) {
                chooser.setCurrentDirectory(path.toFile());
            }
        }
        if (JFileChooser.APPROVE_OPTION==chooser.showSaveDialog(gui.frame)) {
            Path path=chooser.getSelectedFile().toPath().toAbsolutePath();
            if ((!Files.exists(path))
                    || (JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(
                    gui.frame,
                    String.format("Do you want to overwrite %1$s?", path),
                    "Overwrite file?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE))) {
                try {
                    ImageIO.write(image, ImageWriter.outputFormat(null, path), path.toFile());
                }
                catch (Throwable throwable) {
                    gui.log(throwable);
                }
            }
        }
    }

    void transformChanged(Transform transform) {
        int index=transforms.getSelectedIndex();
        if ((0<=index)
                && (modelOutput.transforms.size()>index)) {
            modelOutput.transforms.set(index, transform);
            transformsModel.fireContentsChanged(transformsModel, index, index);
        }
    }

    private void transformSelectionChanged(ListSelectionEvent event) {
        int index=transforms.getSelectedIndex();
        if ((0<=index)
                && (modelOutput.transforms.size()>index)) {
            Transform transform=modelOutput.transforms.get(index);
            String card=transform.visit(Outputs.TRANSFORM_CARDS);
            transformCardsLayout.show(transformCardsPanel, card);
            transformCards.get(card).setTransform(transform);
        }
        else {
            transformCardsLayout.show(transformCardsPanel, Outputs.CARD_EMPTY);
        }
    }

    private void upTransform(ActionEvent event) {
        int index=transforms.getSelectedIndex();
        if (0<index) {
            modelOutput.transforms.set(
                    index,
                    modelOutput.transforms.set(
                            index-1,
                            modelOutput.transforms.get(index)));
            transformsModel.fireContentsChanged(transformsModel, index-1, index);
            transforms.setSelectedIndex(index-1);
        }
    }
}
