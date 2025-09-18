import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import Sliders.ThresholdSliderUI;
import Sliders.RangeSlider;
import Sliders.RangeSliderUI;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DV extends JFrame
{
    // error logging
    private final static Logger LOGGER = Logger.getLogger(Analytics.class.getName());

    /**************************************************
     * FOR GUI
     *************************************************/
    // sliders
    static RangeSlider domainSlider;
    static RangeSlider overlapSlider;
    static JSlider thresholdSlider;
    // panels
    static JPanel mainPanel;
    static JPanel angleSliderPanel;
    static JPanel confusionMatrixPanel;
    static JPanel crossValidationPanel;
    static JPanel analyticsPanel;
    static JPanel graphPanel;
    static JPanel sliderPanel;
    static JPanel controlPanel;

    // panels for remote windows
    static JPanel remoteGraphPanel;
    static JPanel remoteAnalyticsPanel;
    static JPanel remoteConfusionMatrixPanel;
    static JPanel remoteCrossValidationPanel;
    static boolean displayRemoteGraphs;
    static boolean displayRemoteAnalytics;

    // scroll areas
    JScrollPane anglesPane;
    JScrollPane analyticsPane;

    // main frame for DV
    static JFrame mainFrame;

    // minimum frame size
    static final int[] minSize = new int[]{ 1280, 720 };

    /**************************************************
     * FOR GRAPHS
     *************************************************/
    // line colors
    static Color domainLines = Color.RED;
    static Color overlapLines = Color.ORANGE;
    static Color thresholdLine = Color.GREEN;
    static Color background = Color.WHITE;
    static Color svmLines = new Color(139,69,19,75);

    /**************
     * General Information
     **************/
    static String dataFileName;

    // graph colors
    static Color[] graphColors = new Color[] {
            new Color(	147, 112, 219),   // upper graph (purple)
            new Color(84, 133, 145)       // lower graph (dark cyan)
    };

    // class-specific colors for multiple classes
    static Color[] classColors = new Color[] {
            Color.GREEN,    // benign (green)
            Color.RED,      // malignant (red)
            new Color(0, 0, 255),      // blue
            new Color(255, 165, 0),    // orange
            new Color(128, 0, 128),    // purple
            new Color(255, 192, 203),  // pink
            new Color(0, 255, 255),    // cyan
            new Color(255, 255, 0),    // yellow
            new Color(165, 42, 42),    // brown
            new Color(128, 128, 128)   // gray
    };

    // endpoint colors
    static Color endpoints = Color.BLACK;
    static Color midpoints = Color.BLACK;
    static Color svmEndpoints = new Color(205, 127, 50);
    static boolean showMidpoints = false;

    // highlight color
    static Color highlightColor = Color.ORANGE;

    // hyperblock color
    static Color hyperblockColor = Color.ORANGE;

    // show bars instead of endpoints for graphs
    // the height of a bar is equal to the number of points in its location
    static boolean showBars = false;

    // draw only overlap
    static boolean drawOverlap = false;

    // draw support vectors
    static boolean haveSVM = false;
    static boolean drawSVM = false;
    static boolean drawOnlySVM = false;

    // domain active
    static boolean domainActive = true;

    // domain area
    static double[] domainArea;

    // upper class is visualized on the upper graph
    // lower classes are visualized on the lower graph
    static int upperClass = 0;
    static ArrayList<Boolean> lowerClasses = new ArrayList<>(List.of(false));

    // choose problem type
    static boolean classification = true;

    /***
     * CHANGE LATER
     */

    static ArrayList<Double> reg_predictions = new ArrayList<>();
    static ArrayList<Double> reg_true_values = new ArrayList<>();
    static double reg_largest_coef;
    static double reg_RMSE;
    static double reg_avg_val;


    /***
     * CHANGE LATER
     */

    // whether to show the first line segment or not
    static boolean showFirstSeg = true;

    // whether to highlight a point or not
    static boolean[][] highlights;

    /**************************************************
     * FOR ANALYTICS
     *************************************************/
    // overlap area
    static double[] overlapArea;

    // threshold point
    static double threshold;

    // threshold before optimizing
    static double prevThreshold;

    // true if upper class has lower mean
    static boolean upperIsLower = true;

    // current accuracy
    static double accuracy;

    // current all data confusion matrix
    static String allDataCM;

    // previous all data confusion matrices (only applicable if 3+ classes)
    static ArrayList<String> prevAllDataCM;

    // current all data correct and total
    static int[] allDataClassifications;

    // previous all data correct and total
    static ArrayList<int[]> prevAllDataClassifications;

    // display analytics
    static boolean prevAllDataChecked = true;
    static boolean allDataChecked = true;
    static boolean withoutOverlapChecked = false;
    static boolean overlapChecked = false;
    static boolean worstCaseChecked = false;
    static boolean userValidationChecked = true;
    static boolean userValidationImported = false;
    static boolean svmAnalyticsChecked = false;
    static boolean crossValidationChecked = false;
    static boolean crossValidationNotGenerated = true;

    // number of folds for k-fold cross validation
    static int kFolds = 10;

    /************************************************
     * FOR INPUT DATA
     ***********************************************/
    // input data info
    static boolean hasID;
    static boolean hasClasses;

    // min-max or zScore min-max normalization
    static boolean zScoreMinMax;

    /************************************************
     * FOR DATA
     ***********************************************/
    // angles and initial angles (store angles before optimizing)
    static double[] angles;
    static double[] prevAngles;
    static double[] standardAngles;

    // normalized and original data
    static double trainSplit = 1;
    static double testSplit = 0;
    static ArrayList<DataObject> trainData;
    static ArrayList<DataObject> testData;
    static ArrayList<ArrayList<double[]>> misclassifiedData;
    static ArrayList<DataObject> originalData;
    static DataObject supportVectors;

    // user made validation data
    static ArrayList<DataObject> validationData;

    // classes for data
    static ArrayList<String> uniqueClasses;
    static int classNumber;

    // fieldnames and length
    static ArrayList<String> fieldNames;
    static int fieldLength;
    static ArrayList<String> standardFieldNames;
    static int standardFieldLength;
    static ArrayList<Integer> originalAttributeOrder;

    // active attributes
    static ArrayList<Boolean> activeAttributes;

    /************************************************
     * FOR PROJECT
     ***********************************************/
    // all data files used in project
    static ArrayList<File> dataFiles;

    // name of project (if saved)
    static String projectSaveName;


    /**
     * Main handler for UI
     * Creates main panel, menu bar, and toolbar.
     */
    public DV()
    {
        // set DV properties
        super("DV 2.0");
        this.setSize(minSize[0], minSize[1]);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setExtendedState(this.getExtendedState() & (~JFrame.ICONIFIED));
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        // set mainFrame to DV
        mainFrame = this;

        // setup layout
        mainFrame.setLayout(new BorderLayout());

        // create menu bar
        createMenuBar();

        // create and add toolbar
        JToolBar toolBar = createToolBar();
        mainFrame.add(toolBar, BorderLayout.PAGE_START);

        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // set layouts for remote panels
        remoteGraphPanel = new JPanel();
        remoteGraphPanel.setLayout(new BoxLayout(remoteGraphPanel, BoxLayout.Y_AXIS));
        remoteGraphPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        remoteAnalyticsPanel = new JPanel();
        remoteAnalyticsPanel.setLayout(new BoxLayout(remoteAnalyticsPanel, BoxLayout.Y_AXIS));

        remoteConfusionMatrixPanel = new JPanel(new GridLayout(0, 4, 5, 5));
        remoteCrossValidationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        remoteAnalyticsPanel.add(remoteConfusionMatrixPanel);
        remoteAnalyticsPanel.add(remoteCrossValidationPanel);

        // set layout for graphPanel
        graphPanel = new JPanel();
        graphPanel.setLayout(new GridBagLayout());
        GridBagConstraints gpc = new GridBagConstraints();
        gpc.gridx = 0;
        gpc.gridy = 0;
        gpc.weightx = 1;
        gpc.weighty = 1;
        gpc.fill = GridBagConstraints.BOTH;

        // add blank graph
        graphPanel.add(ChartsAndPlots.blankGraph(), gpc);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.8;
        c.weighty = 0.7;
        c.fill = GridBagConstraints.BOTH;
        mainPanel.add(graphPanel, c);

        // set layout and size
        sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridBagLayout());
        GridBagConstraints sc = new GridBagConstraints();

        // set colors minimum and maximum of slider
        domainSlider = new RangeSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new RangeSliderUI(this, Color.RED, new Color(255, 114, 118), new Color(139, 0, 0)));
                updateLabelUIs();
            }
        };
        domainSlider.setMinimum(0);
        domainSlider.setMaximum(400);
        domainSlider.setMajorTickSpacing(1);
        domainSlider.setValue(0);
        domainSlider.setUpperValue(400);
        domainSlider.setToolTipText("Control visible range of graph");

        // add to panels
        sc.gridx = 0;
        sc.gridy = 0;
        sc.weightx = 1;
        sc.fill = GridBagConstraints.HORIZONTAL;
        sc.ipady = 8;
        sliderPanel.add(domainSlider, sc);

        // add label
        JPanel domainSliderLabel = new JPanel();
        domainSliderLabel.add(new JLabel("Setting up Subset of Utilized Data for All Classes"));
        domainSliderLabel.setToolTipText("Control visible range of graph");
        sc.gridy = 1;
        sc.ipady = 0;
        sliderPanel.add(domainSliderLabel, sc);

        // set colors and minimum and maximum of slider
        overlapSlider = new RangeSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new RangeSliderUI(this, Color.ORANGE, new Color(255, 165, 0), new Color(215,107,0)));
                updateLabelUIs();
            }
        };
        overlapSlider.setMinimum(0);
        overlapSlider.setMaximum(400);
        overlapSlider.setMajorTickSpacing(1);
        overlapSlider.setValue(0);
        overlapSlider.setUpperValue(400);
        overlapSlider.setToolTipText("Control overlap area of graph");

        // add to panel
        sc.gridy = 2;
        sc.ipady = 8;
        sliderPanel.add(overlapSlider, sc);

        // add label
        JPanel overlapSliderLabel = new JPanel();
        overlapSliderLabel.add(new JLabel("Setting up Overlap Area for All Classes"));
        overlapSliderLabel.setToolTipText("Control overlap Area of graph");
        sc.gridy = 3;
        sc.ipady = 0;
        sliderPanel.add(overlapSliderLabel, sc);

        // create threshold slider
        thresholdSlider = new JSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new ThresholdSliderUI(this));
            }
        };

        // set minimum and maximum of slider
        thresholdSlider.setMinimum(0);
        thresholdSlider.setMaximum(400);
        thresholdSlider.setMajorTickSpacing(1);
        thresholdSlider.setValue(200);
        thresholdSlider.setToolTipText("Change threshold value for visualization");

        // add to panels
        sc.gridy = 4;
        sc.ipady = 8;
        sliderPanel.add(thresholdSlider, sc);

        // add label
        JPanel thresholdSliderLabel = new JPanel();
        thresholdSliderLabel.add(new JLabel("Setting up Area of the Class Threshold"));
        thresholdSliderLabel.setToolTipText("Change threshold value for visualization");
        sc.gridy = 5;
        sc.ipady = 0;
        sliderPanel.add(thresholdSliderLabel, sc);

        // finalize domain panel
        c.gridy = 1;
        c.weighty = 0.15;
        mainPanel.add(sliderPanel, c);

        // create confusion matrix and cross validation panels
        confusionMatrixPanel = new JPanel(new GridLayout(0, 4, 5, 5));
        crossValidationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // create analytics panel
        analyticsPanel = new JPanel();
        analyticsPanel.setLayout(new BoxLayout(analyticsPanel, BoxLayout.Y_AXIS));
        analyticsPanel.add(confusionMatrixPanel);
        analyticsPanel.add(crossValidationPanel);

        // create confusion matrix pane
        analyticsPane = new JScrollPane(analyticsPanel);
        analyticsPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        c.weightx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        mainPanel.add(analyticsPane, c);

        // create angles scroll pane
        angleSliderPanel = new JPanel();
        angleSliderPanel.setLayout(new BoxLayout(angleSliderPanel, BoxLayout.PAGE_AXIS));
        anglesPane = new JScrollPane(angleSliderPanel);

        c.weightx = 0.2;
        c.weighty = 0.85;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 2;
        mainPanel.add(anglesPane, c);

        mainFrame.add(mainPanel, BorderLayout.CENTER);

        // add control panel to mainPanel
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel mouseControls = new JLabel("UI Controls: ");
        mouseControls.setFont(mouseControls.getFont().deriveFont(Font.BOLD, 12f));
        controlPanel.add(mouseControls);

        JLabel controls = new JLabel("pan = ctrl + hold left click, " +
                "zoom = scroll wheel, " +
                "unequal zoom in selected rectangle = hold left click + drag down and right, " +
                "unequal zoom out = hold left click + drag left or up");
        controls.setFont(controls.getFont().deriveFont(12f));
        controlPanel.add(controls);

        mainFrame.add(controlPanel, BorderLayout.PAGE_END);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        mainFrame.revalidate();
        mainFrame.pack();
        mainFrame.repaint();

        mainFrame.setMinimumSize(new Dimension(minSize[0], minSize[1]));
    }


    /**
     * Creates menu bar for DV Program
     * Menu bar features:
     * creating new projects, opening saved projects,
     * saving projects (old save), saving projects as (new save),
     * importing data, and informing users
     */
    private void createMenuBar()
    {
        // creates menu bar at top of screen
        JMenuBar menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);

        // file menu
        // keyboard shortcut: alt + f
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        // file menu item: create new project
        // keyboard shortcut: alt + n
        JMenuItem newProjItem = new JMenuItem("Create New Project");
        newProjItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        newProjItem.addActionListener(e -> createNewProject());
        fileMenu.add(newProjItem);

        // file menu item: open saved project
        // keyboard shortcut: alt + o
        JMenuItem openSavedItem = new JMenuItem("Open Saved Project");
        openSavedItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK));
        openSavedItem.addActionListener(e -> openSavedProject());
        fileMenu.add(openSavedItem);

        // file menu item: save project
        // keyboard shortcut: alt + s
        JMenuItem saveProjItem = new JMenuItem("Save Project");
        saveProjItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        saveProjItem.addActionListener(e -> saveProject());
        fileMenu.add(saveProjItem);

        // file menu item: save project as
        // keyboard shortcut: alt + a
        JMenuItem saveProjAsItem = new JMenuItem("Save Project As");
        saveProjAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        saveProjAsItem.addActionListener(e -> saveProjectAs());
        fileMenu.add(saveProjAsItem);

        // file menu item: import data
        // keyboard shortcut: alt + i
        JMenuItem importDataItem = new JMenuItem("Import Data");
        importDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
        importDataItem.addActionListener(e -> importData());
        fileMenu.add(importDataItem);

        // file menu item: validation data
        // keyboard shortcut: alt + v
        JMenuItem validationDataItem = new JMenuItem("Validation Data");
        validationDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK));
        validationDataItem.addActionListener(e -> createUserValidationSet());
        fileMenu.add(validationDataItem);

        // help menu
        // keyboard shortcut: alt + h
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        // help menu item: open manual
        // keyboard shortcut: alt + m
        JMenuItem manualItem = new JMenuItem("User Manual");
        manualItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        manualItem.addActionListener(e ->
        {
            // try opening DVManual
            try
            {
               Desktop.getDesktop().open(new File("documentation\\user\\DV_User_Manual.pdf"));
            }
            catch (IOException | IllegalArgumentException ioe)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error opening user manual.\n" +
                                ioe,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        helpMenu.add(manualItem);

        // help manu item: normalization info
        // keyboard shortcut: alt + z
        JMenuItem normalizationInfoItem = new JMenuItem("Normalization Info");
        normalizationInfoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK));
        normalizationInfoItem.addActionListener(e -> normalizationInfoPopup());
        helpMenu.add(normalizationInfoItem);

        // help menu item: code help menu
        // keyboard shortcut: N/A
        JMenu codeHelpMenu = new JMenu("Code");
        codeHelpMenu.setMnemonic(KeyEvent.VK_E);
        helpMenu.add(codeHelpMenu);

        // code help menu item: UML diagram
        // keyboard shortcut: alt + u
        JMenuItem umlItem = new JMenuItem("UML Diagram");
        umlItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        umlItem.addActionListener(e ->
        {
            // try opening DV_UML
            try
            {
                Desktop.getDesktop().open(new File("documentation\\code\\DV_UML.png"));
            }
            catch (IOException | IllegalArgumentException ioe)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error opening UML diagram.\n" +
                                ioe,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        codeHelpMenu.add(umlItem);

        // code help menu: UML descriptions
        // keyboard shortcut: alt + l
        JMenuItem umlDescriptionItem = new JMenuItem("UML Descriptions");
        umlDescriptionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK));
        umlDescriptionItem.addActionListener(e ->
        {
            // try opening DV_UML_Descriptions
            try
            {
                Desktop.getDesktop().open(new File("documentation\\code\\DV_UML_Descriptions.pdf"));
            }
            catch (IOException | IllegalArgumentException ioe)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error opening UML descriptions.\n" +
                                ioe,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        codeHelpMenu.add(umlDescriptionItem);
    }


    /**
     * Creates toolbar for DV Program
     */
    private JToolBar createToolBar()
    {
        // create toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        // colors options
        JButton colorOptionsBtn = new JButton("Color Options");
        colorOptionsBtn.setToolTipText("Open the color options menu");
        colorOptionsBtn.addActionListener(e -> new ColorMenu());
        toolBar.addSeparator();
        toolBar.add(colorOptionsBtn);
        toolBar.addSeparator();

        // visualization options
        JButton visOptionsBtn = new JButton("Visualization Options");
        visOptionsBtn.setToolTipText("Open the visualization options menu");
        visOptionsBtn.addActionListener(e -> new VisualizationMenu());
        toolBar.add(visOptionsBtn);
        toolBar.addSeparator();

        // confusion matrix options
        JButton analyticsBtn = new JButton("Analytic Options");
        analyticsBtn.setToolTipText("Open the analytics options menu");
        analyticsBtn.addActionListener(e -> new AnalyticsMenu());
        toolBar.add(analyticsBtn);
        toolBar.addSeparator();

        // create LDF rules for all data
        JButton ldfRuleBtn = new JButton("LDF Rule");
        ldfRuleBtn.setToolTipText("Open the LDF Rule menu");
        ldfRuleBtn.addActionListener(e -> new LDFRule());
        toolBar.add(ldfRuleBtn);
        toolBar.addSeparator();

        // create hyperblocks for all data
        JButton hpBtn = new JButton("Hyperblocks");
        hpBtn.setToolTipText("Generate hyperblocks for all data");
        hpBtn.addActionListener(e -> new HyperBlockGeneration());
        toolBar.add(hpBtn);
        toolBar.addSeparator();

        // resets screen
        JButton resetScreenBtn = new JButton("Reset Screen");
        resetScreenBtn.setToolTipText("Resets rendered zoom area");
        resetScreenBtn.addActionListener(e ->
        {
            if (trainData != null)
            {
                DataVisualization.drawGraphs();
                repaint();
                revalidate();
            }
        });
        toolBar.add(resetScreenBtn);
        toolBar.addSeparator();

        // optimize visualization
        JButton optimizeBtn = new JButton("Optimize Visualization");
        optimizeBtn.setToolTipText("Attempts to optimize angles and threshold");
        optimizeBtn.addActionListener(e -> DataVisualization.optimizeAngles(true));
        toolBar.add(optimizeBtn);
        toolBar.addSeparator();

        // undo optimization
        JButton undoOptimizeBtn = new JButton("Undo Optimization");
        undoOptimizeBtn.setToolTipText("Reverses previous optimization operation");
        undoOptimizeBtn.addActionListener(e -> DataVisualization.undoOptimization());
        toolBar.add(undoOptimizeBtn);
        toolBar.addSeparator();

        // normalize angles between [0, 90]
        JButton normAnglesBtn = new JButton("Normalize Angles");
        normAnglesBtn.setToolTipText("Normalizes the current angles between 0 and 90 degrees.");
        normAnglesBtn.addActionListener(e -> DataVisualization.normalizeAngles());
        toolBar.add(normAnglesBtn);
        toolBar.addSeparator();

        // toggle bar-line
        JButton barLineBtn = new JButton("Toggle Bar-line");
        barLineBtn.setToolTipText("Toggle for showing bar-line graph of endpoint placement");
        barLineBtn.addActionListener(e ->
        {
            if (trainData != null)
            {
                showBars = !showBars;
                if (classification)
                    DataVisualization.drawGraphs();
                else
                    DataVisualization.drawRegGraph();
            }
        });
        toolBar.add(barLineBtn);
        toolBar.addSeparator();

        // add icons
        int offset = colorOptionsBtn.getInsets().top + colorOptionsBtn.getInsets().bottom;
        colorOptionsBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\color-palette.png"), colorOptionsBtn.getHeight() - offset, colorOptionsBtn.getHeight() - offset));
        visOptionsBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\line-chart.png"), visOptionsBtn.getHeight() - offset, visOptionsBtn.getHeight() - offset));
        analyticsBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\statistics.png"), analyticsBtn.getHeight() - offset, analyticsBtn.getHeight() - offset));
        ldfRuleBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\selection.png"), ldfRuleBtn.getHeight() - offset, ldfRuleBtn.getHeight() - offset));
        hpBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\cube.png"), hpBtn.getHeight() - offset, hpBtn.getHeight() - offset));
        resetScreenBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\undo.png"), resetScreenBtn.getHeight() - offset, resetScreenBtn.getHeight() - offset));
        optimizeBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\up-right-arrow.png"), optimizeBtn.getHeight() - offset, optimizeBtn.getHeight() - offset));
        undoOptimizeBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\revert.png"), undoOptimizeBtn.getHeight() - offset, undoOptimizeBtn.getHeight() - offset));
        normAnglesBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\between.png"), normAnglesBtn.getHeight() - offset, normAnglesBtn.getHeight() - offset));
        barLineBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\bar-graph.png"), barLineBtn.getHeight() - offset, barLineBtn.getHeight() - offset));

        return toolBar;
    }


    /**
     * Resizes icon to specified width and height
     * @param img_icon Icon to be resized
     * @param width New width for img_icon
     * @param height New height for img_icon
     * @return Resized icon
     */
    private Icon resizeIcon(ImageIcon img_icon, int width, int height)
    {
        Image img = img_icon.getImage();
        Image resized = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return new ImageIcon(resized);
    }


    /**
     * Asks user questions about data then creates project
     */
    public void createNewProject()
    {
        try
        {
            // create panel for user input
            final JCheckBox regOrClass = new JCheckBox("Is this a classification problem or a regression problem? (True: Classification, False: Regression)", true);
            final JCheckBox idCol = new JCheckBox("Does this project use the first column to designate ID?");
            final JCheckBox classCol = new JCheckBox("Does this project use the last column to designate classes?", true);
            final JCheckBox zScoreNorm = new JCheckBox("z-Score Min-Max Normalization");

            JPanel checkBoxPanel = createNewProjectHelper(regOrClass, idCol, classCol, zScoreNorm);
            int choice = JOptionPane.showConfirmDialog(DV.mainFrame, checkBoxPanel, "Dataset Information", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (choice == 0)
            {
                classification = regOrClass.isSelected();
                hasID = idCol.isSelected();
                hasClasses = classCol.isSelected();
                zScoreMinMax = zScoreNorm.isSelected();
            }
            else
                return;

            // set filter on file chooser
            JFileChooser fileDialog = new JFileChooser();
            fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

            // set to current directory
            File workingDirectory = new File(System.getProperty("user.dir"));
            fileDialog.setCurrentDirectory(workingDirectory);

            // open file dialog
            int results = fileDialog.showOpenDialog(mainFrame);
            if (results == JFileChooser.APPROVE_OPTION)
            {
                File dataFile = fileDialog.getSelectedFile();

                // reset program
                // parse data from file into classes
                resetProgram(true);
                boolean success = DataSetup.setupWithData(dataFile);

                // create graphs
                if (success)
                {
                    // save original file
                    dataFiles.add(dataFile);
                    dataFileName = dataFile.getName();

                    if (classification)
                    {
                        // optimize data setup with Linear Discriminant Analysis
                        DataVisualization.optimizeSetup();
                        DataVisualization.drawGraphs();
                    }
                    else
                    {
                        // optimize data setup with Linear Regression Analysis
                        DataVisualization.optimizeRegSetup();
                        DataVisualization.drawRegGraph();
                    }
                }
                else
                {
                    warningPopup(
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file");

                    // add blank graph
                    graphPanel.add(ChartsAndPlots.blankGraph());
                }
            }
            else if (results != JFileChooser.CANCEL_OPTION)
            {
                warningPopup(
                        "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not open file");

                // add blank graph
                graphPanel.add(ChartsAndPlots.blankGraph());
            }

            // repaint and revalidate DV
            repaint();
            revalidate();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            warningPopup(
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file");

            // add blank graph if data was bad
            graphPanel.add(ChartsAndPlots.blankGraph());

            // repaint and revalidate DV
            repaint();
            revalidate();
        }
    }


    /**
     * Helper for creating new project
     * Creates user input panel
     * @return JPanel for user input
     */
    private JPanel createNewProjectHelper(JCheckBox regOrClass, JCheckBox idCol, JCheckBox classCol, JCheckBox zScoreNorm)
    {
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        checkBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel message = new JLabel("Specify the dataset format for this project.");
        message.setFont(message.getFont().deriveFont(14f));
        checkBoxPanel.add(message);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        regOrClass.setFont(regOrClass.getFont().deriveFont(12f));
        regOrClass.setAlignmentX(Component.LEFT_ALIGNMENT);

        idCol.setFont(idCol.getFont().deriveFont(12f));
        idCol.setAlignmentX(Component.LEFT_ALIGNMENT);

        classCol.setAlignmentX(Component.LEFT_ALIGNMENT);
        classCol.setFont(classCol.getFont().deriveFont(12f));
        regOrClass.addItemListener(e ->
        {
            classCol.setSelected(true);
            classCol.setEnabled(regOrClass.isSelected());
        });

        JPanel normPanel = new JPanel();
        normPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup normBtnGroup = new ButtonGroup();
        JCheckBox minMaxNorm = new JCheckBox("Min-Max Normalization", true);
        zScoreNorm.setFont(zScoreNorm.getFont().deriveFont(12f));
        minMaxNorm.setFont(minMaxNorm.getFont().deriveFont(12f));
        zScoreNorm.setAlignmentX(Component.LEFT_ALIGNMENT);
        minMaxNorm.setAlignmentX(Component.LEFT_ALIGNMENT);
        normBtnGroup.add(minMaxNorm);
        normBtnGroup.add(zScoreNorm);

        JButton normHelp = new JButton("Help");
        normHelp.setFont(normHelp.getFont().deriveFont(12f));
        normHelp.setAlignmentX(Component.LEFT_ALIGNMENT);
        normHelp.setToolTipText("Information on normalization styles");
        normHelp.addActionListener(e -> normalizationInfoPopup());

        normPanel.add(zScoreNorm);
        normPanel.add(minMaxNorm);
        normPanel.add(normHelp);

        JPanel splitPanel = new JPanel();
        splitPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel splitLabel = new JLabel("Data split ratio of training and testing data.");
        splitLabel.setFont(splitLabel.getFont().deriveFont(14f));

        // Create SpinnerNumberModels for train and test split
        SpinnerNumberModel trainModel = new SpinnerNumberModel(1.0, 0.0, 1.0, 0.1);
        SpinnerNumberModel testModel = new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1);

        // Create the JSpinners with these models
        JSpinner trainSpinner = new JSpinner(trainModel);
        JSpinner testSpinner = new JSpinner(testModel);

        trainSpinner.setPreferredSize(new Dimension(80, trainSpinner.getPreferredSize().height));
        testSpinner.setPreferredSize(new Dimension(80, testSpinner.getPreferredSize().height));

        trainSpinner.addChangeListener(e -> {
            double percent = (double) trainSpinner.getValue();
            if (0 < percent && percent <= 1) {
                DV.trainSplit = percent;
                DV.testSplit = 1 - percent;
                testSpinner.setValue(DV.testSplit); // Sync test spinner with train spinner
            }
        });

        testSpinner.addChangeListener(e -> {
            double percent = (double) testSpinner.getValue();
            if (0 <= percent && percent < 1) {
                DV.testSplit = percent;
                DV.trainSplit = 1 - percent;
                trainSpinner.setValue(DV.trainSplit); // Sync train spinner with test spinner
            }
        });

        // Panels for train and test spinners
        JPanel training = new JPanel();
        JLabel trainSplit = new JLabel("Train Split");
        training.add(trainSplit);
        training.add(trainSpinner);

        JPanel testing = new JPanel();
        JLabel testSplit = new JLabel("Test Split");
        testing.add(testSplit);
        testing.add(testSpinner);

        splitPanel.add(splitLabel);
        splitPanel.add(training);
        splitPanel.add(testing);

        checkBoxPanel.add(regOrClass);
        checkBoxPanel.add(idCol);
        checkBoxPanel.add(classCol);
        checkBoxPanel.add(normPanel);
        checkBoxPanel.add(splitLabel);
        checkBoxPanel.add(splitPanel);

        return checkBoxPanel;
    }


    /**
     * Imports new data into current project
     */
    private void importData()
    {
        try
        {
            if (!trainData.isEmpty())
            {
                // set filter on file chooser
                JFileChooser fileDialog = new JFileChooser();
                fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileDialog.setCurrentDirectory(workingDirectory);

                // open file dialog
                int results = fileDialog.showOpenDialog(mainFrame);

                if (results == JFileChooser.APPROVE_OPTION)
                {
                    File importFile = fileDialog.getSelectedFile();

                    // reset program
                    resetProgram(false);

                    // check if import was successful
                    boolean success = DataSetup.setupImportData(importFile);

                    // create graphs
                    if (success)
                    {
                        // optimize data setup with Linear Discriminant Analysis
                        DataVisualization.optimizeSetup();
                        DataVisualization.drawGraphs();
                    }
                    else
                    {
                        // add blank graph
                        warningPopup(
                                "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                                "Error: could not open file");

                        // add blank graph
                        graphPanel.add(ChartsAndPlots.blankGraph());
                    }
                }
                else if (results != JFileChooser.CANCEL_OPTION)
                {
                    warningPopup(
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file");

                    // add blank graph
                    graphPanel.add(ChartsAndPlots.blankGraph());
                }
            }
            else
            {
                warningPopup(
                        "Please create a project before importing data.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not import data");

                // add blank graph
                graphPanel.add(ChartsAndPlots.blankGraph());
            }

            // repaint and revalidate graph
            DV.graphPanel.repaint();
            DV.graphPanel.revalidate();
        }
        catch (Exception e)
        {
            warningPopup(
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file");

            // add blank graph
            graphPanel.add(ChartsAndPlots.blankGraph());
            DV.graphPanel.repaint();
            DV.graphPanel.revalidate();
        }
    }


    /**
     * Opens saved project
     */
    private void openSavedProject()
    {
        try
        {
            // set filter on file chooser
            JFileChooser fileDialog = new JFileChooser();
            fileDialog.setFileFilter(new FileNameExtensionFilter("DV2", "DV2"));

            // set to current directory
            File workingDirectory = new File(System.getProperty("user.dir"));
            fileDialog.setCurrentDirectory(workingDirectory);

            // open file dialog
            int results = fileDialog.showOpenDialog(mainFrame);

            if (results == JFileChooser.APPROVE_OPTION)
            {
                File projectFile = fileDialog.getSelectedFile();

                // reset program
                resetProgram(true);

                // check if import was successful
                DataSetup.setupProjectData(projectFile);

                // set vertical scale of graphs
                /**
                 * CONSTRUCTION
                 */
                //DataVisualization.verticalScale = classNumber > 1 ? 0.4 : 0.8;

                // create angle sliders
                angleSliderPanel.setLayout(new GridLayout(DV.fieldLength, 0));

                for (int i = 0; i < fieldLength; i++)
                    AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);

                // create graphs
                DataVisualization.drawGraphs();

                // repaint DV
                DV.mainFrame.repaint();
                DV.mainFrame.revalidate();
            }
            else if (results != JFileChooser.CANCEL_OPTION)
            {
                warningPopup(
                        "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not open file");

                // add blank graph
                graphPanel.add(ChartsAndPlots.blankGraph());
                DV.graphPanel.repaint();
                DV.crossValidationPanel.repaint();
                DV.graphPanel.revalidate();
                DV.crossValidationPanel.revalidate();
            }
        }
        catch (Exception e)
        {
            warningPopup(
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file");

            // add blank graph
            graphPanel.add(ChartsAndPlots.blankGraph());
            DV.graphPanel.repaint();
            DV.crossValidationPanel.repaint();
            DV.graphPanel.revalidate();
            DV.crossValidationPanel.revalidate();
        }
    }


    /** Saves project
     * Note: project must already have a save
     */
    private void saveProject()
    {
        if (trainData != null && projectSaveName != null)
        {
            try
            {
                // write to csv file
                Writer out = new FileWriter(projectSaveName, false);

                // save graph colors
                out.write(graphColors[0].getRed() + ",");
                out.write(graphColors[0].getGreen() + ",");
                out.write(graphColors[0].getBlue() + "\n");
                out.write(graphColors[1].getRed() + ",");
                out.write(graphColors[1].getGreen() + ",");
                out.write(graphColors[1].getBlue() + "\n");
                out.write(background.getRed() + ",");
                out.write(background.getGreen() + ",");
                out.write(background.getBlue() + "\n");

                // save line colors
                out.write(domainLines.getRed() + ",");
                out.write(domainLines.getGreen() + ",");
                out.write(domainLines.getBlue() + "\n");
                out.write(overlapLines.getRed() + ",");
                out.write(overlapLines.getGreen() + ",");
                out.write(overlapLines.getBlue() + "\n");
                out.write(thresholdLine.getRed() + ",");
                out.write(thresholdLine.getGreen() + ",");
                out.write(thresholdLine.getBlue() + "\n");

                // save data format
                if (hasID) out.write("1,");
                else out.write("0,");
                if (hasClasses) out.write("1,");
                else out.write("0,");
                if (zScoreMinMax) out.write("1\n");
                else out.write("0\n");

                // save field length
                out.write(fieldLength + "\n");

                // save angles
                for (int i = 0; i < angles.length; i++)
                {
                    if (i != angles.length - 1)
                        out.write(angles[i] + ",");
                    else
                        out.write(angles[i] + "\n");
                }

                // save threshold
                out.write(threshold + "\n");

                // save overlap area
                out.write(overlapArea[0] + ",");
                out.write(overlapArea[1] + "\n");

                // save domain area
                out.write(domainArea[0] + ",");
                out.write(domainArea[1] + "\n");

                // save analytics toggles
                if (prevAllDataChecked) out.write("1,");
                else out.write("0,");
                if (allDataChecked) out.write("1,");
                else out.write("0,");
                if (withoutOverlapChecked) out.write("1,");
                else out.write("0,");
                if (overlapChecked) out.write("1,");
                else out.write("0,");
                if (worstCaseChecked) out.write("1,");
                else out.write("0,");
                if (userValidationChecked) out.write("1,");
                else out.write("0,");
                if (userValidationImported) out.write("1,");
                else out.write("0,");
                if (crossValidationChecked) out.write("1\n");
                else out.write("0\n");

                // are there previous confusion matrices
                if (!prevAllDataCM.isEmpty())
                    out.write(prevAllDataCM.size() + "\n");
                else
                    out.write("0\n");

                // save previous confusion matrices
                for (String s : prevAllDataCM)
                {
                    char[] cm = s.toCharArray();

                    for (int j = 0; j < cm.length; j++)
                    {
                        // replace newline character with placeholder
                        if (cm[j] == '\n')
                            cm[j] = '~';
                    }

                    out.write(Arrays.toString(cm) + "\n");
                }

                // save k-folds
                out.write(kFolds + "\n");

                // save number of classes
                out.write(classNumber + "\n");

                // save visualized classes
                out.write(upperClass + "\n");

                for (int i = 0; i < lowerClasses.size(); i++)
                {
                    if (i != lowerClasses.size() - 1)
                    {
                        if (lowerClasses.get(i)) out.write("1,");
                        else out.write("0,");
                    }
                    else
                    {
                        if (lowerClasses.get(i)) out.write("1\n");
                        else out.write("0\n");
                    }
                }

                // save class order
                if (upperIsLower) out.write("1\n");
                else out.write("0\n");

                // save unique classes
                for (int i = 0; i < uniqueClasses.size(); i++)
                {
                    if (i != uniqueClasses.size() - 1)
                        out.write(uniqueClasses.get(i) + ",");
                    else
                        out.write(uniqueClasses.get(i) + "\n");
                }

                // save fieldNames
                for (int i = 0; i < fieldNames.size(); i++)
                {
                    if (i != fieldNames.size() - 1)
                        out.write(fieldNames.get(i) + ",");
                    else
                        out.write(fieldNames.get(i) + "\n");
                }

                // save data
                for (DataObject normData : trainData)
                {
                    // save number of datapoints
                    out.write(normData.data.length + "\n");

                    for (int j = 0; j < normData.data.length; j++)
                    {
                        for (int k = 0; k < fieldLength; k++)
                        {
                            if (k != fieldLength - 1)
                                out.write(normData.data[j][k] + ",");
                            else
                                out.write(normData.data[j][k] + "\n");
                        }
                    }
                }

                // save original data
                for (DataObject origData : originalData)
                {
                    // save number of datapoints
                    out.write(origData.data.length + "\n");

                    for (int j = 0; j < origData.data.length; j++)
                    {
                        for (int k = 0; k < fieldLength; k++)
                        {
                            if (k != fieldLength - 1)
                                out.write(origData.data[j][k] + ",");
                            else
                                out.write(origData.data[j][k] + "\n");
                        }
                    }
                }


                if (userValidationImported)
                {
                    // save validation data
                    for (DataObject valData : validationData)
                    {
                        // save number of datapoints
                        out.write(valData.data.length + "\n");

                        for (int j = 0; j < valData.data.length; j++)
                        {
                            for (int k = 0; k < fieldLength; k++)
                            {
                                if (k != fieldLength - 1)
                                    out.write(valData.data[j][k] + ",");
                                else
                                    out.write(valData.data[j][k] + "\n");
                            }
                        }
                    }
                }

                // close file
                out.close();
            }
            catch(IOException e)
            {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
        else if (trainData == null)
        {
            warningPopup(
                    "Please create a project before saving.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not create project save");
        }
        else
        {
            warningPopup(
                    "There is no project save available. Please use \"Save As\" instead.\nFor additional information, please view the \"Help\" tab.",
                    "Error: no project save available");
        }
    }


    /**
     * Creates new save of project
     */
    private void saveProjectAs()
    {
        if (trainData != null)
        {
            try
            {
                // create save file dialog
                JFileChooser fileSaver = new JFileChooser();
                fileSaver.setDialogType(JFileChooser.SAVE_DIALOG);
                fileSaver.setAcceptAllFileFilterUsed(false);
                fileSaver.addChoosableFileFilter(new FileNameExtensionFilter("DV2 file", "DV2"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileSaver.setCurrentDirectory(workingDirectory);

                if (fileSaver.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
                {
                    // get file
                    File fileToSave = fileSaver.getSelectedFile();
                    String fileName = fileToSave.toString();

                    if (fileName.contains(".") && !fileName.contains(".DV2"))
                    {
                        warningPopup(
                                "All save files must have a .DV2 extension.",
                                "Error: save file must be a DV2");
                    }

                    // add csv extension if not explicitly typed
                    if (!fileName.contains(".DV2"))
                        fileName += ".DV2";

                    projectSaveName = fileName;

                    // write to csv file
                    Writer out = new FileWriter(fileName, false);

                    // save graph colors
                    out.write(graphColors[0].getRed() + ",");
                    out.write(graphColors[0].getGreen() + ",");
                    out.write(graphColors[0].getBlue() + "\n");
                    out.write(graphColors[1].getRed() + ",");
                    out.write(graphColors[1].getGreen() + ",");
                    out.write(graphColors[1].getBlue() + "\n");
                    out.write(background.getRed() + ",");
                    out.write(background.getGreen() + ",");
                    out.write(background.getBlue() + "\n");

                    // save line colors
                    out.write(domainLines.getRed() + ",");
                    out.write(domainLines.getGreen() + ",");
                    out.write(domainLines.getBlue() + "\n");
                    out.write(overlapLines.getRed() + ",");
                    out.write(overlapLines.getGreen() + ",");
                    out.write(overlapLines.getBlue() + "\n");
                    out.write(thresholdLine.getRed() + ",");
                    out.write(thresholdLine.getGreen() + ",");
                    out.write(thresholdLine.getBlue() + "\n");

                    // save data format
                    if (hasID) out.write("1,");
                    else out.write("0,");
                    if (hasClasses) out.write("1,");
                    else out.write("0,");
                    if (zScoreMinMax) out.write("1\n");
                    else out.write("0\n");

                    // save field length
                    out.write(fieldLength + "\n");

                    // save angles
                    for (int i = 0; i < angles.length; i++)
                    {
                        if (i != angles.length - 1)
                            out.write(angles[i] + ",");
                        else
                            out.write(angles[i] + "\n");
                    }

                    // save threshold
                    out.write(threshold + "\n");

                    // save overlap area
                    out.write(overlapArea[0] + ",");
                    out.write(overlapArea[1] + "\n");

                    // save domain area
                    out.write(domainArea[0] + ",");
                    out.write(domainArea[1] + "\n");

                    // save analytics toggles
                    if (prevAllDataChecked) out.write("1,");
                    else out.write("0,");
                    if (allDataChecked) out.write("1,");
                    else out.write("0,");
                    if (withoutOverlapChecked) out.write("1,");
                    else out.write("0,");
                    if (overlapChecked) out.write("1,");
                    else out.write("0,");
                    if (worstCaseChecked) out.write("1,");
                    else out.write("0,");
                    if (userValidationChecked) out.write("1,");
                    else out.write("0,");
                    if (userValidationImported) out.write("1,");
                    else out.write("0,");
                    if (crossValidationChecked) out.write("1\n");
                    else out.write("0\n");

                    // number of previous confusion matrices
                    out.write(prevAllDataCM.size() + "\n");

                    // save previous confusion matrices
                    for (String s : prevAllDataCM)
                    {
                        char[] cm = s.toCharArray();

                        for (int j = 0; j < cm.length; j++)
                        {
                            // replace newline character with placeholder
                            if (cm[j] == '\n')
                                cm[j] = '~';
                            else if (cm[j] == ',')
                                cm[j] = '`';
                        }

                        out.write(new String(cm) + "\n");
                    }

                    // save k-folds
                    out.write(kFolds + "\n");

                    // save number of classes
                    out.write(classNumber + "\n");

                    // save visualized classes
                    out.write(upperClass + "\n");

                    for (int i = 0; i < lowerClasses.size(); i++)
                    {
                        if (i != lowerClasses.size() - 1)
                        {
                            if (lowerClasses.get(i)) out.write("1,");
                            else out.write("0,");
                        }
                        else
                        {
                            if (lowerClasses.get(i)) out.write("1\n");
                            else out.write("0\n");
                        }
                    }

                    // save class order
                    if (upperIsLower) out.write("1\n");
                    else out.write("0\n");

                    // save unique classes
                    for (int i = 0; i < uniqueClasses.size(); i++)
                    {
                        if (i != uniqueClasses.size() - 1)
                            out.write(uniqueClasses.get(i) + ",");
                        else
                            out.write(uniqueClasses.get(i) + "\n");
                    }

                    // save fieldNames
                    for (int i = 0; i < fieldNames.size(); i++)
                    {
                        if (i != fieldNames.size() - 1)
                            out.write(fieldNames.get(i) + ",");
                        else
                            out.write(fieldNames.get(i) + "\n");
                    }

                    // save data
                    for (DataObject normData : trainData)
                    {
                        // save number of datapoints
                        out.write(normData.data.length + "\n");

                        for (int j = 0; j < normData.data.length; j++)
                        {
                            for (int k = 0; k < fieldLength; k++)
                            {
                                if (k != fieldLength - 1)
                                    out.write(normData.data[j][k] + ",");
                                else
                                    out.write(normData.data[j][k] + "\n");
                            }
                        }
                    }

                    // save original data
                    for (DataObject origData : originalData)
                    {
                        // save number of datapoints
                        out.write(origData.data.length + "\n");

                        for (int j = 0; j < origData.data.length; j++)
                        {
                            for (int k = 0; k < fieldLength; k++)
                            {
                                if (k != fieldLength - 1)
                                    out.write(origData.data[j][k] + ",");
                                else
                                    out.write(origData.data[j][k] + "\n");
                            }
                        }
                    }


                    if (userValidationImported)
                    {
                        // save validation data
                        for (DataObject valData : validationData)
                        {
                            // save number of datapoints
                            out.write(valData.data.length + "\n");

                            for (int j = 0; j < valData.data.length; j++)
                            {
                                for (int k = 0; k < fieldLength; k++)
                                {
                                    if (k != fieldLength - 1)
                                        out.write(valData.data[j][k] + ",");
                                    else
                                        out.write(valData.data[j][k] + "\n");
                                }
                            }
                        }
                    }

                    // close file
                    out.close();
                }
            }
            catch(IOException e)
            {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
        else
        {
            warningPopup(
                    "Please create a project before saving.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not create project save");
        }
    }


    /**
     * Creates user validation set
     */
    private void createUserValidationSet()
    {
        try
        {
            if (!trainData.isEmpty() && DV.classNumber > 1)
            {
                // set filter on file chooser
                JFileChooser fileDialog = new JFileChooser();
                fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileDialog.setCurrentDirectory(workingDirectory);

                // open file dialog
                if (fileDialog.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
                {
                    File valFile = fileDialog.getSelectedFile();

                    // check if validation set was successful
                    userValidationImported = DataSetup.setupValidationData(valFile);

                    // informs of validation data status
                    if (userValidationImported)
                    {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "Validation set has been successfully created.\nCreating confusion matrices.",
                                "Success: validation set has been created",
                                JOptionPane.INFORMATION_MESSAGE);

                        // regenerate confusion matrices
                        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                        analytics.execute();

                        // revalidate graphs and confusion matrices
                        DV.graphPanel.repaint();
                        DV.confusionMatrixPanel.repaint();
                        DV.graphPanel.revalidate();
                        DV.confusionMatrixPanel.revalidate();
                    }
                    else
                    {
                        warningPopup(
                                "The validation set was not able to be created.\nPlease ensure the validation data's file has the same format as the original data file.",
                                "Error: failed to create validation set");
                    }
                }
                else
                {
                    warningPopup(
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file");
                }
            }
            else if (DV.classNumber == 1)
            {
                warningPopup(
                        "Not enough classes to create validation set.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not create validation set");
            }
            else
            {
                warningPopup(
                        "Please create a project before creating a validation set.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not create validation set");
            }
        }
        catch (Exception e)
        {
            warningPopup(
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file");
        }
    }


    /**
     * Creates informative popup explaining Min-Max normalization
     * and z-Score Min-Max normalization
     */
    public static void normalizationInfoPopup()
    {
        JOptionPane.showMessageDialog(
                mainFrame,
                "Min-Max Normalization - normalizes data linearly " +
                        "from [0,1] by subtracting the minimum and dividing by the range.\n" +
                        "z-Score Min-Max Normalization - performs a standardization by subtracting " +
                        "the mean and dividing by the standard deviation before performing a Min-Max normalization.",
                "Normalization Help",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    /**
     * Creates informative popup
     */
    public static void informationPopup(String title, String message)
    {
        JOptionPane.showMessageDialog(
                DV.mainFrame,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE);
    }


    /**
     * Creates warning popup
     */
    public static void warningPopup(String title, String message)
    {
        JOptionPane.showMessageDialog(
                DV.mainFrame,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Resets DV program
     * @param remove_classes whether to keep unique classes or not
     */
    private void resetProgram(boolean remove_classes)
    {
        // reset data files
        DV.dataFiles = new ArrayList<>();

        // reset upper and lower class order
        DV.upperIsLower = true;

        // reset panels
        angleSliderPanel.removeAll();
        graphPanel.removeAll();

        // reset classes
        if (remove_classes)
            uniqueClasses = null;

        // reset previous confusion matrices
        prevAllDataCM = new ArrayList<>();

        // reset previous all data classifications
        prevAllDataClassifications = new ArrayList<>();

        // reset cross validation
        crossValidationNotGenerated = true;

        // reset graphs
        drawOverlap = false;

        // reset popup
        DataVisualization.showPopup = true;
    }


    /**
     * Gets data classes
     * @return data classes
     */
    public static String get_classes()
    {
        StringBuilder classes = new StringBuilder();
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            classes.append("Class ").append(i).append(": ").append(DV.trainData.get(i).className);
            if (i != DV.trainData.size() - 1)
                classes.append(", ");
        }

        return classes.toString();
    }

    /**
     * Gets the appropriate color for a class based on its name
     * @param className the name of the class
     * @return the color for the class
     */
    public static Color getClassColor(String className)
    {
        if (className == null) return classColors[0];
        
        String lowerClassName = className.toLowerCase();
        
        // Special handling for benign and malignant
        if (lowerClassName.contains("benign"))
            return Color.GREEN;
        else if (lowerClassName.contains("malignant"))
            return Color.RED;
        
        // For other classes, use a hash-based approach to get consistent colors
        int hash = Math.abs(className.hashCode());
        return classColors[hash % classColors.length];
    }
}
