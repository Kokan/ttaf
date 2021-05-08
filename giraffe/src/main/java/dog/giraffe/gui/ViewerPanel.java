package dog.giraffe.gui;

import dog.giraffe.util.Pair;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ViewerPanel {
    private static final double ZOOM=1.25;

    private class MouseListenerImpl implements MouseListener, MouseMotionListener, MouseWheelListener {
        private Pair<Integer, Integer> dragStart;
        private double dragTranslateX;
        private double dragTranslateY;

        @Override
        public void mouseClicked(MouseEvent event) {
            printCoordinates(event);
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (null!=dragStart) {
                translateX=dragTranslateX+(dragStart.first-event.getX())/viewSizes.zoom;
                translateY=dragTranslateY+(dragStart.second-event.getY())/viewSizes.zoom;
                invalidate();
            }
            printCoordinates(event);
        }

        @Override
        public void mouseEntered(MouseEvent event) {
            printCoordinates(event);
        }

        @Override
        public void mouseExited(MouseEvent event) {
            printCoordinates(event);
            dragStart=null;
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            printCoordinates(event);
        }

        @Override
        public void mousePressed(MouseEvent event) {
            printCoordinates(event);
            dragStart=new Pair<>(event.getX(), event.getY());
            dragTranslateX=translateX;
            dragTranslateY=translateY;
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            printCoordinates(event);
            dragStart=null;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            zoom(-event.getWheelRotation());
        }

        private void printCoordinates(MouseEvent event) {
            double cx=event.getX()%viewSizes.cellWidth;
            cx-=0.5*viewSizes.cellWidth;
            cx/=viewSizes.zoom;
            cx+=0.5*viewSizes.imageWidth+viewSizes.translateX;
            double cy=event.getY()%viewSizes.cellHeight;
            cy-=0.5*viewSizes.cellHeight;
            cy/=viewSizes.zoom;
            cy+=0.5*viewSizes.imageHeight+viewSizes.translateY;
            xx.setText(String.format("%1$.1f", cx));
            yy.setText(String.format("%1$.1f", cy));
        }
    }

    private static class Sizes {
        int cellHeight=1;
        int cellWidth=1;
        int columns=1;
        int images;
        int imageWidth=1;
        int imageHeight=1;
        int rows=1;
        double translateX;
        double translateY;
        int viewWidth=1;
        int viewHeight=1;
        double zoom=1;
    }

    private class View extends JComponent {
        private static final long serialVersionUID=0L;

        @Override
        protected void paintComponent(Graphics graphics1) {
            Graphics2D graphics=(Graphics2D)graphics1;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            sizes(viewSizes);
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.fillRect(0, 0, viewSizes.viewWidth, viewSizes.viewHeight);
            for (int ii=0; gui.outputPanels.size()>ii; ++ii) {
                List<BufferedImage> images=gui.outputPanels.get(ii).images;
                if ((null==images)
                        || images.isEmpty()) {
                    continue;
                }
                imageSize(images, viewSizes);
                BufferedImage image=selectImage(images, viewSizes);
                double zoom1=imageZoom(image, viewSizes);
                double zoom2=viewSizes.zoom/zoom1;
                double iw=image.getWidth();
                double ih=image.getHeight();
                int cc=ii%viewSizes.columns;
                int rr=ii/viewSizes.columns;
                double wl=viewSizes.translateX*zoom1+0.5*(iw-viewSizes.cellWidth/zoom2);
                double wr=viewSizes.translateX*zoom1+0.5*(iw+viewSizes.cellWidth/zoom2);
                double wt=viewSizes.translateY*zoom1+0.5*(ih-viewSizes.cellHeight/zoom2);
                double wb=viewSizes.translateY*zoom1+0.5*(ih+viewSizes.cellHeight/zoom2);
                if ((iw<=wl)
                        || (0.0>=wr)
                        || (ih<=wt)
                        || (0.0>=wb)) {
                    continue;
                }
                double cl=cc*viewSizes.cellWidth;
                double cr=(cc+1)*viewSizes.cellWidth;
                double ct=rr*viewSizes.cellHeight;
                double cb=(rr+1)*viewSizes.cellHeight;
                if (0.0>wl) {
                    cl=cr-(cr-cl)*wr/(wr-wl);
                    wl=0.0;
                }
                if (iw<wr) {
                    cr=cl+(cr-cl)*(iw-wl)/(wr-wl);
                    wr=iw;
                }
                if (0.0>wt) {
                    ct=cb-(cb-ct)*wb/(wb-wt);
                    wt=0.0;
                }
                if (ih<wb) {
                    cb=ct+(cb-ct)*(ih-wt)/(wb-wt);
                    wb=ih;
                }
                graphics.drawImage(
                        image,
                        (int)Math.round(cl),
                        (int)Math.round(ct),
                        (int)Math.round(cr),
                        (int)Math.round(cb),
                        (int)Math.round(wl),
                        (int)Math.round(wt),
                        (int)Math.round(wr),
                        (int)Math.round(wb),
                        graphics.getColor(),
                        this);
            }
        }

        private BufferedImage selectImage(List<BufferedImage> images, Sizes sizes) {
            BufferedImage bestImage=null;
            double bestZoom=0.0;
            for (BufferedImage image2: images) {
                double zoom2=imageZoom(image2, sizes);
                if ((null==bestImage)
                        || ((bestZoom>=sizes.zoom)
                                && (zoom2>=sizes.zoom)
                                && (bestZoom>zoom2))
                        || ((bestZoom<sizes.zoom)
                                && (zoom2>bestZoom))) {
                    bestImage=image2;
                    bestZoom=zoom2;
                }
            }
            return bestImage;
        }

        private double imageZoom(BufferedImage image, Sizes sizes) {
            return Math.min(1.0*image.getWidth()/sizes.imageWidth, 1.0*image.getHeight()/sizes.imageHeight);
        }
    }

    private final GUI gui;
    private final JPanel panel;
    private double translateX;
    private double translateY;
    private final View view=new View();
    private final Sizes viewSizes=new Sizes();
    private final JTextField xx;
    private final JTextField yy;
    private double zoom=1.0;

    public ViewerPanel(GUI gui) {
        this.gui=gui;
        panel=new JPanel(new BorderLayout());

        JPanel topPanel=new JPanel(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel topCenterPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.add(topCenterPanel, BorderLayout.CENTER);
        addButton(topCenterPanel, "\u2190", this::moveLeft);
        addButton(topCenterPanel, "\u2192", this::moveRight);
        addButton(topCenterPanel, "\u2191", this::moveUp);
        addButton(topCenterPanel, "\u2193", this::moveDown);
        addButton(topCenterPanel, "+", this::zoomIn);
        addButton(topCenterPanel, "-", this::zoomOut);
        addButton(topCenterPanel, "1:1", this::zoom100);
        addButton(topCenterPanel, "\u26f6", this::zoomFit);

        JPanel topRightPanel=new JPanel(new FlowLayout());
        topPanel.add(topRightPanel, BorderLayout.EAST);
        topRightPanel.add(new JLabel("X:"));
        xx=new JTextField();
        xx.setColumns(8);
        xx.setEditable(false);
        xx.setHorizontalAlignment(JTextField.RIGHT);
        topRightPanel.add(xx);
        topRightPanel.add(new JLabel("Y:"));
        yy=new JTextField();
        yy.setColumns(8);
        yy.setEditable(false);
        yy.setHorizontalAlignment(JTextField.RIGHT);
        topRightPanel.add(yy);

        MouseListenerImpl mouseListener=new MouseListenerImpl();
        view.addMouseListener(mouseListener);
        view.addMouseMotionListener(mouseListener);
        view.addMouseWheelListener(mouseListener);
        panel.add(view, BorderLayout.CENTER);
    }

    private static void addButton(JPanel panel, String text, ActionListener listener) {
        JButton button=new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
    }

    JComponent component() {
        return panel;
    }

    private void imageSize(List<BufferedImage> images, Sizes sizes) {
        sizes.imageWidth=1;
        sizes.imageHeight=1;
        for (BufferedImage image: images) {
            sizes.imageWidth=Math.max(sizes.imageWidth, image.getWidth());
            sizes.imageHeight=Math.max(sizes.imageHeight, image.getHeight());
        }
    }

    void invalidate() {
        view.repaint();
    }

    void modelChanged() {
        zoomFit(null);
    }

    private void move(int dx, int dy) {
        translateX+=50.0*dx;
        translateY+=50.0*dy;
        invalidate();
    }

    private void moveDown(ActionEvent event) {
        move(0, 1);
    }

    private void moveLeft(ActionEvent event) {
        move(-1, 0);
    }

    private void moveRight(ActionEvent event) {
        move(1, 0);
    }

    private void moveUp(ActionEvent event) {
        move(0, -1);
    }

    private Sizes sizes(Sizes sizes) {
        if (null==sizes) {
            sizes=new Sizes();
        }
        sizes.translateX=translateX;
        sizes.translateY=translateY;
        sizes.zoom=zoom;
        sizes.viewWidth=view.getWidth();
        sizes.viewHeight=view.getHeight();
        sizes.images=gui.outputPanels.size();
        sizes.columns=1;
        while (sizes.columns*sizes.columns<sizes.images) {
            ++sizes.columns;
        }
        sizes.rows=sizes.images/sizes.columns;
        if (sizes.rows*sizes.columns<sizes.images) {
            ++sizes.rows;
        }
        sizes.cellWidth=sizes.viewWidth/sizes.columns;
        sizes.cellHeight=sizes.viewHeight/sizes.rows;
        sizes.imageWidth=1;
        sizes.imageHeight=1;
        for (OutputPanel outputPanel: gui.outputPanels) {
            if (null!=outputPanel.images) {
                for (BufferedImage image: outputPanel.images) {
                    sizes.imageWidth=Math.max(sizes.imageWidth, image.getWidth());
                    sizes.imageHeight=Math.max(sizes.imageHeight, image.getHeight());
                }
            }
        }
        return sizes;
    }

    private void zoom(int clicks) {
        zoom(zoom*Math.pow(ZOOM, clicks));
    }

    private void zoom(double zoom) {
        this.zoom=Math.max(Math.pow(ZOOM, -20), Math.min(Math.pow(ZOOM, 16), zoom));
        invalidate();
    }

    private void zoom100(ActionEvent event) {
        zoom(1.0);
    }

    private void zoomFit(ActionEvent event) {
        Sizes sizes=sizes(null);
        translateX=0.0;
        translateY=0.0;
        zoom(Math.min(1.0*sizes.cellWidth/sizes.imageWidth, 1.0*sizes.cellHeight/sizes.imageHeight));
    }

    private void zoomIn(ActionEvent event) {
        zoom(1);
    }

    private void zoomOut(ActionEvent event) {
        zoom(-1);
    }
}
