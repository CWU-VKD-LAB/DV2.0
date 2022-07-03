import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ConfusionMatrices
{
    // holds the percentage of overlap points used
    static String percentageOverlapPointsUsed;

    // holds overlapping datapoints
    static ArrayList<double[]> upper;
    static ArrayList<double[]> lower;

    // holds angles and threshold for LDA
    static ArrayList<Double> LDAFunction = new ArrayList<>();

    // holds current classes
    static ArrayList<String> curClasses;

    // holds confusion matrices in order
    static Map<Integer, JTextArea> confusionMatrices;


    /**
     * Generates all data, data without overlap,
     * overlap, and worst case confusion matrices
     */
    public static void generateConfusionMatrices()
    {
        // holds confusion matrices
        confusionMatrices = new HashMap<>();

        // get current classes being visualized
        getCurClasses();

        // remove old analytics
        DV.confusionMatrixPanel.removeAll();
        DV.crossValidationPanel.removeAll();

        // add confusion matrices from previous splits
        AddOldConfusionMatrices oldCM = new AddOldConfusionMatrices();

        if (DV.prevAllDataChecked)
            oldCM.execute();

        // create all data confusion matrix
        GetAllDataConfusionMatrix allCM = new GetAllDataConfusionMatrix();

        if (DV.allDataChecked)
            allCM.execute();

        // create data without overlap confusion matrix
        GetDataWithoutOverlapConfusionMatrix withoutCM = new GetDataWithoutOverlapConfusionMatrix();

        if (DV.withoutOverlapChecked || DV.worstCaseChecked)
            withoutCM.execute();

        // create overlap confusion matrix
        GetOverlapConfusionMatrix overlapCM = new GetOverlapConfusionMatrix();

        if (DV.overlapChecked || DV.worstCaseChecked)
            overlapCM.execute();

        // create worst case confusion matrix
        GetWorstCaseConfusionMatrix worstCM = new GetWorstCaseConfusionMatrix();

        try
        {
            if (DV.worstCaseChecked && withoutCM.get() && overlapCM.get())
                worstCM.execute();
        }
        catch (ExecutionException | InterruptedException e)
        {
            e.printStackTrace();
            return;
        }

        // create user validation confusion matrix
        GetUserValidationConfusionMatrix userCM = new GetUserValidationConfusionMatrix();

        if (DV.userValidationImported && DV.userValidationChecked)
            userCM.execute();

        // run k-fold cross validation
        if (DV.crossValidationChecked)
            runKFoldCrossValidation();

        // wait for threads to finish
        try
        {
            if (DV.prevAllDataChecked) oldCM.get();
            if (DV.allDataChecked) allCM.get();
            if (DV.withoutOverlapChecked) withoutCM.get();
            if (DV.overlapChecked) overlapCM.get();
            if (DV.worstCaseChecked) worstCM.get();
            if (DV.userValidationImported && DV.userValidationChecked) userCM.get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            e.printStackTrace();
            return;
        }

        // add confusion matrices in order
        for (int i = 0; i < DV.prevCM.size() + 6; i++)
        {
            if (confusionMatrices.containsKey(i))
            {
                DV.confusionMatrixPanel.add(confusionMatrices.get(i));
            }
        }
    }


    /**
     * Gets current classes being used
     */
    private static void getCurClasses()
    {
        curClasses = new ArrayList<>();

        // add upper class
        curClasses.add(String.format("%d", DV.upperClass));

        StringBuilder lowerClasses = new StringBuilder();

        // add lower classes
        for (int i = 0; i < DV.classNumber; i++)
        {
            if (DV.lowerClasses.get(i))
            {
                if (!lowerClasses.isEmpty())
                    lowerClasses.append(",");

                lowerClasses.append(i);
            }
        }

        curClasses.add(lowerClasses.toString());
    }


    /**
     * Regenerates old confusion matrices from previous data splits
     */
    private static class AddOldConfusionMatrices extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // set all previous confusion matrices
            for (int i = 0; i < DV.prevCM.size(); i++)
            {
                JTextArea confusionMatrix = new JTextArea(DV.prevCM.get(i));
                confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                confusionMatrix.setEditable(false);
                confusionMatrices.put(i, confusionMatrix);
            }

            return true;
        }
    }


    /**
     * Generates confusion matrix with all data
     * Confusion matrix uses its own function
     */
    private static class GetAllDataConfusionMatrix extends SwingWorker<Boolean, Void>
    {

        @Override
        protected Boolean doInBackground()
        {
            DV.allDataClassifications = new int[]{ 0, 0 };
            int totalPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            for (int i = 0; i < DV.data.size(); i++)
            {
                totalPoints += DV.data.get(i).data.length;

                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // check if endpoint is within the subset of used data
                        if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                        {
                            // get classification
                            if (i == DV.upperClass && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < DV.threshold)
                                {
                                    pntDist[0][0]++;
                                    DV.allDataClassifications[1]++;
                                    DV.allDataClassifications[0]++;
                                }
                                else
                                {
                                    DV.allDataClassifications[1]++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if (i == DV.upperClass)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > DV.threshold)
                                {
                                    pntDist[0][0]++;
                                    DV.allDataClassifications[1]++;
                                    DV.allDataClassifications[0]++;
                                }
                                else
                                {
                                    DV.allDataClassifications[1]++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > DV.threshold)
                                {
                                    pntDist[1][1]++;
                                    DV.allDataClassifications[1]++;
                                    DV.allDataClassifications[0]++;
                                }
                                else
                                {
                                    DV.allDataClassifications[1]++;
                                    pntDist[1][0]++;
                                }
                            }
                            else
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < DV.threshold)
                                {
                                    pntDist[1][1]++;
                                    DV.allDataClassifications[1]++;
                                    DV.allDataClassifications[0]++;
                                }
                                else
                                {
                                    DV.allDataClassifications[1]++;
                                    pntDist[1][0]++;
                                }
                            }
                        }
                    }
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("All Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * DV.allDataClassifications[0] / DV.allDataClassifications[1]));

            // add overall accuracy if applicable
            if (DV.prevAllDataClassifications.size() > 0)
            {
                int correct = DV.allDataClassifications[0];
                int used = DV.allDataClassifications[1];

                for (int i = 0; i < DV.prevAllDataClassifications.size(); i++)
                {
                    correct += DV.prevAllDataClassifications.get(i)[0];
                    used += DV.prevAllDataClassifications.get(i)[1];
                }

                cm.append(String.format("\nOverall Accuracy: %.2f%%", 100.0 * correct  / used));
            }

            // append percentage of total points used
            cm.append(String.format("\nData Used: %.2f%%", 100.0 * DV.allDataClassifications[1] / totalPoints));

            // set current all data confusion matrix string
            DV.allDataCM = cm.toString();

            // set all data confusion matrix
            JTextArea confusionMatrix = new JTextArea(DV.allDataCM);
            confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix.setEditable(false);
            confusionMatrices.put(DV.prevCM.size(), confusionMatrix);

            return true;
        }
    }


    /**
     * Generates confusion matrix without overlap data
     * Confusion matrix uses its own function
     */
    private static class GetDataWithoutOverlapConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // store overlapping datapoints in upper and lower graphs
            ArrayList<double[]> upper = new ArrayList<>();
            ArrayList<double[]> lower = new ArrayList<>();

            // total datapoints within subset of utilized data
            int totalPoints = 0;
            int totalPointsUsed = 0;

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass)
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is outside of overlap then store point
                        if ((DV.overlapArea[0] >= endpoint || endpoint >= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            upper.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
                else if (DV.lowerClasses.get(i))
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is outside of overlap then store point
                        if ((DV.overlapArea[0] >= endpoint || endpoint >= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            lower.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
            }

            String fileName = System.getProperty("user.dir") + "\\src\\Python\\DWO_CM.csv";

            // create file for python process
            createCSVFileForConfusionMatrix(new ArrayList<>(List.of(upper, lower)), fileName);

            // get confusion matrix with LDA
            ArrayList<String> cmValues = LDAForConfusionMatrices(true, fileName);

            if (DV.withoutOverlapChecked)
            {
                if (cmValues != null)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Data Without Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 0; i < 2; i++)
                        cm.append(curClasses.get(i)).append("\t");

                    for (int i = 0, index = -1; i < 2; i++)
                    {
                        // append class label
                        cm.append("\n").append(curClasses.get(i)).append("\t");

                        // append classifications
                        cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
                    }

                    // append accuracy
                    cm.append("\n").append(cmValues.get(cmValues.size() - 1));

                    // append percentage of total points used
                    cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

                    // set overlap confusion matrix
                    JTextArea confusionMatrix = new JTextArea(cm.toString());
                    confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                    confusionMatrix.setEditable(false);
                    confusionMatrices.put(DV.prevCM.size() + 1, confusionMatrix);
                }
            }

            return true;
        }
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses its own function
     */
    private static class GetOverlapConfusionMatrix extends SwingWorker<Boolean, Void>
    {

        @Override
        protected Boolean doInBackground()
        {
            // store overlapping datapoints in upper and lower graphs
            upper = new ArrayList<>();
            lower = new ArrayList<>();

            // total datapoints within subset of utilized data
            int totalPoints = 0;
            int totalPointsUsed = 0;

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass)
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is within overlap then store point
                        if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            upper.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
                else if (DV.lowerClasses.get(i))
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is within overlap then store point
                        if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            lower.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
            }

            // get percentage of overlap points used
            percentageOverlapPointsUsed = String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints);

            if (DV.overlapChecked)
            {
                String fileName = System.getProperty("user.dir") + "\\src\\Python\\OL_CM.csv";

                // create file for python process
                createCSVFileForConfusionMatrix(new ArrayList<>(List.of(upper, lower)), fileName);

                // get confusion matrix with LDA
                ArrayList<String> cmValues = LDAForConfusionMatrices(false, fileName);

                if (cmValues != null && cmValues.size() > 0)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 0; i < 2; i++)
                        cm.append(curClasses.get(i)).append("\t");

                    for (int i = 0, index = -1; i < 2; i++)
                    {
                        // append class label
                        cm.append("\n").append(curClasses.get(i)).append("\t");

                        // append classifications
                        cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
                    }

                    // append accuracy
                    cm.append("\n").append(cmValues.get(cmValues.size() - 1));

                    // append percentage of total points used
                    cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

                    // set overlap confusion matrix
                    JTextArea confusionMatrix = new JTextArea(cm.toString());
                    confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                    confusionMatrix.setEditable(false);
                    confusionMatrices.put(DV.prevCM.size() + 2, confusionMatrix);
                }
                else if (cmValues != null)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 0; i < 2; i++)
                        cm.append(curClasses.get(i)).append("\t");

                    for (int i = 0; i < 2; i++)
                    {
                        // append class label
                        cm.append("\n").append(curClasses.get(i)).append("\t");

                        // append classifications
                        cm.append(0).append("\t").append(0).append("\t");
                    }

                    // append accuracy
                    cm.append("\nAccuracy: NaN%");

                    // append percentage of total points used
                    cm.append(percentageOverlapPointsUsed);

                    // set overlap confusion matrix
                    JTextArea confusionMatrix = new JTextArea(cm.toString());
                    confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                    confusionMatrix.setEditable(false);
                    confusionMatrices.put(DV.prevCM.size() + 3, confusionMatrix);
                }
            }

            return true;
        }
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses function from getDataWithoutOverlapConfusionMatrix
     */
    private static class GetWorstCaseConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // get double[][] arrays for data
            double[][] tmpUpper = new double[upper.size()][];
            double[][] tmpLower = new double[lower.size()][];

            for (int i = 0; i < upper.size(); i++)
                tmpUpper[i] = upper.get(i);

            for (int i = 0; i < lower.size(); i++)
                tmpLower[i] = lower.get(i);

            // create DataObjects of overlapping points
            DataObject upperOverlap = new DataObject("upper", tmpUpper);
            DataObject lowerOverlap = new DataObject("lower", tmpLower);

            // get data without overlap threshold
            double worstCaseThreshold = LDAFunction.get(LDAFunction.size() - 1);

            // get data without overlap angles
            double[] worstCaseAngles = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
                worstCaseAngles[i] = LDAFunction.get(i);

            // update points with worst case angles
            upperOverlap.updateCoordinates(worstCaseAngles);
            lowerOverlap.updateCoordinates(worstCaseAngles);

            int totalPointsUsed = lower.size() + upper.size();
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            // get distribution for upper graph
            for (int i = 0; i < upper.size(); i++)
            {
                if (DV.upperIsLower)
                {
                    if (upperOverlap.coordinates[i][DV.fieldLength- 1][0] < worstCaseThreshold)
                    {
                        pntDist[0][0]++;
                        correctPoints++;
                    }
                    else
                        pntDist[0][1]++;
                }
                else
                {
                    if (upperOverlap.coordinates[i][DV.fieldLength- 1][0] > worstCaseThreshold)
                    {
                        pntDist[1][1]++;
                        correctPoints++;
                    }
                    else
                        pntDist[1][0]++;
                }
            }

            // get point distribution for lower graph
            for (int i = 0; i < lower.size(); i++)
            {
                if (DV.upperIsLower)
                {
                    if (lowerOverlap.coordinates[i][DV.fieldLength- 1][0] > worstCaseThreshold)
                    {
                        pntDist[1][1]++;
                        correctPoints++;
                    }
                    else
                        pntDist[1][0]++;
                }
                else
                {
                    if (lowerOverlap.coordinates[i][DV.fieldLength- 1][0] < worstCaseThreshold)
                    {
                        pntDist[0][0]++;
                        correctPoints++;
                    }
                    else
                        pntDist[0][1]++;
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("Worst Case Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPointsUsed));

            // append percentage of total points used
            cm.append(percentageOverlapPointsUsed);

            // set all data confusion matrix
            JTextArea confusionMatrix = new JTextArea(cm.toString());
            confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix.setEditable(false);
            confusionMatrices.put(DV.prevCM.size() + 4, confusionMatrix);

            return true;
        }
    }


    /**
     * Generates confusion matrix with user imported validation data
     */
    private static class GetUserValidationConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // get data without overlap threshold
            double worstCaseThreshold = LDAFunction.get(LDAFunction.size() - 1);

            // get data without overlap angles
            double[] worstCaseAngles = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
                worstCaseAngles[i] = LDAFunction.get(i);

            // update points with worst case angles
            for (int i = 0; i < DV.validationData.size(); i++)
                DV.validationData.get(i).updateCoordinates(worstCaseAngles);

            int totalPoints = 0;

            for (int i = 0; i < DV.data.size(); i++)
                totalPoints += DV.data.get(i).data.length;

            int totalPointsUsed = 0;
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            for (int i = 0; i < DV.validationData.size(); i++)
            {
                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    for (int j = 0; j < DV.validationData.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.validationData.get(i).coordinates[j][DV.fieldLength-1][0];

                        // check if endpoint is within the subset of used data
                        if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                        {
                            // get classification
                            if (i == DV.upperClass && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < worstCaseThreshold)
                                {
                                    pntDist[0][0]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if (i == DV.upperClass)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > worstCaseThreshold)
                                {
                                    pntDist[0][0]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > worstCaseThreshold)
                                {
                                    pntDist[1][1]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[1][0]++;
                                }
                            }
                            else
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < worstCaseThreshold)
                                {
                                    pntDist[1][1]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[1][0]++;
                                }
                            }
                        }
                    }
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("User Validation Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPointsUsed));

            // append percentage of total points used
            cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

            // set all data confusion matrix
            JTextArea confusionMatrix = new JTextArea( cm.toString());
            confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix.setEditable(false);
            confusionMatrices.put(DV.prevCM.size() + 5, confusionMatrix);

            return true;
        }
    }


    /**
     * Creates CSV file representing specified data from
     * the upper class as class 1 and lower graph as class 2
     * @param data data to be used in csv file
     * @param fileName name of file to be created
     */
    private static void createCSVFileForConfusionMatrix(ArrayList<ArrayList<double[]>> data, String fileName)
    {
        try
        {
            // write to csv file
            Writer out = new FileWriter(fileName, false);

            // create header for file
            for (int i = 0; i < DV.fieldLength; i++)
            {
                if (i != DV.fieldLength - 1)
                    out.write("feature,");
                else
                    out.write("feature,class\n");
            }

            // check all classes
            for (int i = 0; i < data.size(); i++)
            {
                for (int j = 0; j < data.get(i).size(); j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.write(String.format("%f,", data.get(i).get(j)[k]));
                        else
                            out.write(String.format("%f," + i + "\n", data.get(i).get(j)[k]));
                    }
                }
            }

            // close file
            out.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }


    /**
     * Gets functions for confusion matrices
     * @param storeFunction whether to store angles and threshold
     * @param fileName name of csv file storing data
     */
    private static ArrayList<String> LDAForConfusionMatrices(boolean storeFunction, String fileName)
    {
        // get python boolean
        String pyBool;

        if (storeFunction)
            pyBool = "T";
        else
            pyBool = "";

        // create LDA (python) process
        ProcessBuilder lda = new ProcessBuilder("cmd", "/c",
                System.getProperty("user.dir") + "\\src\\Python\\ConfusionMatrixGenerator\\ConfusionMatrixGenerator.exe",
                fileName,
                pyBool);

        try
        {
            // run python (LDA) process
            Process process = lda.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // confusion matrix classifications
            ArrayList<String> classifications = new ArrayList<>();

            // data without overlap confusion matrix angles and threshold
            if (storeFunction)
                LDAFunction = new ArrayList<>();

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles and threshold
                if (storeFunction && cnt <= DV.fieldLength)
                {
                    LDAFunction.add(Double.parseDouble(output));
                    cnt++;
                }
                else
                {
                    // get rows of confusion matrix
                    classifications.add(output);
                }
            }

            // delete created file
            File fileToDelete = new File(fileName);
            Files.deleteIfExists(fileToDelete.toPath());

            // return confusion matrix
            return classifications;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not create confusion matrix", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


    /**
     * Runs k-fold cross validation on dataset
     */
    static void runKFoldCrossValidation()
    {
        // store datapoints in upper and lower graphs
        ArrayList<double[]> upper = new ArrayList<>();
        ArrayList<double[]> lower = new ArrayList<>();

        // check all classes
        for (int i = 0; i < DV.data.size(); i++)
        {
            if (i == DV.upperClass)
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                    // if endpoint is outside of overlap then store point
                    if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                    {
                        double[] thisPoint = new double[DV.fieldLength];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                        upper.add(thisPoint);
                    }
                }
            }
            else if (DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                    // if endpoint is outside of overlap then store point
                    if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                    {
                        double[] thisPoint = new double[DV.fieldLength];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                        lower.add(thisPoint);
                    }
                }
            }
        }

        String fileName = System.getProperty("user.dir") + "\\src\\Python\\k_fold.csv";

        // create file for python process
        createCSVFileForConfusionMatrix(new ArrayList<>(List.of(upper, lower)), fileName);

        // create k-fold (python) process
        ProcessBuilder cv = new ProcessBuilder("cmd", "/c",
                System.getProperty("user.dir") + "\\src\\Python\\kFoldCrossValidation\\kFoldCrossValidation.exe",
                fileName,
                String.valueOf(DV.kFolds));

        try
        {
            // run python (LDA) process
            Process process = cv.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // confusion matrix classifications
            ArrayList<String> cvTable = new ArrayList<>();

            while ((output = reader.readLine()) != null)
            {
                // get cross validation table
                cvTable.add(output);
            }

            if (cvTable.size() > 0)
            {
                // create confusion matrix
                StringBuilder table = new StringBuilder("k-Fold Cross Validation");

                for (String row : cvTable) {
                    // append model rows
                    table.append("\n").append(row);
                }

                // delete created file
                File fileToDelete = new File(fileName);
                Files.deleteIfExists(fileToDelete.toPath());

                // set overlap confusion matrix
                JTextArea cross_validate = new JTextArea(table.toString());
                cross_validate.setFont(cross_validate.getFont().deriveFont(Font.BOLD, 12f));
                cross_validate.setEditable(false);
                DV.crossValidationPanel.add(cross_validate);
            }
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run k-Fold Cross Validation", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
