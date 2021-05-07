package dog.giraffe.gui;

import dog.giraffe.Icons;
import dog.giraffe.Pair;
import dog.giraffe.SwingContext;
import dog.giraffe.gui.model.Model;
import dog.giraffe.gui.model.Output;
import dog.giraffe.gui.model.Transform;
import dog.giraffe.image.BufferedImageReader;
import dog.giraffe.image.BufferedImageWriter;
import dog.giraffe.image.Image;
import dog.giraffe.image.ImageReader;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.image.transform.Mask;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import dog.giraffe.threads.Supplier;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

public class GUI {
    private class WindowListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent event) {
            if (null!=context) {
                context.close();
            }
        }
    }

    SwingContext context;
    JFrame frame;
    InputImagePanel inputImagePanel;
    MaskPanel maskPanel;
    Model model;
    final List<OutputPanel> outputPanels=new ArrayList<>();
    private Path settingsFile;
    private JLabel settingsFileLabel;
    private JTabbedPane tabbedPane;
    ViewerPanel viewerPanel;

    private void addOutput(ActionEvent event) {
        Output modelOutput=Output.create();
        OutputPanel outputPanel=new OutputPanel(this, modelOutput);
        model.outputs.add(modelOutput);
        outputPanels.add(outputPanel);
        tabbedPane.insertTab("Output", null, outputPanel.component(), null, outputPanels.size());
    }

    private void loadSettings(ActionEvent event) {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Settings file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        String userDir=System.getProperty("user.dir");
        if (null!=userDir) {
            chooser.setCurrentDirectory(Paths.get(userDir).toFile());
        }
        if (null!=settingsFile) {
            Path path=settingsFile.getParent();
            if (null!=path) {
                chooser.setCurrentDirectory(path.toFile());
            }
            chooser.setSelectedFile(settingsFile.toFile());
        }
        if (JFileChooser.APPROVE_OPTION==chooser.showOpenDialog(frame)) {
            loadSettings(chooser.getSelectedFile().toPath());
        }
    }

    private void loadSettings(Path path) {
        try {
            path=path.toAbsolutePath();
            model=Model.load(path);
            settingsFile=path;
            settingsFileLabel.setText(settingsFile.toString());
            modelChanged();
        }
        catch (Throwable throwable) {
            log(throwable);
        }
    }

    void log(Throwable throwable) {
        throwable.printStackTrace(System.err);
        JOptionPane.showMessageDialog(
                frame,
                throwable.toString(),
                "Error",
                JOptionPane.ERROR_MESSAGE);

    }

    private void main(Path settingsFile) throws Throwable {
        model=Model.create();

        frame=new JFrame("Giraffe");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImages(Icons.icons());
        frame.addWindowListener(new WindowListener());

        tabbedPane=new JTabbedPane(JTabbedPane.TOP);
        frame.getContentPane().add(new JScrollPane(tabbedPane));

        JPanel optionsPanel=new JPanel(new BorderLayout());
        tabbedPane.add("Input", optionsPanel);
        JPanel topPanel=new JPanel(new BorderLayout());
        optionsPanel.add(topPanel, BorderLayout.NORTH);

        JPanel settingsPanel=new JPanel(new BorderLayout());
        topPanel.add(settingsPanel, BorderLayout.NORTH);
        JPanel settingsButtonsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        settingsPanel.add(settingsButtonsPanel, BorderLayout.WEST);
        JButton loadSettings=new JButton("Load settings");
        loadSettings.addActionListener(this::loadSettings);
        settingsButtonsPanel.add(loadSettings);
        JButton saveSettings=new JButton("Save settings");
        saveSettings.addActionListener(this::saveSettings);
        settingsButtonsPanel.add(saveSettings);
        settingsFileLabel=new JLabel();
        settingsPanel.add(settingsFileLabel, BorderLayout.CENTER);

        inputImagePanel=new InputImagePanel(this);
        topPanel.add(inputImagePanel.component(), BorderLayout.CENTER);

        JPanel outputsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(outputsPanel, BorderLayout.SOUTH);
        JButton addOutput=new JButton("Add output");
        addOutput.addActionListener(this::addOutput);
        outputsPanel.add(addOutput);
        JButton renderAllOutputs=new JButton("Render all outputs");
        renderAllOutputs.addActionListener(this::renderAllOutputs);
        outputsPanel.add(renderAllOutputs);

        maskPanel=new MaskPanel(this);
        optionsPanel.add(maskPanel.component(), BorderLayout.CENTER);

        viewerPanel=new ViewerPanel(this);
        tabbedPane.add("Viewer", viewerPanel.component());

        modelChanged();

        frame.pack();
        if ((100>frame.getWidth())
                || (100>frame.getHeight())) {
            frame.setSize(Math.max(100, frame.getWidth()), Math.max(100, frame.getHeight()));
        }
        Dimension screenSize=frame.getToolkit().getScreenSize();
        frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        context=new SwingContext();

        frame.setVisible(true);

        if (null!=settingsFile) {
            loadSettings(settingsFile);
        }
        modelChanged();
    }

    public static void main(String[] args) throws Throwable {
        Path settingsFile=null;
        if (0<args.length) {
            settingsFile=Paths.get(args[0]);
        }
        new GUI().main(settingsFile);
    }

    private void modelChanged() throws Throwable {
        try {
            try {
                inputImagePanel.modelChanged();
            }
            finally {
                maskPanel.modelChanged();
            }
        }
        finally {
            try {
                outputPanels.forEach((outputPanel)->tabbedPane.remove(outputPanel.component()));
                outputPanels.clear();
                for (int ii=0; model.outputs.size()>ii; ++ii) {
                    OutputPanel outputPanel=new OutputPanel(this, model.outputs.get(ii));
                    outputPanels.add(outputPanel);
                    tabbedPane.insertTab("Output", null, outputPanel.component(), null, ii+1);
                }
                outputPanels.forEach(OutputPanel::modelChanged);
            }
            finally {
                viewerPanel.modelChanged();
            }
        }
    }

    void removeOutput(OutputPanel outputPanel) {
        outputPanels.remove(outputPanel);
        tabbedPane.remove(outputPanel.component());
        model.outputs.remove(outputPanel.modelOutput);
    }

    private void renderAllOutputs(ActionEvent event) {
        OutputPanel.renderOutputs(this, outputPanels);
    }

    <T> void run(String title, AsyncTask<T> task, Continuation<T> continuation) {
        try {
            SubContext subContext=new SubContext(context);
            ProgressBar progressBar=new ProgressBar(subContext, frame, title);
            Continuation<T> continuation2=Continuations.async(
                    Continuations.finallyBlock(
                            progressBar::dispose,
                            continuation),
                    context.executorGui());
            progressBar.show();
            try {
                context.executor().execute(()->{
                    try {
                        task.run(subContext, continuation2);
                    }
                    catch (Throwable throwable) {
                        continuation2.failed(throwable);
                    }
                });
            }
            catch (Throwable throwable) {
                continuation2.failed(throwable);
            }
        }
        catch (Throwable throwable) {
            log(throwable);
        }
    }

    private void saveSettings(ActionEvent event) {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Settings file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        String userDir=System.getProperty("user.dir");
        if (null!=userDir) {
            chooser.setCurrentDirectory(Paths.get(userDir).toFile());
        }
        if (null!=settingsFile) {
            Path path=settingsFile.getParent();
            if (null!=path) {
                chooser.setCurrentDirectory(path.toFile());
            }
            chooser.setSelectedFile(settingsFile.toFile());
        }
        if (JFileChooser.APPROVE_OPTION==chooser.showSaveDialog(frame)) {
            Path path=chooser.getSelectedFile().toPath().toAbsolutePath();
            if ((!Files.exists(path))
                    || (JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(
                            frame,
                            String.format("Do you want to overwrite %1$s?", path),
                            "Overwrite file?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE))) {
                settingsFile=path;
                settingsFileLabel.setText(settingsFile.toString());
                try {
                    model.save(settingsFile);
                }
                catch (Throwable throwable) {
                    log(throwable);
                }
            }
        }
    }
}
