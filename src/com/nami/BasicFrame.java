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

public class BasicFrame extends JFrame implements Runnable {

    //Constants
    private final String VERSION = "v1.0.0";
    private final char[] CHARS = {'@', '&', '#', '*', '!', '=', ';', ':', '~', '-', ',', '.', ' '};
    private final FontRenderContext FRC = new FontRenderContext(new AffineTransform(), false, false);
    private final int FONT_SIZE = 11;

    //GUI
    private JPanel panel;

    private JButton imagePathSelectButton;
    private JTextField imagePathTextField;

    private JButton destinationPathSelectButton;
    private JTextField destinationPathTextField;

    private JRadioButton widthScalingRadioButton;
    private JRadioButton heightScalingRadioButton;
    private JRadioButton noneScalingRadioButton;

    private JComboBox<String> fontNameComboBox;

    private JProgressBar runProgressBar;
    private JButton runButton;

    public BasicFrame() {
        //Setting up frame
        setTitle("Image-To-Chars " + VERSION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(panel);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        //Image file chooser
        JFileChooser imageFileChooser = new JFileChooser();
        imageFileChooser.setDialogTitle("Select Image");
        imageFileChooser.setMultiSelectionEnabled(false);
        imageFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        imageFileChooser.setAcceptAllFileFilterUsed(false);
        String[] exts = {"jpg", "jpeg", "jfif", "pjpeg", "pjp", "png", "bmp", "ico", "cur"};
        imageFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Image " + Arrays.toString(exts), exts));

        //Folder file chooser
        JFileChooser destinationFileChooser = new JFileChooser();
        destinationFileChooser.setDialogTitle("Select Destination Folder");
        destinationFileChooser.setMultiSelectionEnabled(false);
        destinationFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //Adding monospace fonts to comboBox
        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String fn : fontNames) {
            Font font = new Font(fn, Font.PLAIN, FONT_SIZE);
            Rectangle2D boundsI = font.getStringBounds("i", FRC);
            Rectangle2D boundsM = font.getStringBounds("m", FRC);
            if (boundsI.getWidth() == boundsM.getWidth())
                fontNameComboBox.addItem(fn);
        }
        fontNameComboBox.setSelectedItem("Consolas");

        //When selecting image
        imagePathSelectButton.addActionListener(e -> {
            int rep = imageFileChooser.showOpenDialog(null);
            if (rep != JFileChooser.APPROVE_OPTION)
                return;

            File file = imageFileChooser.getSelectedFile();
            imagePathTextField.setText(file.getAbsolutePath());

            if (destinationPathTextField.getText().trim().isEmpty())
                destinationPathTextField.setText(file.getParent());
        });

        //When selecting Folder
        destinationPathSelectButton.addActionListener(e -> {
            int rep = destinationFileChooser.showOpenDialog(null);
            if (rep != JFileChooser.APPROVE_OPTION)
                return;

            destinationPathTextField.setText(destinationFileChooser.getSelectedFile().getAbsolutePath());
        });

        //When selecting 'wScale'
        widthScalingRadioButton.addActionListener(e -> fontNameComboBox.setEnabled(true));

        //When selecting 'hScale'
        heightScalingRadioButton.addActionListener(e -> fontNameComboBox.setEnabled(true));

        //When selecting 'no scale'
        noneScalingRadioButton.addActionListener(e -> fontNameComboBox.setEnabled(false));

        //When running program
        runButton.addActionListener(e -> startCalc());

        setVisible(true);
    }

    private void startCalc() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        //Check if an image was selected
        File imgFile = new File(imagePathTextField.getText());
        if (!imgFile.exists()) {
            errorWindow("Select image file");
            return;
        }

        //Check if a destination was selected
        File destinationFile = new File(destinationPathTextField.getText());
        if (!destinationFile.exists()) {
            errorWindow("Choose destination path");
            return;
        }

        long startTime = System.nanoTime();
        runProgressBar.setValue(0);

        //Loading Image
        BufferedImage rawImg;
        try {
            rawImg = ImageIO.read(imgFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            errorWindow("Could not read image file", ex);
            return;
        }

        //Calculating scale if needed
        BufferedImage img;
        if (!noneScalingRadioButton.isSelected()) {
            Font font = new Font((String) fontNameComboBox.getSelectedItem(), Font.PLAIN, FONT_SIZE);
            Rectangle2D bounds = font.getStringBounds("@", FRC);
            double scale = ((bounds.getHeight() / bounds.getWidth()) * (widthScalingRadioButton.isSelected() ? 1 : 0)) + ((bounds.getWidth() / bounds.getHeight()) * (heightScalingRadioButton.isSelected() ? 1 : 0));

            //Scaling the image
            img = new BufferedImage((int) (rawImg.getWidth() * (widthScalingRadioButton.isSelected() ? scale : 1)), (int) (rawImg.getHeight() * (heightScalingRadioButton.isSelected() ? scale : 1)),
                    BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = AffineTransform.getScaleInstance(widthScalingRadioButton.isSelected() ? scale : 1, heightScalingRadioButton.isSelected() ? scale : 1);
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
                double index = brightness / 255 * (CHARS.length - 1);
                sb.append(CHARS[(int) Math.round(index)]);

                double percent = (double) (x + y * imgWidth) / pixelSum * 100d;
                runProgressBar.setValue((int) Math.round(percent));
            }
            sb.append("\n");
        }

        // Creating new TXT file
        File newFile = new File(destinationFile.getAbsolutePath().concat("/").concat(imgFile.getName()).concat(".txt"));
        if (!newFile.exists()) {
            try {
                boolean rep = newFile.createNewFile();
                if (!rep) {
                    errorWindow("Could not create new file");
                    return;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                errorWindow("Could not create new file", ex);
                return;
            }
        }

        //Appending processing-specifications to StringBuilder
        sb.append("\n<INFO>\n");
        sb.append(String.format("version: %s\n", VERSION));
        sb.append("author: Memphis\n");
        sb.append("instagram: @memphis_pc\n");
        sb.append("discord: Memphis#3543\n");
        sb.append("github: https://github.com/iNxmi\n");

        sb.append("\n<SETTINGS>\n");
        sb.append(String.format("filePathTextField: %s\n", imagePathTextField.getText()));
        sb.append(String.format("destinationPathTextField: %s\n", destinationPathTextField.getText()));
        sb.append(String.format("fontNameComboBox: %s\n", fontNameComboBox.getSelectedItem()));
        sb.append(String.format("widthScalingRadioButton: %s\n", widthScalingRadioButton.isSelected()));
        sb.append(String.format("heightScalingRadioButton: %s\n", heightScalingRadioButton.isSelected()));
        sb.append(String.format("noneScalingRadioButton: %s\n", noneScalingRadioButton.isSelected()));

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
            errorWindow("Could not save file", ex);
            return;
        }

        JOptionPane.showMessageDialog(null, String.format("%s\nDone! %sms", newFile.getAbsolutePath(), (System.nanoTime() - startTime) / 1000000d));
    }

    private void errorWindow(String msg, Exception ex) {
        JOptionPane.showMessageDialog(null, String.format("Error: %s\n%s", msg, ex.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void errorWindow(String msg) {
        JOptionPane.showMessageDialog(null, String.format("Error: %s", msg), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            new BasicFrame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

