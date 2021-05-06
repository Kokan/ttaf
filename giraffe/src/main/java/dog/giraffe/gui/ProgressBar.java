package dog.giraffe.gui;

import dog.giraffe.Context;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressBar {
    private class WindowListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent event) {
            context.close();
        }
    }

    private final Context context;
    private final JDialog dialog;
    private final JFrame parent;

    public ProgressBar(Context context, JFrame parent, String title) {
        this.context=context;
        this.parent=parent;

        dialog=new JDialog(parent, title);
        dialog.addWindowListener(new WindowListener());

        JPanel panel=new JPanel(new BorderLayout());
        dialog.getContentPane().add(panel);

        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(topPanel, BorderLayout.NORTH);
        JProgressBar progressBar=new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString(title);
        progressBar.setStringPainted(true);
        topPanel.add(progressBar);

        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bottomPanel, BorderLayout.SOUTH);
        JButton cancel=new JButton("Cancel");
        cancel.addActionListener(this::cancel);
        bottomPanel.add(cancel);

        dialog.pack();
        Rectangle parentBounds=parent.getBounds();
        Dimension size=dialog.getSize();
        dialog.setLocation(
                parentBounds.x+(parentBounds.width-size.width)/2,
                parentBounds.y+(parentBounds.height-size.height)/2);
    }

    private void cancel(ActionEvent event) {
        context.close();
    }

    public void dispose() {
        dialog.dispose();
        parent.setEnabled(true);
        parent.requestFocus();
    }

    public void show() {
        dialog.setVisible(true);
        parent.setEnabled(false);
    }
}
