package dog.giraffe.gui;

import dog.giraffe.image.FileImageReader;
import dog.giraffe.image.ImageReader;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

class InputImagePanel {
    private final JTextField channels;
    private final GUI gui;
    private final JTextField height;
    private final JTextField inputFile;
    private final JPanel topPanel;
    private final JTextField width;

    InputImagePanel(GUI gui) {
        this.gui=gui;
        topPanel=new JPanel(new GridBagLayout());
        JButton chooseInput=new JButton("Input file");
        chooseInput.addActionListener(this::chooseInput);
        topPanel.add(
                chooseInput,
                new GBC()
                        .gridX(0)
                        .gridY(0)
                        .insets(new Insets(0, 5, 0, 0))
                        .constraints());
        inputFile=new JTextField();
        inputFile.setEditable(false);
        topPanel.add(
                inputFile,
                new GBC()
                        .gridX(1)
                        .gridY(0)
                        .gridWidthRemainder()
                        .weightX(1.0)
                        .anchorLineStart()
                        .fillHorizontal()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());

        topPanel.add(
                new JLabel("width: "),
                new GBC()
                        .gridX(0)
                        .gridY(1)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        width=new JTextField();
        width.setHorizontalAlignment(JTextField.RIGHT);
        width.setColumns(8);
        width.setEditable(false);
        topPanel.add(
                width,
                new GBC()
                        .gridX(1)
                        .gridY(1)
                        .anchorLineStart()
                        .fillVertical()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());

        topPanel.add(
                new JLabel("height: "),
                new GBC()
                        .gridX(0)
                        .gridY(2)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        height=new JTextField();
        height.setHorizontalAlignment(JTextField.RIGHT);
        height.setColumns(8);
        height.setEditable(false);
        topPanel.add(
                height,
                new GBC()
                        .gridX(1)
                        .gridY(2)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());

        topPanel.add(
                new JLabel("channels: "),
                new GBC()
                        .gridX(0)
                        .gridY(3)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
        channels=new JTextField();
        channels.setHorizontalAlignment(JTextField.RIGHT);
        channels.setColumns(8);
        channels.setEditable(false);
        topPanel.add(
                channels,
                new GBC()
                        .gridX(1)
                        .gridY(3)
                        .anchorLineStart()
                        .insets(new Insets(0, 7, 0, 0))
                        .constraints());
    }

    private void chooseInput(ActionEvent event) {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Input image");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        String userDir=System.getProperty("user.dir");
        if (null!=userDir) {
            chooser.setCurrentDirectory(Paths.get(userDir).toFile());
        }
        if (null!=gui.model.inputFile) {
            Path path=Paths.get(gui.model.inputFile).getParent();
            if (null!=path) {
                chooser.setCurrentDirectory(path.toFile());
            }
        }
        if (JFileChooser.APPROVE_OPTION==chooser.showOpenDialog(gui.frame)) {
            gui.model.inputFile=chooser.getSelectedFile().toString();
            try {
                modelChanged();
            }
            catch (Throwable throwable) {
                gui.log(throwable);
            }
        }
    }

    public JComponent component() {
        return topPanel;
    }

    void modelChanged() throws Throwable {
        inputFile.setText("");
        width.setText("-");
        height.setText("-");
        channels.setText("-");
        if (null!=gui.model.inputFile) {
            inputFile.setText(gui.model.inputFile);
            try (ImageReader ir=FileImageReader.create(Paths.get(gui.model.inputFile))) {
                width.setText(Integer.toString(ir.width()));
                height.setText(Integer.toString(ir.height()));
                channels.setText(Integer.toString(ir.dimensions()));
            }
        }
    }
}
