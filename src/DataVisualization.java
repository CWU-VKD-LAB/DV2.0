import Sliders.RangeSlider;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;
import org.jfree.chart.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataVisualization
{
    // exception logger
    private final static Logger LOGGER = Logger.getLogger(Analytics.class.getName());

    // holds upper and lower graphs
    final static Map<Integer, JFreeChart> GRAPHS = new HashMap<>();
    final static Map<Integer, JPanel> REMOTE_GRAPHS = new HashMap<>();

    // warn user about scaling
    static boolean showPopup;

    // graphs
    static Graph upperGraph;
    static Graph lowerGraph;
    static RegGraph regGraph;


    /**
     * Updates coordinates for data
     */
    private static void updatePoints()
    {
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            // check if class is visualized
            if (i == DV.upperClass || DV.lowerClasses.get(i))
                DV.trainData.get(i).updateCoordinatesGLC(DV.angles);
        }
    }


    /**
     * Optimizes threshold for visualization
     */
    private static void getThreshold()
    {
        // find the best threshold when upperIsLower true
        findLocalBestThreshold(0);

        // if accuracy is less than half, the upper class is lower
        if (DV.accuracy < 50)
        {
            DV.upperIsLower = true;
            findGlobalBestThreshold(0);
        }
    }


    /**
     * Gets optimal angles and threshold
     * then gets overlap and domain
     * areas
     */
    public static void optimizeSetup()
    {
        // set domain are to max length
        DV.domainArea = new double[] { -DV.fieldLength, DV.fieldLength };

        if (DV.classNumber > 1)
        {
            // get optimal angles and threshold
            LDA();
            updatePoints();
            getThreshold();
            optimizeAngles(false);
        }
        else
        {
            // set threshold to left end of graph
            DV.threshold = -DV.fieldLength;
            DV.thresholdSlider.setValue(0);
        }

        // get overlap area
        getOverlap();

        // setup angle slider panel
        DV.angleSliderPanel.removeAll();

        // setup angle sliders
        for (int i = 0; i < DV.fieldLength; i++)
            AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);

        DV.angleSliderPanel.repaint();
        DV.angleSliderPanel.revalidate();
    }


    /**
     * Gets optimal angles and threshold
     * then gets overlap and domain
     * areas
     */
    public static void optimizeRegSetup()
    {
        // set domain are to max length
        DV.domainArea = new double[] { -DV.fieldLength, DV.fieldLength };

        // get optimal angles and threshold
        LR();
        updatePoints();

        // setup angle slider panel
        DV.angleSliderPanel.removeAll();

        // setup angle sliders
        for (int i = 0; i < DV.fieldLength; i++)
            AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);

        DV.angleSliderPanel.repaint();
        DV.angleSliderPanel.revalidate();
    }


    /**
     * Linear Discriminant Analysis (LDA)
     * Gets optimal angles and threshold
     */
    private static void LDA()
    {

        // Create the LDA process builder
        ProcessBuilder lda;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows
            lda = new ProcessBuilder("cmd", "/c",
                    "source\\Python\\LinearDiscriminantAnalysis\\LinearDiscriminantAnalysis.exe",
                    "source\\Python\\DV_data.csv");
        } else {
            // macOS or Unix/Linux
            lda = new ProcessBuilder("python3",
                    "source/Python/LinearDiscriminantAnalysis/LinearDiscriminantAnalysis.py",
                    "source/Python/DV_data.csv");
        }

        try
        {
            // create file for python process
            CSV.createCSVDataObject(DV.trainData, "source\\Python\\DV_data.csv");

            // run python (LDA) process
            Process process = lda.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // if process is running continue
            DV.angleSliderPanel.removeAll();

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles then threshold
                if (cnt < DV.fieldLength)
                {
                    // update angles
                    DV.angles[cnt] = Double.parseDouble(output);
                    cnt++;
                }
                else
                {
                    // get threshold
                    DV.threshold = Double.parseDouble(output);
                    DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);
                }
            }

            // delete created file
            File fileToDelete = new File("source\\Python\\DV_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());

            // repaint and revalidate angle sliders
            DV.angleSliderPanel.repaint();
            DV.angleSliderPanel.revalidate();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Linear Discriminant Analysis", "Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * Linear Regression (LR)
     * Gets optimal angles and values
     */
    private static void LR()
    {
        // create LDA (python) process
        ProcessBuilder lda = new ProcessBuilder("cmd", "/c",
                "python",
                "source\\Python\\code_and_pyinstaller_spec\\LinearRegression.py",
                "source\\Python\\DV_data.csv");

        try
        {
            // create file for python process
            CSV.createRegCSVDataObject(DV.trainData, "source\\Python\\DV_data.csv");

            // run python (LDA) process
            Process process = lda.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // if process is running continue
            DV.angleSliderPanel.removeAll();

            int cnt = 0;
            int set = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles then threshold
                if (cnt > DV.fieldLength + 3)
                {
                    DV.reg_predictions.add(Double.parseDouble(output));
                    DV.reg_true_values.set(set, DV.reg_true_values.get(set) / DV.reg_largest_coef - DV.threshold);
                    set++;
                }
                else if (cnt < DV.fieldLength)
                {
                    // update angles
                    DV.angles[cnt] = Double.parseDouble(output);
                    cnt++;
                }
                else if (cnt == DV.fieldLength)
                {
                    // get threshold
                    DV.threshold = Double.parseDouble(output);
                    cnt++;
                }
                else if (cnt == DV.fieldLength + 1)
                {
                    // get threshold
                    DV.reg_largest_coef = Double.parseDouble(output);
                    cnt++;
                }
                else if (cnt == DV.fieldLength + 2)
                {
                    // get threshold
                    DV.reg_RMSE = Double.parseDouble(output);
                    cnt++;
                }
                else if (cnt == DV.fieldLength + 3)
                {
                    // get threshold
                    DV.reg_avg_val = Double.parseDouble(output);
                    cnt++;
                }
            }

            // delete created file
            File fileToDelete = new File("source\\Python\\DV_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());

            // repaint and revalidate angle sliders
            DV.angleSliderPanel.repaint();
            DV.angleSliderPanel.revalidate();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Linear Discriminant Analysis", "Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * Support Vector Machine (SVM)
     * Gets support vectors
     */
    public static void SVM()
    {
        // create LDA (python) process
        ProcessBuilder svm = new ProcessBuilder("cmd", "/c",
                "source\\Python\\SupportVectorMachine\\SupportVectorMachine.exe",
                "source\\Python\\DV_data.csv");

        try
        {
            // create file for python process
            CSV.createCSVDataObject(DV.trainData, "source\\Python\\DV_data.csv");

            // run python (LDA) process
            Process process = svm.start();
            process.waitFor();

            // setup support vectors
            File svFile = new File("source\\Python\\svm_data.csv");

            if (!DataSetup.setupSupportVectors(svFile))
                JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Support Vector Machines", "Error", JOptionPane.ERROR_MESSAGE);

            // delete created file
            File fileToDelete = new File("source\\Python\\svm_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());
            fileToDelete = new File("source\\Python\\DV_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());
        }
        catch (IOException | InterruptedException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Support Vector Machines", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Searches for the best accuracy across 15% of the total slider range
     * @param bestAccuracy current best accuracy
     */
    public static void findLocalBestThreshold(double bestAccuracy)
    {
        // store current best threshold
        double bestThreshold = DV.threshold;

        // increment equals 2 slider ticks
        double increment = 2 * DV.fieldLength / 200.0;
        DV.threshold -= 15 * increment;

        // search for best threshold
        // search range is 15% of total range
        for (int i = 0; i < 30; i++)
        {
            // calculate accuracy with trial threshold
            getAccuracy();

            // update the best threshold and accuracy
            if (DV.accuracy >= bestAccuracy)
            {
                bestAccuracy = DV.accuracy;
                bestThreshold = DV.threshold;
            }

            // new trial threshold
            DV.threshold += increment;
        }

        // set best accuracy
        DV.accuracy = bestAccuracy;

        // use best threshold
        DV.threshold = bestThreshold;

        // set slider to best
        DV.thresholdSlider.setValue((int) (bestThreshold / DV.fieldLength * 200) + 200);
    }


    /**
     * Searches for the best accuracy across the total slider range
     * @param bestAccuracy current best accuracy
     */
    public static void findGlobalBestThreshold(double bestAccuracy)
    {
        // store current best threshold
        double bestThreshold = DV.threshold;

        // increment equals 2 slider ticks
        double increment = DV.fieldLength / 200.0;
        DV.threshold = -DV.fieldLength;

        // search for best threshold
        // search range is 100% of total range
        for (int i = 0; i < 400; i++)
        {
            // calculate accuracy with trial threshold
            getAccuracy();

            // update the best threshold and accuracy
            if (DV.accuracy >= bestAccuracy)
            {
                bestAccuracy = DV.accuracy;
                bestThreshold = DV.threshold;
            }

            // new trial threshold
            DV.threshold += increment;
        }

        // set best accuracy
        DV.accuracy = bestAccuracy;

        // use best threshold
        DV.threshold = bestThreshold;

        // set slider to best
        DV.thresholdSlider.setValue((int) (bestThreshold / DV.fieldLength * 200) + 200);
    }


    /**
     * Uses random gradient search
     * to attempt optimizing angles.
     * Uses optimizeThreshold() to
     * optimize threshold.
     * @param informUser show popup relating optimization success
     */
    public static void optimizeAngles(boolean informUser)
     {
         if (DV.classNumber > 1)
         {
             // store previous angles and threshold
             DV.prevAngles = Arrays.copyOf(DV.angles, DV.fieldLength);
             DV.prevThreshold = DV.threshold;

             // store current best angles, threshold, and accuracy
             double[] currentBestAngles = Arrays.copyOf(DV.angles, DV.fieldLength);
             double currentBestThreshold = DV.threshold;
             double currentBestAccuracy = DV.accuracy;

             // get random
             Random rand = new Random(System.currentTimeMillis());

             // get count and foundBetter
             int cnt = 0;
             boolean foundBetter = false;
             int[] angleRange = new int[]{ 0, 180 };

             // try optimizing 200 times
             while (cnt < 200)
             {
                 // get random angles
                 for (int i = 0; i < DV.angles.length; i++)
                 {
                     int gradient = (rand.nextInt(101) - 50);
                     double fieldAngle = currentBestAngles[i] + gradient;

                     if (fieldAngle < angleRange[0])
                         fieldAngle = angleRange[0];
                     else if (fieldAngle > angleRange[1])
                         fieldAngle = angleRange[1];

                     DV.angles[i] = fieldAngle;
                 }

                 // optimize threshold for new angles
                 findLocalBestThreshold(currentBestAccuracy);

                 // get accuracy for new setup
                 getAccuracy();

                 // update current bests if accuracy improved
                 if (DV.accuracy > currentBestAccuracy)
                 {
                     foundBetter = true;

                     currentBestAngles= Arrays.copyOf(DV.angles, DV.fieldLength);
                     currentBestThreshold = DV.threshold;
                     currentBestAccuracy = DV.accuracy;
                 }

                 cnt++;
             }

             // set accuracy and threshold to the best
             DV.threshold = currentBestThreshold;
             DV.accuracy = currentBestAccuracy;

             // remove angle sliders and set layout
             DV.angleSliderPanel.removeAll();

             // inform user if optimization was successful or now
             optimizeAnglesHelper(foundBetter, informUser, currentBestThreshold, currentBestAngles);
         }
        else
        {
            // inform user
            DV.warningPopup("Unable to Optimize",
                    "The current data has no classes: unable to optimize.");
        }
    }


    /**
     * Helper function for optimizeAngles
     * @param foundBetter whether optimization was successful
     * @param informUser whether to inform user of optimization
     * @param currentBestThreshold current best threshold
     * @param currentBestAngles current best angles
     */
    private static void optimizeAnglesHelper(boolean foundBetter, boolean informUser, double currentBestThreshold, double[] currentBestAngles)
    {
        if (foundBetter)
        {
            // update threshold
            DV.threshold = currentBestThreshold;
            DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);

            // update angles
            for (int i = 0; i < DV.angles.length; i++)
            {
                DV.angles[i] = currentBestAngles[i];
                AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
            }

            // inform user
            if (informUser)
            {
                // redraw graphs
                drawGraphs();
                DV.angleSliderPanel.repaint();
                DV.angleSliderPanel.revalidate();

                DV.informationPopup("Optimization Complete",
                        "Visualization has been optimized!");
            }
        }
        else
        {
            // update threshold
            DV.threshold = DV.prevThreshold;
            DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);

            // update angles
            for (int i = 0; i < DV.angles.length; i++)
            {
                DV.angles[i] = DV.prevAngles[i];
                AngleSliders.createSliderPanel("tmp " + 1, (int) (DV.angles[i] * 100), i);//DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
            }

            // inform user
            if (informUser)
            {
                // redraw graphs
                drawGraphs();
                DV.angleSliderPanel.repaint();
                DV.angleSliderPanel.revalidate();

                DV.informationPopup("Unable to Optimize",
                        "Was unable to optimize visualization.\nVisualization is already optimal or near optimal.");
            }
        }
    }


    /**
     * Undoes last optimizations
     */
    public static void undoOptimization()
    {
        // threshold
        DV.angles = Arrays.copyOf(DV.prevAngles, DV.fieldLength);

        // update sliders
        DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);
        DV.angleSliderPanel.removeAll();

        // restore angles
        for (int i = 0; i < DV.fieldLength; i++)
        {
            DV.angles[i] = DV.prevAngles[i];
            AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
        }

        // redraw graphs
        drawGraphs();
        DV.mainFrame.repaint();
        DV.mainFrame.revalidate();
    }


    /**
     * Normalizes angles between [0,90]
     */
    public static void normalizeAngles()
    {
        if (DV.trainData != null)
        {
            JPanel normPanel = new JPanel();
            normPanel.add(new JLabel("Choose a normalization style or click \"Help\" for more information on normalization styles."));

            int choice = -2;
            while (choice == -2)
            {
                choice = JOptionPane.showOptionDialog(
                        DV.mainFrame, normPanel,
                        "Normalize Angles",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        new Object[]{"z-Score Min-Max", "Min-Max", "Help"},
                        null);

                if (choice == 2)
                {
                    DV.normalizationInfoPopup();
                    choice = -2;
                }
            }

            // get min and max values
            double[] minMax = normalizeAnglesHelper(choice);
            double min = minMax[0];
            double max = minMax[1];

            DV.angleSliderPanel.removeAll();

            // normalize between [0, 90]
            for (int i = 0; i < DV.angles.length; i++)
            {
                DV.angles[i] = ((DV.angles[i] - min) / (max - min)) * 90;
                AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
            }

            DV.angleSliderPanel.repaint();
            DV.angleSliderPanel.revalidate();

            DV.upperIsLower = true;
            getThreshold();
            updateGraphs();
        }
        else
            DV.warningPopup("Error: could not normalize angles",
                    "Please create a project before normalizing angles.\nFor additional information, please view the \"Help\" tab.");
    }


    /**
     * Normalizes angles helper
     * @param choice normalization style
     * @return min and max values
     */
    private static double[] normalizeAnglesHelper(int choice)
    {
        if (choice == 0)
        {
            // mean and standard deviation per column
            double mean = 0;
            double sd = 0;

            // get mean for each column
            for (int i = 0; i < DV.angles.length; i++)
                mean += DV.angles[i];

            mean /= DV.angles.length;

            // get standard deviation for each column
            for (int i = 0; i < DV.angles.length; i++)
            {
                sd += Math.pow(DV.angles[i] - mean, 2);

                if (sd < 0.001)
                {
                    String message = String.format("""
                            Standard deviation in column %d is less than 0.001.
                            Please manually enter a minimum and maximum. The standard deviation will get (max - min) / 2.
                            Else, the standard deviation will get 0.001.""", i+1);

                    // ask user for manual min max entry
                    double[] manualMinMax = DataSetup.manualMinMaxEntry(message);

                    // use manual min max or default to 0.001 if null
                    if (manualMinMax != null)
                        sd = (manualMinMax[1] / manualMinMax[0]) / 2;
                    else
                        sd = 0.001;
                }
            }

            sd = Math.sqrt(sd / DV.angles.length);

            // get min and max values
            for (int i = 0; i < DV.angles.length; i++)
                DV.angles[i] = (DV.angles[i] - mean) / sd;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < DV.angles.length; i++)
        {
            if (DV.angles[i] < min)
                min = DV.angles[i];
            if (DV.angles[i] > max)
                max = DV.angles[i];
        }

        return new double[]{min, max};
    }


    /**
     * Gets current accuracy of visualization
     */
    public static void getAccuracy()
    {
        // total correct points and total points
        int[][] pntDist = Analytics.getPointDistribution(DV.trainData, DV.threshold);

        // get accuracy
        DV.accuracy = (double) (pntDist[0][0] + pntDist[1][1]) / (pntDist[0][0] + pntDist[0][1] + pntDist[1][0] + pntDist[1][1]) * 100;

        // reverse which side each class is on relative to the threshold
        if (DV.accuracy <= 50)
        {
            DV.accuracy = 100 - DV.accuracy;
            DV.upperIsLower = !DV.upperIsLower;
        }
    }


    /**
     * Gets overlap area of visualization
     */
    public static void getOverlap()
    {
        if (DV.classNumber > 1 && DV.accuracy != 100)
        {
            // overlap area, previous overlap are, and total area
            DV.overlapArea = new double[] { DV.fieldLength, -DV.fieldLength };
            double[] prevOverlap = { DV.fieldLength, -DV.fieldLength };
            double[] totalArea = { DV.fieldLength, -DV.fieldLength };

            // check if graph has misclassified a point
            // check if previous overlap exists
            boolean[] misclassified = { false, false };
            boolean[] isPrevious = { false, false };

            // find overlap for all classes
            getOverlapHelper(prevOverlap, totalArea, misclassified, isPrevious);

            // if overlap area exceeds 90% or total area, set overlap point to previous overlap point or threshold
            if ((DV.overlapArea[0] + DV.overlapArea[1]) / (totalArea[0] + totalArea[1]) > 0.9)
            {
                double high = Math.abs(totalArea[1]) - Math.abs(DV.overlapArea[1]);
                double low = Math.abs(DV.overlapArea[0]) - Math.abs(totalArea[0]);

                if (high >= low)
                {
                    if (isPrevious[1])
                        DV.overlapArea[1] = prevOverlap[1];
                    else
                        DV.overlapArea[1] = DV.threshold;
                }
                else
                {
                    if (isPrevious[0])
                        DV.overlapArea[0] = prevOverlap[0];
                    else
                        DV.overlapArea[0] = DV.threshold;
                }
            }

            // if a class was never misclassified set overlap point to the threshold
            if (misclassified[0] && !misclassified[1])
                DV.overlapArea[1] = DV.threshold;
            else if (!misclassified[0] && misclassified[1])
                DV.overlapArea[0] = DV.threshold;

            // set slider
            DV.overlapSlider.setValue((int) (DV.overlapArea[0] / DV.fieldLength * 200) + 200);
            DV.overlapSlider.setUpperValue((int) (DV.overlapArea[1] / DV.fieldLength * 200) + 200);
        }
        else
        {
            // set overlap area
            DV.overlapArea = new double[] { DV.fieldLength, DV.fieldLength };

            // set slider
            DV.overlapSlider.setValue(400);
            DV.overlapSlider.setUpperValue(400);
        }
    }


    /**
     * Helper function for getOverlap
     * @param prevOverlap previous overlap area
     * @param totalArea total area
     * @param misclassified whether a point is misclassified
     * @param isPrevious whether a point is previous
     */
    private static void getOverlapHelper(double[] prevOverlap, double[] totalArea, boolean[] misclassified, boolean[] isPrevious)
    {
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.trainData.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.trainData.get(i).coordinates[j][DV.trainData.get(i).coordinates[j].length-1][0];

                    // get classification
                    if ((i == DV.upperClass && DV.upperIsLower) || (DV.lowerClasses.get(i) && !DV.upperIsLower))
                    {
                        // check if endpoint expands total area
                        if (endpoint < totalArea[0])
                            totalArea[0] = endpoint;

                        // check if endpoint is misclassified
                        if (endpoint > DV.threshold)
                        {
                            // check if endpoint expands overlap area
                            if (endpoint > DV.overlapArea[1])
                            {
                                prevOverlap[1] = DV.overlapArea[1];
                                DV.overlapArea[1] = endpoint;

                                if (misclassified[1])
                                    isPrevious[1] = true;

                                misclassified[1] = true;
                            }
                            else if (endpoint != DV.overlapArea[1] && endpoint > prevOverlap[1]) // get largest previous overlap point
                            {
                                prevOverlap[1] = endpoint;
                                isPrevious[1] = true;
                            }
                        }
                    }
                    else
                    {
                        // check if endpoint expands total area
                        if (endpoint > totalArea[1])
                            totalArea[1] = endpoint;

                        // check if endpoint is misclassified
                        if (endpoint <= DV.threshold)
                        {
                            // check if endpoint expands overlap area
                            if (endpoint < DV.overlapArea[0])
                            {
                                prevOverlap[0] = DV.overlapArea[0];
                                DV.overlapArea[0] = endpoint;

                                if (misclassified[0])
                                    isPrevious[0] = true;

                                misclassified[0] = true;
                            }
                            else if (endpoint != DV.overlapArea[0] && endpoint < prevOverlap[0]) // get smallest previous overlap point
                            {
                                prevOverlap[0] = endpoint;
                                isPrevious[0] = true;
                            }
                        }
                    }
                }
            }
        }
    }



    /**
     * Removes mouse listeners from sliders
     */
    private static void removeMouseListeners()
    {
        for (int i = 1; i < 3; i++)
            DV.domainSlider.removeMouseListener(DV.domainSlider.getMouseListeners()[DV.domainSlider.getMouseListeners().length-i]);

        for (int i = 1; i < 3; i++)
            DV.overlapSlider.removeMouseListener(DV.overlapSlider.getMouseListeners()[DV.overlapSlider.getMouseListeners().length-i]);

        // add overlap listeners
        for (int i = 1; i < 3; i++)
            DV.thresholdSlider.removeMouseListener(DV.thresholdSlider.getMouseListeners()[DV.thresholdSlider.getMouseListeners().length-i]);
    }


    /**
     * Draws classification graphs
     */
    public static void drawGraphs()
    {
        // remove old graphs
        if (!GRAPHS.isEmpty())
            removeMouseListeners();

        GRAPHS.clear();
        DV.graphPanel.removeAll();

        if (DV.displayRemoteGraphs)
        {
            REMOTE_GRAPHS.clear();
            DV.remoteGraphPanel.removeAll();
        }

        // holds classes to be graphed
        ArrayList<DataObject> upperObjects = new ArrayList<>(List.of(DV.trainData.get(DV.upperClass)));
        ArrayList<DataObject> lowerObjects = new ArrayList<>();

        // get classes to be graphed
        if (DV.hasClasses)
        {
            for (int j = 0; j < DV.classNumber; j++)
            {
                if (DV.lowerClasses.get(j))
                    lowerObjects.add(DV.trainData.get(j));
            }
        }

        // calculate coordinates
        // scale graph according to largest value
        double graphScale = Math.max(getCoordinates(upperObjects), getCoordinates(lowerObjects));

        // generate analytics
        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
        analytics.execute();

        // generate graphs
        upperGraph = new Graph(upperObjects, 0, graphScale);
        upperGraph.execute();

        lowerGraph = null;
        if (!lowerObjects.isEmpty())
        {
            lowerGraph = new Graph(lowerObjects, 1, graphScale);
            lowerGraph.execute();
        }

        // wait for threads to finish
        try
        {
            upperGraph.get();

            if (DV.hasClasses && lowerGraph != null)
                lowerGraph.get();

            generateGraphs(graphScale);
            analytics.get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }


    /**
     * Generates graphs for visualization
     * @param graphScale highest endpoint frequency on the x coordinate
     */
    private static void generateGraphs(double graphScale)
    {
        GridBagConstraints gpc = new GridBagConstraints();
        gpc.gridx = 0;
        gpc.gridy = 0;
        gpc.weightx = 1;
        gpc.weighty = 1;
        gpc.fill = GridBagConstraints.BOTH;

        // graph characteristics
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setGap(0);
        plot.getDomainAxis().setVisible(false);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        // add graphs in order
        JFreeChart chart = new JFreeChart("", null, plot, false);
        for (int i = 0; i < GRAPHS.size(); i++)
        {
            if (GRAPHS.containsKey(i))
            {
                plot.add((XYPlot) GRAPHS.get(i).getPlot(), 1);

                if (DV.displayRemoteGraphs)
                    DV.remoteGraphPanel.add(REMOTE_GRAPHS.get(i), gpc);
            }
        }

        // add to DV panel
        DV.graphPanel.add(createChartPanel(chart), gpc);

        // revalidate graphs and confusion matrices
        DV.graphPanel.repaint();
        DV.graphPanel.revalidate();

        if (DV.displayRemoteGraphs)
        {
            DV.remoteGraphPanel.repaint();
            DV.remoteGraphPanel.revalidate();
        }

        // warn user if graphs are scaled
        if (!showPopup && graphScale > 1)
        {
            DV.informationPopup("Zoom Warning",
                    """
                    Because of the size, the graphs have been zoomed out.
                    All functionality remains, but there will be empty space on each side of the graph.
                    Zoom in or scale the graphs to remove the white space.
                    """);
            showPopup = false;
        }
    }


    /**
     * Creates ChartPanel of generated graphs
     * @param chart generated graphs
     * @return ChartPanel of graphs
     */
    private static ChartPanel createChartPanel(JFreeChart chart)
    {
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.addChartMouseListener(new ChartMouseListener()
        {
            @Override
            public void chartMouseClicked(ChartMouseEvent e)
            {
                // get clicked entity
                ChartEntity ce = e.getEntity();
                if (ce instanceof XYItemEntity xy)
                {
                    // ensure entity is endpoint
                    if (xy.getDataset().getSeriesCount() > 1)
                        datapointInfoPopup(xy);
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
        });

        return chartPanel;
    }


    /**
     * Shows info for a given datapoint
     * @param xy datapoint
     */
    private static void datapointInfoPopup(XYItemEntity xy)
    {
        // get index and item
        int index = xy.getSeriesIndex();
        int item = xy.getItem();

        // get dataset and value
        XYDataset dataset = xy.getDataset();
        double y = dataset.getYValue(index, item);

        // upper class name and lower class names
        StringBuilder curClassName = new StringBuilder();
        StringBuilder opClassName = new StringBuilder();

        int curClass = -1;

        // find the current class and the index of the data
        if (y < 0)
        {
            // class is not upper so upper is the opposite class
            opClassName.append(DV.trainData.get(DV.upperClass).className);

            // loop through classes until the class containing index is found
            for (int i = 0; i < DV.classNumber; i++)
            {
                if (DV.lowerClasses.get(i))
                {
                    if (!curClassName.isEmpty())
                        curClassName.append(", ");

                    curClassName.append(DV.trainData.get(i).className);

                    // if true, index is within the current class
                    if (curClass == -1)
                    {
                        if (DV.trainData.get(i).data.length > index)
                            curClass = i;
                        else // adjust index to compare to next class
                            index -= DV.trainData.get(i).data.length;
                    }
                }
            }
        }
        else
        {
            curClass = DV.upperClass;
            curClassName.append(DV.trainData.get(curClass).className);

            for (int i = 0; i < DV.trainData.size(); i++)
            {
                if (DV.lowerClasses.get(i))
                {
                    if (!curClassName.isEmpty())
                        opClassName.append(", ");

                    opClassName.append(DV.trainData.get(i).className);
                }
            }
        }

        // show info
        datapointInfo(curClass, index, curClassName, opClassName);
    }


    /**
     * Gets info for a given datapoint
     * @param curClass class of datapoint
     * @param index index of datapoint within class
     * @param curClassName class name
     * @param opClassName opposite class names
     */
    private static void datapointInfo(int curClass, int index, StringBuilder curClassName, StringBuilder opClassName)
    {
        // get mouse location
        Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
        int mouseX = (int) mouseLoc.getX();
        int mouseY = (int) mouseLoc.getY();

        // get case data
        String chosenDataPoint = caseValue(curClass, index);

        // create text pane
        JTextPane dataPontText = new JTextPane();
        dataPontText.setContentType("text/html");
        dataPontText.setText(chosenDataPoint);
        dataPontText.setEditable(false);
        JScrollPane textScroll = new JScrollPane(dataPontText);

        // create popup
        JOptionPane optionPane = new JOptionPane(textScroll, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new Object[]{"Highlight Point", "Create LDF"}, 0);
        JDialog dialog = optionPane.createDialog(null, "Datapoint");
        dialog.setLocation(mouseX, mouseY);
        dialog.setSize(new Dimension(600, 200));
        dialog.setResizable(true);
        dialog.setVisible(true);

        if (optionPane.getValue() != null)
        {
            if (optionPane.getValue().equals("Highlight Point"))
            {
                DV.highlights[curClass][index] = !DV.highlights[curClass][index];
                DataVisualization.drawGraphs();
            }
            else
            {
                new LDFCaseRule(chosenDataPoint, curClassName.toString(), opClassName.toString(), curClass, index);
            }
        }
    }


    /**
     * Values and class of case
     * @param curClass class of case
     * @param index index of case within class
     * @return values and class of case
     */
    private static String caseValue(int curClass, int index)
    {
        StringBuilder originalPoint = new StringBuilder("<b>Original Point: </b>");
        StringBuilder normalPoint = new StringBuilder("<b>Normalized Point: </b>");

        for (int i = 0; i < DV.fieldLength; i++)
        {
            // get feature values
            String tmpOrig = String.format("%.2f", DV.originalData.get(curClass).data[index][i]);
            String tmpNorm = String.format("%.2f", DV.trainData.get(curClass).data[index][i]);

            // add values to points
            originalPoint.append(tmpOrig);
            normalPoint.append(tmpNorm);

            if (i != DV.fieldLength - 1)
            {
                originalPoint.append(", ");
                normalPoint.append(", ");

                // add new line
                if (i % 10 == 9)
                {
                    originalPoint.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
                    normalPoint.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
                }
            }
        }

        // case values
        return "<html>" + "<b>Class: </b>" + DV.uniqueClasses.get(curClass) + "<br/>" + originalPoint + "<br/>" + normalPoint + "</html>";
    }


    /**
     * Generates coordinates for dataObjects that will be graphed
     * @param dataObjects dataObjects to be graphed
     * @return whether the graphs need to be scaled or not
     */
    public static double getCoordinates(ArrayList<DataObject> dataObjects)
    {
        double graphScale = 1;

        // get coordinates
        for (DataObject dataObject : dataObjects)
        {
            double tmpScale = dataObject.updateCoordinatesGLC(DV.angles);

            // check for greater scale
            if (tmpScale > graphScale)
                graphScale = tmpScale;
        }

        return graphScale;
    }


    /**
     * Gets highest endpoint frequency on the x coordinate of the visualization
     * @return highest endpoint frequency on the x coordinate
     */
    private static double getMaxFrequency()
    {
        double max = 0;
        // get bar lengths
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            int[] barRanges = new int[400];
            // translate endpoint to slider ticks
            // increment bar which endpoint lands
            for (int j = 0; j < DV.trainData.get(i).coordinates.length; j++)
            {
                int tmpTick = (int) (Math.round((DV.trainData.get(i).coordinates[j][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                barRanges[tmpTick]++;

                if (barRanges[tmpTick] > max)
                    max = barRanges[tmpTick];
            }
        }

        return max;
    }


    /**
     * Updates graphs with new data
     */
    public static void updateGraphs()
    {
        double graphScale;
        if (DV.classNumber > 1)
        {
            graphScale = Math.max(getCoordinates(upperGraph.DATA_OBJECTS), getCoordinates(lowerGraph.DATA_OBJECTS));

            upperGraph.setGraphScale(graphScale);
            lowerGraph.setGraphScale(graphScale);

            upperGraph.updateGraph();
            lowerGraph.updateGraph();
        }
        else
        {
            graphScale = getCoordinates(upperGraph.DATA_OBJECTS);
            upperGraph.setGraphScale(graphScale);
            upperGraph.updateGraph();
        }

        // get analytics
        getAnalytics();
    }


    /**
     * Gets analytics for visualization
     */
    private static void getAnalytics()
    {
        // generate analytics
        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
        analytics.execute();

        // wait for generation
        try
        {
            analytics.get();
        }
        catch (InterruptedException | ExecutionException ex)
        {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        // revalidate graphs and confusion matrices
        DV.confusionMatrixPanel.repaint();
        DV.confusionMatrixPanel.revalidate();

        if (DV.displayRemoteAnalytics)
        {
            DV.remoteConfusionMatrixPanel.repaint();
            DV.remoteConfusionMatrixPanel.revalidate();
        }
    }


    /**
     * Draw graph with specified parameters
     */
    private static class Graph extends SwingWorker<Boolean, Void>
    {
        final ArrayList<DataObject> DATA_OBJECTS;
        final int UPPER_OR_LOWER;
        double GRAPH_SCALE;
        final double buffer;
        double lineHeight;

        // create main renderer and dataset
        final XYLineAndShapeRenderer lineRenderer;
        final XYSeriesCollection graphLines;

        final XYLineAndShapeRenderer highlightLineRenderer;
        final XYSeriesCollection highlightLines;
        final XYLineAndShapeRenderer highlightEndpointRenderer;
        final XYSeriesCollection highlightEndpoints;

        // create SVM renderer and dataset
        final XYLineAndShapeRenderer svmLineRenderer;
        final XYSeriesCollection svmGraphLines;

        // create renderer for domain, overlap, and threshold lines
        final XYLineAndShapeRenderer domainRenderer;
        final XYLineAndShapeRenderer overlapRenderer;
        final XYLineAndShapeRenderer thresholdRenderer;
        final XYSeriesCollection domain;
        final XYSeriesCollection overlap;
        final XYSeriesCollection threshold;
        final XYSeries domainMaxLine;
        final XYSeries domainMinLine;
        final XYSeries overlapMaxLine;
        final XYSeries overlapMinLine;
        final XYSeries thresholdLine;

        // renderer for endpoint, midpoint, and timeline
        final XYLineAndShapeRenderer endpointRenderer;
        final XYLineAndShapeRenderer svmEndpointRenderer;
        final XYLineAndShapeRenderer midpointRenderer;
        final XYLineAndShapeRenderer svmMidpointRenderer;
        final XYLineAndShapeRenderer timeLineRenderer;
        final XYLineAndShapeRenderer svmTimeLineRenderer;
        final XYSeriesCollection endpoints;
        final XYSeriesCollection svmEndpoints;
        final XYSeriesCollection midpoints;
        final XYSeriesCollection svmMidpoints;
        final XYSeriesCollection timeLine;
        final XYSeriesCollection svmTimeLine;
        final XYSeries svmEndpointSeries;
        final XYSeries midpointSeries;
        final XYSeries svmMidpointSeries;
        final XYSeries timeLineSeries;
        final XYSeries svmTimeLineSeries;

        // renderer for bars
        final XYIntervalSeriesCollection bars;
        final XYBarRenderer barRenderer;

        // stroke and shapes
        final BasicStroke graphLineStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        final BasicStroke domainStroke = new BasicStroke(2f);
        final BasicStroke domainActiveStroke = new BasicStroke(4f);
        final BasicStroke thresholdOverlapStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);
        final BasicStroke thresholdOverlapActiveStroke = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12f, 6f}, 0.0f);
        final Ellipse2D.Double endpointShape = new Ellipse2D.Double(-1, -1, 2, 2);
        final Ellipse2D.Double midpointShape = new Ellipse2D.Double(-0.5, -0.5, 1, 1);

        // get chart and plot
        JFreeChart chart;
        XYPlot plot;

        /**
         * Initializes parameters
         * @param dataObjects classes to draw
         * @param upperOrLower draw up when upper (0) and down when lower (1)
         * @param graphScale how much to zoom out the graphs
         */
        Graph(ArrayList<DataObject> dataObjects, int upperOrLower, double graphScale)
        {
            this.DATA_OBJECTS = dataObjects;
            this.UPPER_OR_LOWER = upperOrLower;
            this.GRAPH_SCALE = graphScale;
            this.buffer = DV.fieldLength / 10.0;
            setLineHeight();

            // create main renderer and dataset
            this.lineRenderer = new XYLineAndShapeRenderer(true, false);
            this.graphLines = new XYSeriesCollection();
            this.graphLines.setNotify(false);

            this.highlightLineRenderer = new XYLineAndShapeRenderer(true, false);
            this.highlightLines = new XYSeriesCollection();
            this.highlightLines.setNotify(false);
            this.highlightEndpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.highlightEndpoints = new XYSeriesCollection();
            this.highlightEndpoints.setNotify(false);

            // create SVM renderer and dataset
            this.svmLineRenderer = new XYLineAndShapeRenderer(true, false);
            this.svmGraphLines = new XYSeriesCollection();
            this.svmGraphLines.setNotify(false);

            // create renderer for domain, overlap, and threshold lines
            this.domainRenderer = new XYLineAndShapeRenderer(true, false);
            this.overlapRenderer = new XYLineAndShapeRenderer(true, false);
            this.thresholdRenderer = new XYLineAndShapeRenderer(true, false);
            this.domain = new XYSeriesCollection();
            this.domain.setNotify(false);
            this.overlap = new XYSeriesCollection();
            this.overlap.setNotify(false);
            this.threshold = new XYSeriesCollection();
            this.threshold.setNotify(false);
            this.domainMaxLine = new XYSeries(-1, false, true);
            this.domainMaxLine.setNotify(false);
            this.domainMinLine = new XYSeries(-2, false, true);
            this.domainMinLine.setNotify(false);
            this.overlapMaxLine = new XYSeries(-3, false, true);
            this.overlapMaxLine.setNotify(false);
            this.overlapMinLine = new XYSeries(-4, false, true);
            this.overlapMinLine.setNotify(false);
            this.thresholdLine = new XYSeries(0, false, true);
            this.thresholdLine.setNotify(false);

            // renderer for endpoint, midpoint, and timeline
            this.endpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.svmEndpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.midpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.svmMidpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.timeLineRenderer = new XYLineAndShapeRenderer(false, true);
            this.svmTimeLineRenderer = new XYLineAndShapeRenderer(false, true);
            this.endpoints = new XYSeriesCollection();
            this.endpoints.setNotify(false);
            this.svmEndpoints = new XYSeriesCollection();
            this.svmEndpoints.setNotify(false);
            this.midpoints = new XYSeriesCollection();
            this.midpoints.setNotify(false);
            this.svmMidpoints = new XYSeriesCollection();
            this.svmMidpoints.setNotify(false);
            this.timeLine = new XYSeriesCollection();
            this.timeLine.setNotify(false);
            this.svmTimeLine = new XYSeriesCollection();
            this.svmTimeLine.setNotify(false);

            this.svmEndpointSeries = new XYSeries(0, false, true);
            this.svmEndpointSeries.setNotify(false);
            this.midpointSeries = new XYSeries(0, false, true);
            this.midpointSeries.setNotify(false);
            this.svmMidpointSeries = new XYSeries(0, false, true);
            this.svmMidpointSeries.setNotify(false);
            this.timeLineSeries = new XYSeries(0, false, true);
            this.timeLineSeries.setNotify(false);
            this.svmTimeLineSeries = new XYSeries(0, false, true);
            this.svmTimeLineSeries.setNotify(false);

            // renderer for bars
            this.barRenderer = new XYBarRenderer();
            this.bars = new XYIntervalSeriesCollection();
            this.bars.setNotify(false);

            // get chart and plot
            this.chart = ChartsAndPlots.createChart(graphLines, false);
            this.plot = ChartsAndPlots.createPlot(chart, UPPER_OR_LOWER);
        }


        /**
         * Updates graph with new data
         */
        protected void updateGraph()
        {
            // update lines
            updateDomainLines();
            updateOverlapLines();
            updateThresholdLine();

            chart.setNotify(false);

            // clear old lines, midpoints, endpoints, and timeline points
            graphLines.removeAllSeries();
            highlightLines.removeAllSeries();
            midpointSeries.clear();
            endpoints.removeAllSeries();
            highlightEndpoints.removeAllSeries();
            timeLineSeries.clear();

            // populate main series
            int lineCnt = -1;
            int highCnt = -1;

            for (DataObject data : DATA_OBJECTS)
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                    // ensure datapoint is within domain
                    // if drawing overlap, ensure datapoint is within overlap
                    if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                            (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                    {
                        int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                        // start line at (0, 0)
                        XYSeries line;
                        XYSeries endpointSeries;

                        if (DV.highlights[UPPER_OR_LOWER][i])
                        {
                            line = new XYSeries(++highCnt, false, true);
                            endpointSeries = new XYSeries(highCnt, false, true);
                        }
                        else
                        {
                            line = new XYSeries(++lineCnt, false, true);
                            endpointSeries = new XYSeries(lineCnt, false, true);
                        }

                        line.setNotify(false);
                        endpointSeries.setNotify(false);

                        if (DV.showFirstSeg)
                            line.add(0, upOrDown * buffer);

                        // add points to lines
                        for (int j = 0; j < data.coordinates[i].length; j++)
                        {
                            line.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                            if (DV.showMidpoints && (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1]))
                                midpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                            // add endpoint and timeline
                            if (j == data.coordinates[i].length - 1)
                            {
                                endpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));
                                timeLineSeries.add(data.coordinates[i][j][0], 0);

                                if (DV.highlights[UPPER_OR_LOWER][i])
                                {
                                    highlightLines.addSeries(line);
                                    highlightLineRenderer.setSeriesPaint(highCnt, DV.highlightColor);
                                    highlightLineRenderer.setSeriesStroke(highCnt, graphLineStroke);

                                    highlightEndpoints.addSeries(endpointSeries);
                                    highlightEndpointRenderer.setSeriesPaint(highCnt, DV.highlightColor);
                                    highlightEndpointRenderer.setSeriesShape(highCnt, endpointShape);
                                }
                                else
                                {
                                    graphLines.addSeries(line);
                                    // Use class-specific color for multiple classes in lower graph
                                    Color lineColor = (UPPER_OR_LOWER == 1 && DATA_OBJECTS.size() > 1) ? 
                                        DV.getClassColor(data.className) : DV.graphColors[UPPER_OR_LOWER];
                                    lineRenderer.setSeriesPaint(lineCnt, lineColor);
                                    lineRenderer.setSeriesStroke(lineCnt, graphLineStroke);

                                    endpoints.addSeries(endpointSeries);
                                    endpointRenderer.setSeriesShape(lineCnt, endpointShape);

                                    if (UPPER_OR_LOWER == 0 && DV.upperIsLower)
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint <= DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                                    }
                                    else if (UPPER_OR_LOWER == 0)
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint > DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                                    }
                                    else if(UPPER_OR_LOWER == 1 && DV.upperIsLower)
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint > DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                                    }
                                    else
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint <= DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            chart.setNotify(true);
        }


        /**
         * Updates endpoints of graph
         */
        protected void updateEndpoints()
        {
            chart.setNotify(false);

            endpoints.removeAllSeries();
            highlightEndpoints.removeAllSeries();

            // populate main series
            int lineCnt = -1;
            int highCnt = -1;

            for (DataObject data : DATA_OBJECTS)
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                    // ensure datapoint is within domain
                    // if drawing overlap, ensure datapoint is within overlap
                    if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                            (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                    {
                        int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;
                        XYSeries endpointSeries;

                        if (DV.highlights[UPPER_OR_LOWER][i])
                            endpointSeries = new XYSeries(++highCnt, false, true);
                        else
                            endpointSeries = new XYSeries(++lineCnt, false, true);

                        endpointSeries.setNotify(false);

                        // add endpoint
                        int j = data.coordinates[i].length - 1;
                        endpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                        if (DV.highlights[UPPER_OR_LOWER][i])
                        {
                            highlightEndpoints.addSeries(endpointSeries);
                            highlightEndpointRenderer.setSeriesPaint(highCnt, DV.highlightColor);
                            highlightEndpointRenderer.setSeriesShape(highCnt, endpointShape);
                        }
                        else
                        {
                            endpoints.addSeries(endpointSeries);

                            if (UPPER_OR_LOWER == 0 && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint <= DV.threshold)
                                    endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                else
                                    endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                            }
                            else if (UPPER_OR_LOWER == 0)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > DV.threshold)
                                    endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                else
                                    endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                            }
                            else if(UPPER_OR_LOWER == 1 && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > DV.threshold)
                                    endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                else
                                    endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                            }
                            else
                            {
                                // check if endpoint is correctly classified
                                if (endpoint <= DV.threshold)
                                    endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                else
                                    endpointRenderer.setSeriesPaint(lineCnt, Color.RED);
                            }

                            endpointRenderer.setSeriesShape(lineCnt, endpointShape);
                        }
                    }
                }
            }

            chart.setNotify(true);
        }


        /**
         * Updates Support Vector Machine data
         */
        protected void updateSVM()
        {
            chart.setNotify(false);

            // clear old lines, midpoints, endpoints, and timeline points
            svmGraphLines.removeAllSeries();
            svmMidpointSeries.clear();
            svmEndpoints.removeAllSeries();
            svmTimeLineSeries.clear();

            // update coordinates
            getCoordinates(new ArrayList<>(List.of(DV.supportVectors)));

            for (int i = 0, lineCnt = 0; i < DV.supportVectors.data.length; i++, lineCnt++)
            {
                int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                // start line at (0, 0)
                XYSeries line = new XYSeries(lineCnt, false, true);
                line.setNotify(false);

                if (DV.showFirstSeg)
                    line.add(0, upOrDown * buffer);

                double endpoint = DV.supportVectors.coordinates[i][DV.supportVectors.coordinates[i].length-1][0];

                // ensure datapoint is within domain
                if (!DV.domainActive || (endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]))
                {
                    for (int j = 0; j < DV.supportVectors.coordinates[i].length; j++)
                    {
                        line.add(DV.supportVectors.coordinates[i][j][0], upOrDown * (DV.supportVectors.coordinates[i][j][1] + buffer));

                        if (DV.showMidpoints && (j > 0 && j < DV.supportVectors.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1]))
                            svmMidpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * (DV.supportVectors.coordinates[i][j][1]+ buffer));

                        // add endpoint and timeline
                        if (j == DV.supportVectors.coordinates[i].length - 1)
                        {
                            svmEndpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * (DV.supportVectors.coordinates[i][j][1] + buffer));
                            svmTimeLineSeries.add(DV.supportVectors.coordinates[i][j][0], 0);
                        }
                    }

                    svmGraphLines.addSeries(line);
                    svmLineRenderer.setSeriesPaint(lineCnt, DV.svmLines);
                }
            }

            chart.setNotify(true);
        }


        /**
         * Updates Frequency Graph
         */
        protected void updateBars()
        {
            chart.setNotify(false);

            bars.removeAllSeries();

            // get maximum frequency for frequency graph
            double maxFrequency = getMaxFrequency();

            // get interval and bounds
            double interval = DV.fieldLength / 200.0;
            double maxBound = -DV.fieldLength;
            double minBound = -DV.fieldLength;

            // For multiple classes in lower graph, create separate bar series for each class
            if (UPPER_OR_LOWER == 1 && DATA_OBJECTS.size() > 1)
            {
                int seriesIndex = 0;
                for (DataObject dataObj : DATA_OBJECTS)
                {
                    // get bar lengths for this specific class
                    int[] barRanges = new int[400];
                    for (int i = 0; i < dataObj.data.length; i++)
                    {
                        int tmpTick = (int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                        barRanges[tmpTick]++;
                    }

                    // reset bounds for this class
                    maxBound = -DV.fieldLength;
                    minBound = -DV.fieldLength;

                    // add series for this class
                    for (int i = 0; i < 400; i++)
                    {
                        // get max bound
                        maxBound += interval;

                        XYIntervalSeries bar = new XYIntervalSeries(seriesIndex * 400 + i, false, true);
                        bar.setNotify(false);

                        // bar width = interval
                        // bar height = (total endpoints on bar) / (total endpoints)
                        // buffer = maximum bar height
                        bar.add(interval, minBound, maxBound, (-barRanges[i] / maxFrequency) * buffer, -buffer, 0);

                        bars.addSeries(bar);
                        barRenderer.setSeriesPaint(seriesIndex * 400 + i, DV.getClassColor(dataObj.className));

                        // set min bound to old max
                        minBound = maxBound;
                    }
                    seriesIndex++;
                }
            }
            else
            {
                // Original logic for single class or upper graph
                // get bar lengths
                int[] barRanges = new int[400];
                for (DataObject dataObj : DATA_OBJECTS)
                {
                    // translate endpoint to slider ticks
                    // increment bar which endpoint lands
                    for (int i = 0; i < dataObj.data.length; i++)
                    {
                        int tmpTick = (int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                        barRanges[tmpTick]++;
                    }
                }

                // add series to collection
                for (int i = 0; i < 400; i++)
                {
                    // get max bound
                    maxBound += interval;

                    XYIntervalSeries bar = new XYIntervalSeries(i, false, true);
                    bar.setNotify(false);

                    // bar width = interval
                    // bar height = (total endpoints on bar) / (total endpoints)
                    // buffer = maximum bar height
                    if (UPPER_OR_LOWER == 1)
                        bar.add(interval, minBound, maxBound, (-barRanges[i] / maxFrequency) * buffer, -buffer, 0);
                    else
                        bar.add(interval, minBound, maxBound, (barRanges[i] / maxFrequency) * buffer, 0, buffer);

                    bars.addSeries(bar);
                    barRenderer.setSeriesPaint(i, DV.graphColors[UPPER_OR_LOWER]);

                    // set min bound to old max
                    minBound = maxBound;
                }
            }

            chart.setNotify(true);
        }


        /**
         * Sets the scale of the graph
         * @param graphScale scale of the graph
         */
        protected void setGraphScale(double graphScale)
        {
            GRAPH_SCALE = graphScale;
            setLineHeight();
        }


        /**
         * Sets the line height of the graph
         */
        protected void setLineHeight()
        {
            // get line height
            lineHeight = UPPER_OR_LOWER == 1 ? GRAPH_SCALE * -DV.fieldLength : GRAPH_SCALE * DV.fieldLength;
        }


        /**
         * Updates domain lines
         */
        protected void updateDomainLines()
        {
            chart.setNotify(false);

            domainMinLine.clear();
            domainMaxLine.clear();

            // set overlap line
            domainMinLine.add(DV.domainArea[0], 0);
            domainMinLine.add(DV.domainArea[0], lineHeight);
            domainMaxLine.add(DV.domainArea[1], 0);
            domainMaxLine.add(DV.domainArea[1], lineHeight);

            chart.setNotify(true);
        }


        /**
         * Updates overlap lines
         */
        protected void updateOverlapLines()
        {
            chart.setNotify(false);

            overlapMinLine.clear();
            overlapMaxLine.clear();

            // set overlap lines
            overlapMinLine.add(DV.overlapArea[0], 0);
            overlapMinLine.add(DV.overlapArea[0], lineHeight);
            overlapMaxLine.add(DV.overlapArea[1], 0);
            overlapMaxLine.add(DV.overlapArea[1], lineHeight);

            chart.setNotify(true);
        }


        /**
         * Updates threshold line
         */
        protected void updateThresholdLine()
        {
            chart.setNotify(false);

            thresholdLine.clear();

            // set threshold line
            thresholdLine.add(DV.threshold, 0);
            thresholdLine.add(DV.threshold, lineHeight);

            chart.setNotify(true);
        }


        /**
         * Sets the renderer and dataset for the graph
         */
        protected void setDatasetsAndRenderers()
        {
            chart.setNotify(false);

            // set svm endpoint renderer and dataset
            svmEndpointRenderer.setSeriesShape(0, endpointShape);
            svmEndpointRenderer.setSeriesPaint(0, DV.svmEndpoints);
            plot.setRenderer(0, svmEndpointRenderer);
            plot.setDataset(0, svmEndpoints);

            // set highlight endpoint renderer and dataset
            plot.setRenderer(1, highlightEndpointRenderer);
            plot.setDataset(1, highlightEndpoints);

            // set endpoint renderer and dataset
            plot.setRenderer(2, endpointRenderer);
            plot.setDataset(2, endpoints);

            // set mvm midpoint renderer and dataset
            svmMidpointRenderer.setSeriesShape(0, endpointShape);
            svmMidpointRenderer.setSeriesPaint(0, DV.svmEndpoints);
            plot.setRenderer(3, svmMidpointRenderer);
            plot.setDataset(3, svmMidpoints);

            // set midpoint renderer and dataset
            midpointRenderer.setSeriesShape(0, midpointShape);
            midpointRenderer.setSeriesPaint(0, DV.midpoints);
            plot.setRenderer(4, midpointRenderer);
            plot.setDataset(4, midpoints);

            // set threshold renderer and dataset
            thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
            thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
            plot.setRenderer(5, thresholdRenderer);
            plot.setDataset(5, threshold);

            // set overlap renderer and dataset
            overlapRenderer.setSeriesStroke(0, thresholdOverlapStroke);
            overlapRenderer.setSeriesStroke(1, thresholdOverlapStroke);
            overlapRenderer.setSeriesPaint(0, DV.overlapLines);
            overlapRenderer.setSeriesPaint(1, DV.overlapLines);
            plot.setRenderer(6, overlapRenderer);
            plot.setDataset(6, overlap);

            if (DV.domainActive)
            {
                // set domain renderer and dataset
                domainRenderer.setSeriesStroke(0, domainStroke);
                domainRenderer.setSeriesStroke(1, domainStroke);
                domainRenderer.setSeriesPaint(0, DV.domainLines);
                domainRenderer.setSeriesPaint(1, DV.domainLines);
                plot.setRenderer(7, domainRenderer);
                plot.setDataset(7, domain);
            }

            // set svm line renderer and dataset
            svmLineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(8, svmLineRenderer);
            plot.setDataset(8, svmGraphLines);

            // set highlight line renderer and dataset
            highlightLineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(9, highlightLineRenderer);
            plot.setDataset(9, highlightLines);

            // set line renderer and dataset
            lineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(10, lineRenderer);
            plot.setDataset(10, graphLines);

            // set bar or timeline renderer and dataset
            if (DV.showBars)
            {
                barRenderer.setSeriesPaint(0, DV.graphColors[UPPER_OR_LOWER]);
                barRenderer.setShadowVisible(false);
                plot.setRenderer(11, barRenderer);
                plot.setDataset(11, bars);
            }
            else
            {
                svmTimeLineRenderer.setSeriesPaint(0, DV.svmLines);
                plot.setRenderer(11, svmTimeLineRenderer);
                plot.setDataset(11, svmTimeLine);

                if (UPPER_OR_LOWER == 1)
                    timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
                else
                    timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, -3, 0.5, 3));

                timeLineRenderer.setSeriesPaint(0, DV.graphColors[UPPER_OR_LOWER]);
                plot.setRenderer(12, timeLineRenderer);
                plot.setDataset(12, timeLine);
            }

            chart.setNotify(true);
        }


        /**
         * Sets the domain and range of the graph
         */
        protected void setDomainAndRange()
        {
            // set domain and range of graph
            double bound = GRAPH_SCALE * DV.fieldLength;

            // set domain
            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range
            ValueAxis rangeView = plot.getRangeAxis();
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range up or down
            if (UPPER_OR_LOWER == 1)
                rangeView.setRange(-bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8), 0);
            else
                rangeView.setRange(0, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));
        }


        /**
         * Draws the graph
         * @return whether the graph was drawn or not
         */
        @Override
        protected Boolean doInBackground()
        {
            // add series collection
            domain.addSeries(domainMaxLine);
            domain.addSeries(domainMinLine);
            overlap.addSeries(overlapMaxLine);
            overlap.addSeries(overlapMinLine);
            threshold.addSeries(thresholdLine);

            // add data to series
            svmEndpoints.addSeries(svmEndpointSeries);
            midpoints.addSeries(midpointSeries);
            svmMidpoints.addSeries(svmMidpointSeries);
            timeLine.addSeries(timeLineSeries);
            svmTimeLine.addSeries(svmTimeLineSeries);

            // populate svm series
            if (DV.drawOnlySVM || DV.drawSVM)
                updateSVM();

            if (!DV.drawOnlySVM)
                updateGraph();

            // create bar chart
            if (DV.showBars)
                updateBars();

            // set domain and range of graph
            setDomainAndRange();
            setDatasetsAndRenderers();

            // add domain listeners
            DV.domainSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    DV.domainArea[0] = (slider.getValue() - 200) * DV.fieldLength / 200.0;
                    DV.domainArea[1] = (slider.getUpperValue() - 200) * DV.fieldLength / 200.0;

                    // update graph
                    updateGraph();

                    // generate analytics
                    Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                    analytics.execute();

                    // wait for generation
                    try
                    {
                        analytics.get();
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.domainSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    domainRenderer.setSeriesStroke(0, domainActiveStroke);
                    domainRenderer.setSeriesStroke(1, domainActiveStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    domainRenderer.setSeriesStroke(0, domainStroke);
                    domainRenderer.setSeriesStroke(1, domainStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add overlap listeners
            DV.overlapSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    DV.overlapArea[0] = (slider.getValue() - 200) * DV.fieldLength / 200.0;
                    DV.overlapArea[1] = (slider.getUpperValue() - 200) * DV.fieldLength / 200.0;

                    // draw lines as active (thicker)
                    overlapRenderer.setSeriesStroke(0, thresholdOverlapActiveStroke);
                    overlapRenderer.setSeriesStroke(1, thresholdOverlapActiveStroke);

                    // set graph
                    updateOverlapLines();
                    updateEndpoints();

                    // generate analytics
                    if (DV.withoutOverlapChecked || DV.worstCaseChecked)
                        getAnalytics();
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.overlapSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    overlapRenderer.setSeriesStroke(0, thresholdOverlapActiveStroke);
                    overlapRenderer.setSeriesStroke(1, thresholdOverlapActiveStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    overlapRenderer.setSeriesStroke(0, thresholdOverlapStroke);
                    overlapRenderer.setSeriesStroke(1, thresholdOverlapStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add threshold listeners
            DV.thresholdSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    // get position
                    JSlider slider = (JSlider) e.getSource();
                    DV.threshold = (slider.getValue() - 200) * DV.fieldLength / 200.0;

                    // draw line as active (thicker)
                    thresholdRenderer.setSeriesStroke(0, thresholdOverlapActiveStroke);

                    // set threshold line
                    updateThresholdLine();
                    updateEndpoints();

                    // generate analytics
                    getAnalytics();
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.thresholdSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    thresholdRenderer.setSeriesStroke(0, thresholdOverlapActiveStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add graph to graph panel
            synchronized (GRAPHS)
            {
                GRAPHS.put(UPPER_OR_LOWER, chart);
            }

           if (DV.displayRemoteGraphs)
            {
                try
                {
                    // create the graph panel and add it to the main panel
                    ChartPanel chartPanel2 = new ChartPanel((JFreeChart) chart.clone());
                    chartPanel2.setMouseWheelEnabled(true);
                    chartPanel2.setPreferredSize(new Dimension(Resolutions.singleChartPanel[0], Resolutions.singleChartPanel[1] * (DV.classNumber == 1 ? 2 : 1)));

                    // show datapoint when clicked
                    chartPanel2.addChartMouseListener(new ChartMouseListener()
                    {
                        // display point on click
                        @Override
                        public void chartMouseClicked(ChartMouseEvent e)
                        {
                            // get clicked entity
                            ChartEntity ce = e.getEntity();

                            if (ce instanceof XYItemEntity xy)
                            {
                                // ensure entity is endpoint
                                if (xy.getDataset().getSeriesCount() > 1)
                                    datapointInfoPopup(xy);
                            }
                        }

                        @Override
                        public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
                    });

                    // add graph to graph panel
                    synchronized (REMOTE_GRAPHS)
                    {
                        REMOTE_GRAPHS.put(UPPER_OR_LOWER, chartPanel2);
                    }
                }
                catch(CloneNotSupportedException e)
                {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
            }

            return true;
        }
    }


    /**
     * Draws regression graphs
     */
    public static void drawRegGraph()
    {
        GRAPHS.clear();
        DV.graphPanel.removeAll();

        // holds classes to be graphed
        ArrayList<DataObject> regObject = new ArrayList<>(List.of(DV.trainData.get(0)));

        // calculate coordinates
        // scale graph according to largest value
        double graphScale = getCoordinates(regObject);

        // generate graphs
        regGraph = new RegGraph(regObject, 0, graphScale);
        regGraph.execute();

        // wait for threads to finish
        try
        {
            regGraph.get();
            generateRegGraph(graphScale);
        }
        catch (ExecutionException | InterruptedException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }


    /**
     * Generates graphs for visualization
     * @param graphScale highest endpoint frequency on the x coordinate
     */
    private static void generateRegGraph(double graphScale)
    {
        GridBagConstraints gpc = new GridBagConstraints();
        gpc.gridx = 0;
        gpc.gridy = 0;
        gpc.weightx = 1;
        gpc.weighty = 1;
        gpc.fill = GridBagConstraints.BOTH;

        // add graph
        JFreeChart chart = new JFreeChart("", null, GRAPHS.get(0).getPlot(), false);

        // add to DV panel
        DV.graphPanel.add(createChartPanel(chart), gpc);

        // revalidate graphs and confusion matrices
        DV.graphPanel.repaint();
        DV.graphPanel.revalidate();

        // warn user if graphs are scaled
        if (!showPopup && graphScale > 1)
        {
            DV.informationPopup("Zoom Warning",
                    """
                    Because of the size, the graphs have been zoomed out.
                    All functionality remains, but there will be empty space on each side of the graph.
                    Zoom in or scale the graphs to remove the white space.
                    """);
            showPopup = false;
        }
    }


    /**
     * Draw graph with specified parameters
     */
    private static class RegGraph extends SwingWorker<Boolean, Void>
    {
        final ArrayList<DataObject> DATA_OBJECTS;
        final int UPPER_OR_LOWER;
        double GRAPH_SCALE;
        final double buffer;
        double lineHeight;

        // accuracy info
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        // create main renderer and dataset
        final XYLineAndShapeRenderer lineRenderer;
        final XYSeriesCollection graphLines;

        final XYLineAndShapeRenderer highlightLineRenderer;
        final XYSeriesCollection highlightLines;
        final XYLineAndShapeRenderer highlightEndpointRenderer;
        final XYSeriesCollection highlightEndpoints;

        // create renderer for domain, overlap, and threshold lines
        final XYLineAndShapeRenderer domainRenderer;
        final XYSeriesCollection domain;
        final XYSeries domainMaxLine;
        final XYSeries domainMinLine;

        // renderer for endpoint, midpoint, and timeline
        final XYLineAndShapeRenderer endpointRenderer;
        final XYLineAndShapeRenderer midpointRenderer;
        final XYLineAndShapeRenderer timeLineRenderer;
        final XYLineAndShapeRenderer errorLineRenderer;
        final XYSeriesCollection endpoints;
        final XYSeriesCollection midpoints;
        final XYSeriesCollection timeLine;
        final XYSeriesCollection errorLine;
        final XYSeries midpointSeries;
        final XYSeries timeLineSeries;
        final XYSeries errorLineSeries;

        // renderer for bars
        final XYIntervalSeriesCollection bars;
        final XYBarRenderer barRenderer;

        // stroke and shapes
        final BasicStroke graphLineStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        final BasicStroke domainStroke = new BasicStroke(2f);
        final BasicStroke domainActiveStroke = new BasicStroke(4f);
        final Ellipse2D.Double endpointShape = new Ellipse2D.Double(-1, -1, 2, 2);
        final Ellipse2D.Double midpointShape = new Ellipse2D.Double(-0.5, -0.5, 1, 1);

        // get chart and plot
        JFreeChart chart;
        XYPlot plot;

        /**
         * Initializes parameters
         * @param dataObjects classes to draw
         * @param upperOrLower draw up when upper (0) and down when lower (1)
         * @param graphScale how much to zoom out the graphs
         */
        RegGraph(ArrayList<DataObject> dataObjects, int upperOrLower, double graphScale)
        {
            this.DATA_OBJECTS = dataObjects;
            this.UPPER_OR_LOWER = upperOrLower;
            this.GRAPH_SCALE = graphScale;
            this.buffer = DV.fieldLength / 10.0;

            // create main renderer and dataset
            this.lineRenderer = new XYLineAndShapeRenderer(true, false);
            this.graphLines = new XYSeriesCollection();
            this.graphLines.setNotify(false);

            this.highlightLineRenderer = new XYLineAndShapeRenderer(true, false);
            this.highlightLines = new XYSeriesCollection();
            this.highlightLines.setNotify(false);
            this.highlightEndpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.highlightEndpoints = new XYSeriesCollection();
            this.highlightEndpoints.setNotify(false);

            // create renderer for domain, overlap, and threshold lines
            this.domainRenderer = new XYLineAndShapeRenderer(true, false);
            this.domain = new XYSeriesCollection();
            this.domain.setNotify(false);
            this.domainMaxLine = new XYSeries(-1, false, true);
            this.domainMaxLine.setNotify(false);
            this.domainMinLine = new XYSeries(-2, false, true);
            this.domainMinLine.setNotify(false);

            // renderer for endpoint, midpoint, and timeline
            this.endpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.midpointRenderer = new XYLineAndShapeRenderer(false, true);
            this.timeLineRenderer = new XYLineAndShapeRenderer(false, true);
            this.errorLineRenderer = new XYLineAndShapeRenderer(false, true);
            this.endpoints = new XYSeriesCollection();
            this.endpoints.setNotify(false);
            this.midpoints = new XYSeriesCollection();
            this.midpoints.setNotify(false);
            this.timeLine = new XYSeriesCollection();
            this.timeLine.setNotify(false);
            this.errorLine = new XYSeriesCollection();
            this.errorLine.setNotify(false);

            this.midpointSeries = new XYSeries(0, false, true);
            this.midpointSeries.setNotify(false);
            this.timeLineSeries = new XYSeries(0, false, true);
            this.timeLineSeries.setNotify(false);
            this.errorLineSeries = new XYSeries(0, false, true);
            this.errorLineSeries.setNotify(false);

            // renderer for bars
            this.barRenderer = new XYBarRenderer();
            this.bars = new XYIntervalSeriesCollection();
            this.bars.setNotify(false);

            // get chart and plot
            this.chart = ChartsAndPlots.createChart(graphLines, false);
            this.plot = ChartsAndPlots.createPlot(chart, UPPER_OR_LOWER);
        }


        /**
         * Updates the graph
         */
        protected void updateGraph()
        {
            // update lines
            updateDomainLines();

            chart.setNotify(false);

            // clear old lines, midpoints, endpoints, and timeline points
            graphLines.removeAllSeries();
            highlightLines.removeAllSeries();
            midpointSeries.clear();
            endpoints.removeAllSeries();
            highlightEndpoints.removeAllSeries();
            timeLineSeries.clear();
            errorLineSeries.clear();

            // populate main series
            int lineCnt = -1;
            int highCnt = -1;

            for (DataObject data : DATA_OBJECTS)
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                    // ensure datapoint is within domain
                    // if drawing overlap, ensure datapoint is within overlap
                    if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                            (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                    {
                        // start line at (0, 0)
                        XYSeries line;
                        XYSeries endpointSeries;

                        if (DV.highlights[UPPER_OR_LOWER][i])
                        {
                            line = new XYSeries(++highCnt, false, true);
                            endpointSeries = new XYSeries(highCnt, false, true);
                        }
                        else
                        {
                            line = new XYSeries(++lineCnt, false, true);
                            endpointSeries = new XYSeries(lineCnt, false, true);
                        }

                        line.setNotify(false);
                        endpointSeries.setNotify(false);

                        if (DV.showFirstSeg)
                            line.add(0, buffer);

                        // add points to lines
                        for (int j = 0; j < data.coordinates[i].length; j++)
                        {
                            line.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);

                            if (DV.showMidpoints && (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1]))
                                midpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);

                            // add endpoint and timeline
                            if (j == data.coordinates[i].length - 1)
                            {
                                endpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);
                                //timeLineSeries.add(data.coordinates[i][j][0], 0);
                                errorLineSeries.add(data.coordinates[i][j][0], -(DV.reg_true_values.get(i) + buffer));

                                if (DV.highlights[UPPER_OR_LOWER][i])
                                {
                                    highlightLines.addSeries(line);
                                    highlightLineRenderer.setSeriesPaint(highCnt, DV.highlightColor);
                                    highlightLineRenderer.setSeriesStroke(highCnt, graphLineStroke);

                                    highlightEndpoints.addSeries(endpointSeries);
                                    highlightEndpointRenderer.setSeriesPaint(highCnt, DV.highlightColor);
                                    highlightEndpointRenderer.setSeriesShape(highCnt, endpointShape);
                                }
                                else
                                {
                                    graphLines.addSeries(line);
                                    lineRenderer.setSeriesPaint(lineCnt, Color.BLUE);
                                    lineRenderer.setSeriesStroke(lineCnt, graphLineStroke);

                                    endpoints.addSeries(endpointSeries);
                                    endpointRenderer.setSeriesShape(lineCnt, endpointShape);
                                    endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                }
                            }
                        }
                    }
                }
            }

            double minTrue = Double.MAX_VALUE;
            double maxTrue = -Double.MAX_VALUE;

            for (int i = 0; i < DV.reg_true_values.size(); i++)
            {
                // get min and max values
                if (DV.reg_true_values.get(i) < minTrue)
                    minTrue = DV.reg_true_values.get(i);
                else if (DV.reg_true_values.get(i) > maxTrue)
                    maxTrue = DV.reg_true_values.get(i);
            }

            double zed = ((DV.reg_RMSE / DV.reg_avg_val) / 2) * (maxTrue - minTrue);

            XYSeries regLine = new XYSeries(++lineCnt, false, true);
            regLine.add(minTrue, -(minTrue + buffer));
            regLine.add(maxTrue, -(maxTrue + buffer));

            graphLines.addSeries(regLine);
            lineRenderer.setSeriesPaint(lineCnt, Color.BLUE);
            lineRenderer.setSeriesStroke(lineCnt, graphLineStroke);

            XYSeries regLine1 = new XYSeries(++lineCnt, false, true);
            regLine1.add(minTrue, (-(minTrue + buffer)) - zed);
            regLine1.add(maxTrue, (-(maxTrue + buffer)) - zed);

            graphLines.addSeries(regLine1);
            lineRenderer.setSeriesPaint(lineCnt, Color.MAGENTA);
            lineRenderer.setSeriesStroke(lineCnt, graphLineStroke);

            XYSeries regLine2 = new XYSeries(++lineCnt, false, true);
            regLine2.add(minTrue, (-(minTrue + buffer)) + zed);
            regLine2.add(maxTrue, (-(maxTrue + buffer)) + zed);

            graphLines.addSeries(regLine2);
            lineRenderer.setSeriesPaint(lineCnt, Color.MAGENTA);
            lineRenderer.setSeriesStroke(lineCnt, graphLineStroke);

            chart.setNotify(true);
        }


        /**
         * Updates Frequency Graph
         */
        protected void updateBars()
        {
            chart.setNotify(false);

            bars.removeAllSeries();

            // get maximum frequency for frequency graph
            double maxFrequency = getMaxFrequency();

            // get bar lengths
            int[] barRanges = new int[400];
            for (DataObject dataObj : DATA_OBJECTS)
            {
                // translate endpoint to slider ticks
                // increment bar which endpoint lands
                for (int i = 0; i < dataObj.data.length; i++)
                {
                    int tmpTick = (int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                    barRanges[tmpTick]++;
                }
            }

            // get interval and bounds
            double interval = DV.fieldLength / 200.0;
            double maxBound = -DV.fieldLength;
            double minBound = -DV.fieldLength;

            // add series to collection
            for (int i = 0; i < 400; i++)
            {
                // get max bound
                maxBound += interval;

                XYIntervalSeries bar = new XYIntervalSeries(i, false, true);
                bar.setNotify(false);

                // bar width = interval
                // bar height = (total endpoints on bar) / (total endpoints)
                // buffer = maximum bar height
                if (UPPER_OR_LOWER == 1)
                    bar.add(interval, minBound, maxBound, (-barRanges[i] / maxFrequency) * buffer, -buffer, 0);
                else
                    bar.add(interval, minBound, maxBound, (barRanges[i] / maxFrequency) * buffer, 0, buffer);

                bars.addSeries(bar);
                barRenderer.setSeriesPaint(i, Color.BLUE);

                // set min bound to old max
                minBound = maxBound;
            }

            chart.setNotify(true);
        }


        /**
         * Sets the scale of the graph
         */
        protected void setLineHeight()
        {
            // get line height
            lineHeight = GRAPH_SCALE * DV.fieldLength + max;

        }


        /**
         * Updates domain lines
         */
        protected void updateDomainLines()
        {
            chart.setNotify(false);

            domainMinLine.clear();
            domainMaxLine.clear();

            // set overlap line
            domainMinLine.add(DV.domainArea[0], 0 - max);
            domainMinLine.add(DV.domainArea[0], lineHeight);
            domainMaxLine.add(DV.domainArea[1], 0 - max);
            domainMaxLine.add(DV.domainArea[1], lineHeight);

            chart.setNotify(true);
        }


        /**
         * Sets the renderer and dataset for the graph
         */
        protected void setDatasetsAndRenderers()
        {
            chart.setNotify(false);

            // set highlight endpoint renderer and dataset
            plot.setRenderer(0, highlightEndpointRenderer);
            plot.setDataset(0, highlightEndpoints);

            // set endpoint renderer and dataset
            plot.setRenderer(1, endpointRenderer);
            plot.setDataset(1, endpoints);

            // set midpoint renderer and dataset
            midpointRenderer.setSeriesShape(0, midpointShape);
            midpointRenderer.setSeriesPaint(0, DV.midpoints);
            plot.setRenderer(2, midpointRenderer);
            plot.setDataset(2, midpoints);

            if (DV.domainActive)
            {
                // set domain renderer and dataset
                domainRenderer.setSeriesStroke(0, domainStroke);
                domainRenderer.setSeriesStroke(1, domainStroke);
                domainRenderer.setSeriesPaint(0, DV.domainLines);
                domainRenderer.setSeriesPaint(1, DV.domainLines);
                plot.setRenderer(3, domainRenderer);
                plot.setDataset(3, domain);
            }

            // set highlight line renderer and dataset
            highlightLineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(4, highlightLineRenderer);
            plot.setDataset(4, highlightLines);

            // set line renderer and dataset
            lineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(5, lineRenderer);
            plot.setDataset(5, graphLines);

            // set bar or timeline renderer and dataset
            if (DV.showBars)
            {
                barRenderer.setSeriesPaint(0, Color.BLUE);
                barRenderer.setShadowVisible(false);
                plot.setRenderer(6, barRenderer);
                plot.setDataset(6, bars);
            }
            else
            {
                timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
                timeLineRenderer.setSeriesPaint(0, Color.RED);
                plot.setRenderer(7, timeLineRenderer);
                plot.setDataset(7, timeLine);
            }

            errorLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
            errorLineRenderer.setSeriesPaint(0, Color.GREEN);
            plot.setRenderer(8, errorLineRenderer);
            plot.setDataset(8, errorLine);

            chart.setNotify(true);
        }


        /**
         * Sets the domain and range of the graph
         */
        protected void setDomainAndRange()
        {
            // set domain and range of graph
            double bound = GRAPH_SCALE * DV.fieldLength;

            // set domain
            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range
            ValueAxis rangeView = plot.getRangeAxis();
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range
            rangeView.setRange(0 - max, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));
        }


        /**
         * Gets the minimum and maximum values differences between the predicted and true values
         */
        protected void getMinMax()
        {
            for (int i = 0; i < DV.reg_predictions.size(); i++)
            {
                double val = Math.abs(DV.reg_predictions.get(i) - DV.reg_true_values.get(i));

                if (val < min)
                    min = val;
                if (val > max)
                    max = val;
            }
        }


        /**
         * Draws the graph
         * @return whether the graph was drawn or not
         */
        @Override
        protected Boolean doInBackground()
        {
            // establish min and max to normalize error values
            getMinMax();
            setLineHeight();

            // add series collection
            domain.addSeries(domainMaxLine);
            domain.addSeries(domainMinLine);

            // add data to series
            midpoints.addSeries(midpointSeries);
            timeLine.addSeries(timeLineSeries);
            errorLine.addSeries(errorLineSeries);

            updateGraph();

            // create bar chart
            if (DV.showBars)
                updateBars();

            // set domain and range of graph
            setDomainAndRange();
            setDatasetsAndRenderers();

            // add domain listeners
            DV.domainSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    DV.domainArea[0] = (slider.getValue() - 200) * DV.fieldLength / 200.0;
                    DV.domainArea[1] = (slider.getUpperValue() - 200) * DV.fieldLength / 200.0;

                    // update graph
                    updateGraph();

                    // generate analytics
                    Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                    analytics.execute();

                    // wait for generation
                    try
                    {
                        analytics.get();
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        LOGGER.log(Level.SEVERE, ex.toString(), ex);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.domainSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    domainRenderer.setSeriesStroke(0, domainActiveStroke);
                    domainRenderer.setSeriesStroke(1, domainActiveStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    domainRenderer.setSeriesStroke(0, domainStroke);
                    domainRenderer.setSeriesStroke(1, domainStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add graph to graph panel
            synchronized (GRAPHS)
            {
                GRAPHS.put(UPPER_OR_LOWER, chart);
            }

            if (DV.displayRemoteGraphs)
            {
                try
                {
                    // create the graph panel and add it to the main panel
                    ChartPanel chartPanel2 = new ChartPanel((JFreeChart) chart.clone());
                    chartPanel2.setMouseWheelEnabled(true);
                    chartPanel2.setPreferredSize(new Dimension(Resolutions.singleChartPanel[0], Resolutions.singleChartPanel[1] * (DV.classNumber == 1 ? 2 : 1)));

                    // show datapoint when clicked
                    chartPanel2.addChartMouseListener(new ChartMouseListener()
                    {
                        // display point on click
                        @Override
                        public void chartMouseClicked(ChartMouseEvent e)
                        {
                            // get clicked entity
                            ChartEntity ce = e.getEntity();

                            if (ce instanceof XYItemEntity xy)
                            {
                                // ensure entity is endpoint
                                if (xy.getDataset().getSeriesCount() > 1)
                                    datapointInfoPopup(xy);
                            }
                        }

                        @Override
                        public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
                    });

                    // add graph to graph panel
                    synchronized (REMOTE_GRAPHS)
                    {
                        REMOTE_GRAPHS.put(UPPER_OR_LOWER, chartPanel2);
                    }
                }
                catch(CloneNotSupportedException e)
                {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
            }

            return true;
        }
    }
}
