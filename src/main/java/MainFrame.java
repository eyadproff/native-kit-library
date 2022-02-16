import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;

public class MainFrame extends JFrame {

    static class DeviceType {
        public static final String CANON_DEVICE = "canon";
        public static final String CROSSMATCH_DEVICE = "crossmatch";
    }

    /********************************************
     *      UI
     *******************/
    JPanel buttonsPanel;
    JFrame frame;
    JPanel liveViewPanel;
    JPanel statusOutputPanel;
    /********************************************/
    JButton buttonStartSignalRConnection;
    JButton buttonStopSignalRConnection;

    JButton buttonStartCaptureInitCamera;
    JButton buttonStopCaptureShutdownCamera;

    JButton buttonCaptureImage;
    JButton buttonClose;
    JButton buttonUpdateURL;
    JButton buttonResetForm;
    /********************************************/
    ImageIcon captureImageIcon = null;
    ImageIcon liveViewImageIcon = null;
    static JLabel captureImageLabel = null;
    static JLabel liveViewImageLabel = null;

    /********************************************/
    static JTextArea outputArea = null;
    static JFrame mainFrameRef = null;


    public static String HUB_METHOD_STREAM_CAMERA = "canonStream";
    public static String HUB_METHOD_IMAGE_CAMERA = "canonImage";
    public static String HUB_METHOD_NK_STATUS = "nkStatus";
    public static String HUB_METHOD_STREAM_CROSSMATCH = "lscanStream";
    public static String HUB_METHOD_IMAGE_CROSSMATCH = "lscanImage";


    /**
     * Create method with two parameters
     */
    /********************************************
     * Settings
     *******************/
    static HubConnection hubConnection = null;

    static String lastConnectionState = "";
    static String SIGNALR_HUB_URL = "http://localhost:5000/cameraOperationHub";
    static String SIGNALR_HUB_URL_CANON = "cameraOperationHub";
    static String SIGNALR_HUB_URL_CROSSMATCH = "crossmatchOperationHub";


    public static void updateHUBURL() {
        try {
            SIGNALR_HUB_URL = JOptionPane.showInputDialog(mainFrameRef, "Enter HUB URL", SIGNALR_HUB_URL);

            stopConnection();

            hubConnection = HubConnectionBuilder.create(SIGNALR_HUB_URL).build();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameRef, e.getMessage());
        }
    }

    public static String getCurrentDevice() {
        String result = "";

        if (SIGNALR_HUB_URL != null) {
            if (SIGNALR_HUB_URL.endsWith(SIGNALR_HUB_URL_CANON)) {
                result = DeviceType.CANON_DEVICE;
            } else if (SIGNALR_HUB_URL.endsWith(SIGNALR_HUB_URL_CROSSMATCH)) {
                result = DeviceType.CROSSMATCH_DEVICE;
            }
        }

        return result;
    }

    public MainFrame() {
        try {
            mainFrameRef = this;

            // create JButton array list
            SwingUtilities.invokeAndWait(() -> {
                initGUI();
                updateHUBURL();
            });
            Timer timer = new Timer(50, (event) -> {
                if (hubConnection != null) {
                    if ("".equalsIgnoreCase(lastConnectionState)) {
                        lastConnectionState = hubConnection.getConnectionState().toString();
                    }
                    String currentState = hubConnection.getConnectionState().toString();
                    if (!lastConnectionState.isEmpty() && !currentState.equalsIgnoreCase(lastConnectionState)) {
                        lastConnectionState = currentState;
                        // handle connection status for color logic
                        if (HubConnectionState.CONNECTED.toString().equals(currentState)) {
                            buttonsPanel.setBackground(Color.GREEN);
                            buttonStartSignalRConnection.setEnabled(false);
                            buttonStopSignalRConnection.setEnabled(true);
                        } else {
                            buttonsPanel.setBackground(Color.ORANGE);
                            buttonStartSignalRConnection.setEnabled(true);
                            buttonStopSignalRConnection.setEnabled(false);
                        }
                        JOptionPane.showMessageDialog(mainFrameRef, "Connection state ::" + hubConnection.getConnectionState().toString() + "::");
                    }
                }
            });
            timer.start();


        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrameRef, e.getMessage());
            e.printStackTrace();
        }
    }

    public void initGUI() {
        frame = new JFrame();
        frame.setSize(1600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().setLayout(null);
        frame.setVisible(true);
        frame.getContentPane().setLayout(new BorderLayout());

        buttonsPanel = new JPanel(new FlowLayout());

        liveViewPanel = new JPanel();
        liveViewPanel.setBackground(Color.lightGray);

        captureImageIcon = new ImageIcon("");
        liveViewImageIcon = new ImageIcon("");
        captureImageLabel = new JLabel("", captureImageIcon, JLabel.CENTER);
        liveViewImageLabel = new JLabel("", liveViewImageIcon, JLabel.CENTER);

        statusOutputPanel = new JPanel(new BorderLayout());

        JPanel displayImagesPanel = new JPanel(new BorderLayout());
        displayImagesPanel.add(liveViewImageLabel, BorderLayout.EAST);
        displayImagesPanel.add(captureImageLabel, BorderLayout.WEST);
        liveViewPanel.add(displayImagesPanel);


        buttonStartSignalRConnection = new JButton("1- startSignalRConnection");
        buttonStartSignalRConnection.addActionListener((event) -> {
            SwingUtilities.invokeLater(() -> {
                startConnection();
            });
        });
        buttonStopSignalRConnection = new JButton("stopSignalRConnection");
        buttonStopSignalRConnection.addActionListener((event) -> {
            SwingUtilities.invokeLater(() -> {
               stopConnection();
            });
        });

        buttonStartCaptureInitCamera = new JButton("(startCapture) Start Livestream");
        buttonStartCaptureInitCamera.addActionListener(e -> {
            System.out.println(">>>>>>>>>>>>>>>>>> in startCaptureInitCamera... ");
            SwingUtilities.invokeLater(() -> {

                registerCaptureImageEventListener();
                registerLiveViewEventListener();

                if (getCurrentDevice().equals(DeviceType.CANON_DEVICE)) {
                    hubConnection.invoke("startCapturing", new CapturingInfo("Canon", "", false));
                } else if (getCurrentDevice().equals(DeviceType.CROSSMATCH_DEVICE)) {
                    hubConnection.invoke("startCapturing", new CapturingInfo("LScan", "LeftIndex", true));
                }

            });
        });

        buttonStopCaptureShutdownCamera = new JButton("(stopCapture) Stop Livestream");
        buttonStopCaptureShutdownCamera.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                // for Canon only
                hubConnection.send("stopCapturing");
            });
        });

        buttonResetForm = new JButton("Reset Form");
        buttonResetForm.setBackground(Color.pink);
        buttonResetForm.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {

                if (!HubConnectionState.CONNECTED.toString().equals(lastConnectionState) && !HubConnectionState.DISCONNECTED.toString().equals(lastConnectionState)) {
                    buttonsPanel.setBackground(null);
                }
                outputArea.setText("Ready..");

                liveViewImageLabel.setIcon(null);
                captureImageLabel.setIcon(null);

                buttonStartSignalRConnection.setEnabled(true);
                buttonStopSignalRConnection.setEnabled(true);
            });
        });

        buttonCaptureImage = new JButton("Capture (Manual)");
        buttonCaptureImage.addActionListener((event) -> {
            SwingUtilities.invokeLater(() -> {
                if (getCurrentDevice().equals(DeviceType.CANON_DEVICE)) {
                    hubConnection.send("capture");
                } else {
                    JOptionPane.showMessageDialog(mainFrameRef, "Capture method not supported for " + getCurrentDevice());
                }
            });
        });

        buttonClose = new JButton("Close");
        buttonClose.setBackground(Color.pink);
        buttonClose.addActionListener((event) -> {
            if (JOptionPane.showConfirmDialog(frame, "Exit?") == JOptionPane.OK_OPTION) {
                System.exit(0);
            }
        });

        buttonUpdateURL = new JButton("Update URL");
        buttonUpdateURL.addActionListener((event) -> {
            SwingUtilities.invokeLater(() -> {
                updateHUBURL();
            });
        });

        outputArea = new JTextArea();
        outputArea.setText("Ready..");
//        JScrollPane scrollTextArea = new JScrollPane (outputArea,
//                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        statusOutputPanel.add(outputArea, BorderLayout.CENTER);


        buttonsPanel.add(buttonStartSignalRConnection);
        buttonsPanel.add(buttonStopSignalRConnection);
        buttonsPanel.add(buttonStartCaptureInitCamera);
        buttonsPanel.add(buttonStopCaptureShutdownCamera);
        buttonsPanel.add(buttonCaptureImage);
        buttonsPanel.add(buttonUpdateURL);
        buttonsPanel.add(buttonResetForm);
        buttonsPanel.add(buttonClose);

        frame.getContentPane().add(statusOutputPanel, BorderLayout.NORTH);
        frame.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(liveViewPanel, BorderLayout.CENTER);
    }

    /************************************************************/
    public static void startConnection() {
        System.out.println(">>>>>>> hubConnection.start");
        try {

            hubConnection = HubConnectionBuilder.create(SIGNALR_HUB_URL).build();

            hubConnection.start();

            hubConnection.on(HUB_METHOD_NK_STATUS, (sentMsg) -> {
                try {
                    System.out.println(">>>> Recevied message from hub on::" + LocalDateTime.now() + ":::connectionId::" + hubConnection.getConnectionId() + ":");
                    System.out.println(">>>>>>>> Message received::" + sentMsg);

                    SwingUtilities.invokeLater(() -> {
                        addTextToStatusArea(" [nkStatusMsg] :: " + sentMsg);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, String.class);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage() != null ? e.getMessage() : "Error");
        }
    }

    public static void stopConnection() {
        System.out.println(">>>>>>>>>>>>> in stopConnection");
        try {
            if (hubConnection != null) {
                hubConnection.stop().doOnError((throwable) -> {
                    throwable.printStackTrace();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage() != null ? e.getMessage() : "Error");
        }
        try {
            if (hubConnection != null) {
                hubConnection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage() != null ? e.getMessage() : "Error");
        }
        System.out.println(">>>>>>>>>>>>> after stopConnection");
    }

    /************************************************************/

    public static void registerCaptureImageEventListener() {
        if (hubConnection != null) {
            String targetStream = null;
            if (getCurrentDevice().equals(DeviceType.CANON_DEVICE)) {
                targetStream = HUB_METHOD_IMAGE_CAMERA;
            } else if (getCurrentDevice().equals(DeviceType.CROSSMATCH_DEVICE)) {
                targetStream = HUB_METHOD_IMAGE_CROSSMATCH;
            }

            if (targetStream != null) {
                hubConnection.on(targetStream, (sentCapImage) -> {
                    try {
                        SwingUtilities.invokeLater(() -> {
                            InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(sentCapImage.getBase64String()));
                            BufferedImage image = null;
                            try {
                                image = ImageIO.read(in);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // Save file...
                            //Files.write(Paths.get(new URI("file:/d:/deleteme/" + System.currentTimeMillis() + ".jpg")), Base64.getDecoder().decode(sentCapImage.getBase64String()));

                            ImageIcon scaledImageIcon = new ImageIcon(new ImageIcon(image).getImage().getScaledInstance(648, 432, Image.SCALE_SMOOTH));
                            captureImageLabel.setIcon(scaledImageIcon);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, CapturedImage.class);
            }
        }
    }

    public static void registerLiveViewEventListener() {
        if (hubConnection != null) {

            String targetStream = null;
            if (getCurrentDevice().equals(DeviceType.CANON_DEVICE)) {
                targetStream = HUB_METHOD_STREAM_CAMERA;
            } else if (getCurrentDevice().equals(DeviceType.CROSSMATCH_DEVICE)) {
                targetStream = HUB_METHOD_STREAM_CROSSMATCH;
            }

            if (targetStream != null) {
                hubConnection.on(targetStream, (sentLiveFrameImage) -> {
                    try {
                        SwingUtilities.invokeLater(() -> {
                            Thread parseImageThread = new Thread(() -> {
                                InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(sentLiveFrameImage.getBase64String()));
                                BufferedImage imageFromBase64 = null;
                                try {
                                    imageFromBase64 = ImageIO.read(in);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // Save file...
                                //Files.write(Paths.get(new URI("file:/d:/deleteme/" + System.currentTimeMillis() + ".jpg")), Base64.getDecoder().decode(sentCapImage.getBase64String()));

                                ImageIcon scaledImageIcon = new ImageIcon(new ImageIcon(imageFromBase64).getImage().getScaledInstance(648, 432, Image.SCALE_SMOOTH));

                                liveViewImageLabel.setIcon(scaledImageIcon);
                            });
                            // temp workaround of lagging base64 decoding.
                            parseImageThread.setPriority(Thread.MAX_PRIORITY);
                            parseImageThread.start();
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, CapturedImage.class);
            }

        }
    }

    public static void addTextToStatusArea(String msg) {
        final String thisMsg = "[" + new java.util.Date().toString() + "] :: "  + msg;
        System.out.println(thisMsg);

        SwingUtilities.invokeLater(() -> {
            outputArea.setText(null);
            outputArea.append(thisMsg);
        });
    }
    public static void main(String[] args) {
        new MainFrame();
    }

}
