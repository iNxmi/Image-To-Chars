package com.nami;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TabbedFrame extends JFrame implements Runnable {
    //Window
    private JPanel panel;

    //File Tab
    private JButton fileImagePathSelectButton;
    private JTextField fileImagePathTextField;
    private JButton fileDestinationPathSelectButton;
    private JTextField fileDestinationPathTextField;

    //Scaling Tab
    private JRadioButton scalingWidthRadioButton;
    private JRadioButton scalingHeightRadioButton;
    private JRadioButton scalingNoneRadioButton;
    private JComboBox<String> scalingFontsComboBox;

    //Charset
    private JComboBox<String> charsetComboBox;

    //Processing Tab
    private JProgressBar processingProgressBar;
    private JButton processingRunButton;

    //Utilities
    private HashMap<String, char[]> charsets;
    private JFileChooser imageFileChooser, destinationFileChooser;

    public TabbedFrame() {
        initFrame();
        initUtilities();

        initFileTab();
        initScalingTab();
        initCharsetTab();
        initProcessingTab();

        setVisible(true);
    }

    private void initFrame() {
        setTitle("Image-To-Chars");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    private void initUtilities() {
        //Converting ASCII grey scale
        charsets = new HashMap<>();
        charsets.put("symbols", toArray("@&#*!=;:~-,. "));
        charsets.put("all", toArray("$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. "));

        //Image file chooser
        imageFileChooser = new JFileChooser();
        imageFileChooser.setDialogTitle("Select Image");
        imageFileChooser.setMultiSelectionEnabled(false);
        imageFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        imageFileChooser.setAcceptAllFileFilterUsed(false);
        String[] extensions = {"jpg", "jpeg", "jfif", "pjpeg", "pjp", "png", "bmp", "ico", "cur"};
        imageFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Image " + Arrays.toString(extensions), extensions));

        //Folder file chooser
        destinationFileChooser = new JFileChooser();
        destinationFileChooser.setDialogTitle("Select Destination Folder");
        destinationFileChooser.setMultiSelectionEnabled(false);
        destinationFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    private void initFileTab() {
        fileImagePathSelectButton.addActionListener(e -> {
            int rep = imageFileChooser.showOpenDialog(null);
            if (rep != JFileChooser.APPROVE_OPTION)
                return;

            File file = imageFileChooser.getSelectedFile();
            fileImagePathTextField.setText(file.getAbsolutePath());

            if (fileDestinationPathTextField.getText().trim().isEmpty())
                fileDestinationPathTextField.setText(file.getParent());
        });

        fileDestinationPathSelectButton.addActionListener(e -> {
            int rep = destinationFileChooser.showOpenDialog(null);
            if (rep != JFileChooser.APPROVE_OPTION)
                return;

            fileDestinationPathTextField.setText(destinationFileChooser.getSelectedFile().getAbsolutePath());
        });
    }

    private void initScalingTab() {
        scalingWidthRadioButton.addActionListener(e -> scalingFontsComboBox.setEnabled(true));
        scalingHeightRadioButton.addActionListener(e -> scalingFontsComboBox.setEnabled(true));
        scalingNoneRadioButton.addActionListener(e -> scalingFontsComboBox.setEnabled(false));

        final int FONT_SIZE = 16;
        final FontRenderContext FRC = new FontRenderContext(new AffineTransform(), false, false);

        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String fn : fontNames) {
            Font font = new Font(fn, Font.PLAIN, FONT_SIZE);
            Rectangle2D boundsI = font.getStringBounds("i", FRC);
            Rectangle2D boundsM = font.getStringBounds("m", FRC);
            if (boundsI.getWidth() == boundsM.getWidth())
                scalingFontsComboBox.addItem(fn);
        }
        scalingFontsComboBox.setSelectedItem("Consolas");
    }

    private void initCharsetTab() {
        charsets.forEach((k, v) -> charsetComboBox.addItem(k));
        charsetComboBox.setSelectedItem("symbols");
    }

    private void initProcessingTab() {
        processingProgressBar.setString("");
        processingProgressBar.setStringPainted(true);

        processingRunButton.addActionListener(e -> new Thread(this).start());
    }

    private char[] toArray(String str) {
        char[] chars = new char[str.length()];
        str.getChars(0, str.length(), chars, 0);
        return chars;
    }

    @Override
    public void run() {
        //Check if image is selected
        File imageFile = new File(fileImagePathTextField.getText());
        if (!imageFile.exists()) {
            errWindow("Select image file");
            return;
        }

        //Check if destination is selected
        File destinationFile = new File(fileDestinationPathTextField.getText());
        if (!destinationFile.exists()) {
            errWindow("Select destination folder");
            return;
        }

        long startTime = System.nanoTime();
        processingProgressBar.setValue(0);

        //Loading Image
        BufferedImage rawImg;
        try {
            rawImg = ImageIO.read(imageFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            errWindow("Could not read image file", ex);
            return;
        }

        //Calculating scale if needed
        BufferedImage img;
        if (!scalingNoneRadioButton.isSelected()) {
            Font font = new Font((String) scalingFontsComboBox.getSelectedItem(), Font.PLAIN, 100);
            Rectangle2D bounds = font.getStringBounds("@", new FontRenderContext(new AffineTransform(), false, false));
            double scale = ((bounds.getHeight() / bounds.getWidth()) * (scalingWidthRadioButton.isSelected() ? 1 : 0)) + ((bounds.getWidth() / bounds.getHeight()) * (scalingHeightRadioButton.isSelected() ? 1 : 0));

            //Scaling the image
            img = new BufferedImage((int) (rawImg.getWidth() * (scalingWidthRadioButton.isSelected() ? scale : 1)), (int) (rawImg.getHeight() * (scalingHeightRadioButton.isSelected() ? scale : 1)),
                    BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = AffineTransform.getScaleInstance(scalingWidthRadioButton.isSelected() ? scale : 1, scalingHeightRadioButton.isSelected() ? scale : 1);
            AffineTransformOp ato = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
            img = ato.filter(rawImg, img);
        } else {
            img = rawImg;
        }

        // Transforming RGB to Brightness and appending a corresponding Character
        StringBuilder sb = new StringBuilder();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        double pixelSum = imgWidth * imgHeight;
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                Color color = new Color(img.getRGB(x, y));
                double brightness = ((0.21d * color.getRed()) + (0.72d * color.getGreen())
                        + (0.07d * color.getBlue()));
                char[] charset = charsets.get((String) charsetComboBox.getSelectedItem());
                double index = brightness / 255 * (charset.length - 1);
                char chr = charset[(int) Math.round(index)];
                sb.append(chr);

                double percent = (double) (x + y * imgWidth) / pixelSum * 100d;
                processingProgressBar.setString(String.format("x:%s y:%s char:%s", x, y, chr));
                processingProgressBar.setValue((int) Math.round(percent));
            }
            sb.append("\n");
        }

        // Creating new TXT file
        File newFile = new File(destinationFile.getAbsolutePath().concat("/").concat(imageFile.getName()).concat(".txt"));
        if (!newFile.exists()) {
            try {
                boolean rep = newFile.createNewFile();
                if (!rep) {
                    errWindow("Could not create new file");
                    return;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                errWindow("Could not create new file", ex);
                return;
            }
        }

        //Appending processing-specifications to StringBuilder
        sb.append("\n<INFO>\n");
        sb.append("author: Memphis\n");
        sb.append("instagram: @memphis_pc\n");
        sb.append("discord: Memphis#3543\n");
        sb.append("github: https://github.com/iNxmi\n");

        sb.append("\n<SETTINGS>\n");
        sb.append(String.format("filePathTextField: %s\n", fileImagePathTextField.getText()));
        sb.append(String.format("destinationPathTextField: %s\n", fileDestinationPathTextField.getText()));
        sb.append(String.format("fontNameComboBox: %s\n", scalingFontsComboBox.getSelectedItem()));
        sb.append(String.format("widthScalingRadioButton: %s\n", scalingWidthRadioButton.isSelected()));
        sb.append(String.format("heightScalingRadioButton: %s\n", scalingHeightRadioButton.isSelected()));
        sb.append(String.format("noneScalingRadioButton: %s\n", scalingNoneRadioButton.isSelected()));

        sb.append("\n<PROCESSING-SPECS>\n");
        sb.append(String.format("rawImgWidth: %spx\n", rawImg.getWidth()));
        sb.append(String.format("rawImgHeight: %spx\n", rawImg.getHeight()));
        sb.append(String.format("scaledImgWidth: %spx\n", img.getWidth()));
        sb.append(String.format("scaledImgHeight: %spx\n", img.getHeight()));
        sb.append(String.format("processingTime: %sms", (System.nanoTime() - startTime) / 1000000d));

        // Writing to TXT file
        try (FileWriter fw = new FileWriter(newFile)) {
            fw.write(sb.toString());
            fw.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
            errWindow("Could not save file", ex);
            return;
        }

        JOptionPane.showMessageDialog(null, String.format("%s\nDone! %sms", newFile.getAbsolutePath(), (System.nanoTime() - startTime) / 1000000d));
    }

    private void errWindow(String msg) {
        JOptionPane.showMessageDialog(null, String.format("Error: %s", msg), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void errWindow(String msg, Exception ex) {
        errWindow(String.format("%s\n%s", msg, ex.getMessage()));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            new TabbedFrame();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
