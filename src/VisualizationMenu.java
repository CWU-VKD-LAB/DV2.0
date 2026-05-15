import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VisualizationMenu extends JPanel
{
    static String scalarFunction = "x";
    static String vectorFunction = "N/A";

    /**
     * Creates Visualization Options Menu on mouseLocation
     */
    public VisualizationMenu()
    {
        // visualization panel
        JPanel visPanel = new JPanel();
        visPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // choose class to visualize as main
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        visPanel.add(setUpperClassButton(), c);

        // specify visualization
        c.gridx = 1;
        c.gridy = 0;
        visPanel.add(removeClassButton(), c);

        // reorder attributes
        c.gridx = 0;
        c.gridy = 1;
        visPanel.add(reorderAttributesButton(), c);

        // remove attributes
        c.gridx = 1;
        c.gridy = 1;
        visPanel.add(removeAttributesButton(), c);

        // change visualization function for each attribute of each vector
        c.gridx = 0;
        c.gridy = 2;
        visPanel.add(setScalarFunctionButton(), c);

        // change visualization function
        c.gridx = 1;
        c.gridy = 2;
        visPanel.add(setNDFunctionButton(), c);

        // open analytics in another window
        c.gridx = 0;
        c.gridy = 3;
        visPanel.add(separateVisButton(), c);

        // visualize only support vectors
        c.gridx = 1;
        c.gridy = 3;
        visPanel.add(visOnlySVMBox(), c);

        // visualize support vectors
        c.gridx = 0;
        c.gridy = 4;
        visPanel.add(visSVMBox(), c);

        // activates / deactivates domain
        c.gridx = 1;
        c.gridy = 4;
        visPanel.add(domainActiveBox(), c);

        // draw first line of GLC-L visualization
        c.gridx = 0;
        c.gridy = 5;
        visPanel.add(drawFirstLineBox(), c);

        // draw midpoints of GLC-L visualization
        c.gridx = 1;
        c.gridy = 5;
        visPanel.add(drawMidpointsBox(), c);

        // visualize overlap area
        c.gridx = 0;
        c.gridy = 6;
        visPanel.add(visOverlapBox(), c);

        // show visualization menu
        JOptionPane.showOptionDialog(DV.mainFrame, visPanel, "Visualization Options", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{}, null);
    }


    /**
     * Creates button to set the upper class of the visualization
     * @return button
     */
    private JButton setUpperClassButton()
    {
        JButton chooseUpperClassBtn = new JButton("Upper Class");
        chooseUpperClassBtn.setToolTipText("Choose class to be visualized on upper graph");
        chooseUpperClassBtn.setFont(chooseUpperClassBtn.getFont().deriveFont(12f));
        chooseUpperClassBtn.addActionListener(e ->
        {
            if (DV.trainData != null)
            {
                int chosen = JOptionPane.showOptionDialog(
                        DV.mainFrame,
                        "Choose upper class.\nThe upper class will be visualized on the upper graph.",
                        "Choose Upper Class",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        DV.uniqueClasses.toArray(),
                        null);

                if (chosen != -1)
                {
                    // remove past accuracies and classifications
                    DV.prevAllDataCM.clear();
                    DV.prevAllDataClassifications.clear();

                    // set upper class
                    DV.upperClass = chosen;

                    // lower class gets all others
                    int size = 0;
                    for (int i = 0; i < DV.classNumber; i++)
                    {
                        if (i != chosen)
                        {
                            DV.lowerClasses.set(i, true);
                            size += DV.trainData.get(i).data.length;
                        }
                        else
                            DV.lowerClasses.set(i, false);
                    }

                    // create highlights
                    DV.highlights = new boolean[DV.classNumber][];
                    DV.highlights[0] = new boolean[DV.trainData.get(DV.upperClass).data.length];
                    DV.highlights[1] = new boolean[size];
                    Arrays.fill(DV.highlights[0], false);
                    Arrays.fill(DV.highlights[1], false);

                    // generate new cross validation
                    DV.crossValidationChecked = false;
                    DV.crossValidationNotGenerated = true;

                    // optimize setup then draw graphs
                    DataVisualization.optimizeSetup();
                    DataVisualization.drawGraphs();
                }
            }
            else
            {
                DV.warningPopup(
                        "Please create a project before choosing the upper class.\nFor additional information, please view the \"Help\" tab.",
                        "Error: no data");
            }
        });
        
        return chooseUpperClassBtn;
    }


    /**
     * Creates button to remove class from 3+ class visualization
     * @return button
     */
    private JButton removeClassButton()
    {
        JButton specifyVisBtn = new JButton("Specify Visualization");
        specifyVisBtn.setToolTipText("Removes one class from the lower graph");
        specifyVisBtn.setFont(specifyVisBtn.getFont().deriveFont(12f));
        specifyVisBtn.addActionListener(e ->
        {
            if (DV.trainData != null)
            {
                // classes on lower graph
                ArrayList<String> removableClasses = new ArrayList<>();
                ArrayList<String> classes = new ArrayList<>(DV.uniqueClasses);

                for (int i = 0; i < DV.classNumber; i++)
                {
                    if (DV.lowerClasses.get(i))
                        removableClasses.add(classes.get(i));
                }

                if (removableClasses.size() > 1)
                {
                    JLabel removableLabel = new JLabel("Removable Classes");
                    JComboBox<Object> removableList = new JComboBox<>(removableClasses.toArray());
                    removableList.setSelectedIndex(0);
                    removableList.setEditable(true);

                    JPanel removablePanel = new JPanel();
                    removablePanel.add(removableLabel);
                    removablePanel.add(removableList);

                    int choice = JOptionPane.showConfirmDialog(
                            DV.mainFrame,
                            removablePanel,
                            "Remove Class",
                            JOptionPane.OK_CANCEL_OPTION);

                    if (choice == 0)
                    {
                        // add previous analytics
                        DV.prevAllDataCM.add(DV.allDataCM);
                        DV.prevAllDataClassifications.add(DV.allDataClassifications);

                        // get class to be removed
                        int selected = removableList.getSelectedIndex();
                        String className = removableClasses.get(selected);

                        // remove class
                        for (int i = 0; i < DV.classNumber; i++)
                        {
                            if (className.equals(classes.get(i)))
                                DV.lowerClasses.set(i, false);
                        }

                        // optimize setup then draw graphs
                        DataVisualization.optimizeSetup();
                        DataVisualization.drawGraphs();
                    }
                }
                else
                    JOptionPane.showMessageDialog(DV.mainFrame, "Classes cannot be further separated.");
            }
            else
            {
                DV.warningPopup(
                        "Please create a project before specifying the visualization.\nFor additional information, please view the \"Help\" tab.",
                        "Error: not data");
            }
        });
        return specifyVisBtn;
    }


    /**
     * Creates button to reorder attributes
     * @return button
     */
    private JButton reorderAttributesButton()
    {
        JButton reorderBtn = new JButton("Reorder Attributes");
        reorderBtn.setToolTipText("Reorder attributes in visualization");
        reorderBtn.setFont(reorderBtn.getFont().deriveFont(12f));
        reorderBtn.addActionListener(e ->
        {
            if (DV.trainData != null)
            {
                JPanel reorder = new JPanel();
                reorder.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();

                c.gridx = 0;
                c.gridy = 0;
                c.gridwidth = 2;
                c.fill = GridBagConstraints.BOTH;

                reorder.add(new JLabel("Attributes will be displayed in the specified order."), c);

                DefaultTableModel tm = new DefaultTableModel();
                tm.setColumnIdentifiers( new Object[] { "Index", "Feature" });
                final JTable table = new JTable(tm)
                {
                    public boolean isCellEditable(int row, int column)
                    {
                        return false;
                    }
                };

                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                table.getTableHeader().setReorderingAllowed(false);

                for (int i = 0; i < DV.fieldNames.size(); i++)
                    tm.insertRow(i, new Object[] { i+1, DV.fieldNames.get(i) });

                JScrollPane manPane = new JScrollPane(table);

                int yCnt = 1;
                c.gridy = yCnt;
                reorder.add(manPane, c);

                // move row up
                JButton up = increaseAttributeIndex(table, tm);

                c.gridy = ++yCnt;
                c.weightx = 0.5;
                c.gridwidth = 1;
                reorder.add(up, c);

                // move row down
                JButton down = decreaseAttributeIndex(table, tm);

                c.gridx = 1;
                reorder.add(down, c);

                // original attribute order
                JButton original = defaultAttributeOrder(tm);

                c.gridx = 0;
                c.gridy = ++yCnt;
                reorder.add(original, c);

                // decision tree attribute order
                JButton dt = DTAttributeOrder(tm);

                c.gridx = 1;
                c.weightx = 0.5;
                reorder.add(dt, c);

                // order from least to greatest contribution
                JButton lessToGreat = increasingContributionAttributeOrder(tm);

                c.gridx = 0;
                c.gridy = ++yCnt;
                c.weightx = 0.5;
                reorder.add(lessToGreat, c);

                // order from greatest to least contribution
                JButton greatToLess = decreasingContributionAttributeOrder(tm);

                c.gridx = 1;
                c.weightx = 0.5;
                reorder.add(greatToLess, c);

                JOptionPane.showMessageDialog(DV.mainFrame, reorder, "Reorder Attributes", JOptionPane.PLAIN_MESSAGE);
            }
        });

        return reorderBtn;
    }


    /**
     * Creates Button to increase attribute index
     * @param table table display
     * @param tm table of attributes
     * @return button
     */
    private JButton increaseAttributeIndex(JTable table, DefaultTableModel tm)
    {
        JButton up = new JButton("Up");
        up.setToolTipText("Moves selected row up.");
        up.addActionListener(e ->
        {
            int index = table.getSelectedRow();

            if (index == 0 || index == -1) return;

            table.setValueAt(index+1, index-1, 0);
            table.setValueAt(index, index, 0);
            tm.moveRow(index, index, index-1);
            table.setRowSelectionInterval(index-1, index-1);

            // reorder
            swap(index, index-1);

            // update angles
            DV.angleSliderPanel.removeAll();

            for (int j = 0; j < DV.trainData.get(0).coordinates[0].length; j++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);

            DataVisualization.drawGraphs();
        });

        return up;
    }


    /**
     * Creates Button to decrease attribute index
     * @param table table display
     * @param tm table of attributes
     * @return button
     */
    private JButton decreaseAttributeIndex(JTable table, DefaultTableModel tm)
    {
        JButton down = new JButton("Down");
        down.setToolTipText("Moves selected row down.");
        down.addActionListener(e ->
        {
            int index = table.getSelectedRow();

            if (index == DV.fieldLength-1 || index == -1) return;

            table.setValueAt(index+1, index+1, 0);
            table.setValueAt(index+2, index, 0);
            tm.moveRow(index, index, index+1);
            table.setRowSelectionInterval(index+1, index+1);

            // reorder
            swap(index, index + 1);

            // update angles
            DV.angleSliderPanel.removeAll();

            for (int j = 0; j < DV.trainData.get(0).coordinates[0].length; j++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);

            DataVisualization.drawGraphs();
        });

        return down;
    }


    /**
     * Creates button to reset attribute order to default
     * @param tm table of attributes
     * @return button
     */
    private JButton defaultAttributeOrder(DefaultTableModel tm)
    {
        JButton original = new JButton("Original Order");
        original.setToolTipText("Order attributes in their original order.");
        original.addActionListener(e ->
        {
            // sort ascending
            quickSort(DV.originalAttributeOrder, 0, DV.originalAttributeOrder.size() - 1);

            tm.setRowCount(0);
            for (int i = 0; i < DV.fieldNames.size(); i++)
                tm.insertRow(i, new Object[] {i+1, DV.fieldNames.get(i)});

            DV.angleSliderPanel.removeAll();

            // update angles
            for (int j = 0; j < DV.trainData.get(0).coordinates[0].length; j++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);

            DataVisualization.drawGraphs();
        });

        return original;
    }


    /**
     * Creates button to set attribute order to that of a Decision Tree (DT)
     * @param tm table of attributes
     * @return button
     */
    private JButton DTAttributeOrder(DefaultTableModel tm)
    {
        JButton dt = new JButton("Decision Tree Order");
        dt.setToolTipText("Order attributes according to a Decision Tree.");
        dt.addActionListener(e ->
        {
            // get decision tree order
            double[] dt_weight = new double[DV.fieldLength];

            // create dt (python) process
            ProcessBuilder tree = new ProcessBuilder("cmd", "/c",
                    "source\\Python\\DecisionTree\\DecisionTree.exe",
                    "source\\Python\\DV_data.csv");

            try
            {
                // create file for python process
                CSV.createCSVDataObject(DV.trainData, "source\\Python\\DV_data.csv");

                // run python (LDA) process
                Process process = tree.start();

                // read python outputs
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output;

                int cnt = 0;

                // get attribute order
                while ((output = reader.readLine()) != null)
                    dt_weight[cnt++] = Double.parseDouble(output);

                // delete created file
                File fileToDelete = new File("source\\Python\\DV_data.csv");
                Files.deleteIfExists(fileToDelete.toPath());
            }
            catch (IOException dte)
            {
                JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Decision Tree", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // sort to original
            for (int i = 0; i < DV.originalAttributeOrder.size() - 1; i++)
            {
                for (int j = 0; j < DV.originalAttributeOrder.size() - i - 1; j++)
                {
                    if (DV.originalAttributeOrder.get(j) < DV.originalAttributeOrder.get(j+1))
                    {
                        int tmp1 = DV.originalAttributeOrder.get(j);
                        DV.originalAttributeOrder.set(j, DV.originalAttributeOrder.get(j+1));
                        DV.originalAttributeOrder.set(j+1, tmp1);

                        double tmp2 = DV.angles[j];
                        DV.angles[j] = DV.angles[j+1];
                        DV.angles[j+1] = tmp2;

                        String tmp3 = DV.fieldNames.get(j);
                        DV.fieldNames.set(j, DV.fieldNames.get(j+1));
                        DV.fieldNames.set(j+1, tmp3);

                        // reorder in all data
                        for (int k = 0; k < DV.trainData.size(); k++)
                        {
                            for (int w = 0; w < DV.trainData.get(k).data.length; w++)
                            {
                                double tmp = DV.trainData.get(k).data[w][j];
                                DV.trainData.get(k).data[w][j] = DV.trainData.get(k).data[w][j+1];
                                DV.trainData.get(k).data[w][j+1] = tmp;
                            }
                        }
                    }
                }
            }

            // bubble sort ascending
            quickSort(dt_weight, 0, dt_weight.length - 1);

            tm.setRowCount(0);
            for (int i = 0; i < DV.fieldNames.size(); i++)
                tm.insertRow(i, new Object[] {i+1, DV.fieldNames.get(i)});

            DV.angleSliderPanel.removeAll();

            // update angles
            for (int j = 0; j < DV.trainData.get(0).coordinates[0].length; j++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);

            DataVisualization.drawGraphs();
        });
        return dt;
    }


    /**
     * Creates button to sort attribute order by increasing LDF contribution
     * @param tm table of attributes
     * @return button
     */
    private JButton increasingContributionAttributeOrder(DefaultTableModel tm)
    {
        JButton lessToGreat = new JButton("Ascending Contribution");
        lessToGreat.setToolTipText("Order attributes from least contributing (largest angle) to most contributing (smallest angle).");
        lessToGreat.addActionListener(e ->
        {
            // bubble sort ascending
            quickSort(DV.angles, 0, DV.angles.length - 1);

            tm.setRowCount(0);
            for (int i = 0; i < DV.fieldNames.size(); i++)
                tm.insertRow(i, new Object[] {i+1, DV.fieldNames.get(i)});

            DV.angleSliderPanel.removeAll();

            // update angles
            for (int j = 0; j < DV.trainData.get(0).coordinates[0].length; j++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);

            DataVisualization.drawGraphs();
        });
        return lessToGreat;
    }


    /**
     * Creates button to sort attribute order by decreasing LDF contribution
     * @param tm table of attributes
     * @return button
     */
    private JButton decreasingContributionAttributeOrder(DefaultTableModel tm)
    {
        JButton greatToLess = new JButton("Descending Contribution");
        greatToLess.setToolTipText("Order attributes from most contributing (smallest angle) to least contributing (largest angle).");
        greatToLess.addActionListener(e ->
        {
            // bubble sort descending
            quickSortDescending(DV.angles, 0, DV.angles.length - 1);

            tm.setRowCount(0);
            for (int i = 0; i < DV.fieldNames.size(); i++)
                tm.insertRow(i, new Object[] {i+1, DV.fieldNames.get(i)});

            DV.angleSliderPanel.removeAll();

            // update angles
            for (int j = 0; j < DV.trainData.get(0).coordinates[0].length; j++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);

            DataVisualization.drawGraphs();
        });
        return greatToLess;
    }


    /**
     * Quick Sort Sorting Algorithm
     * @param data data to sort
     * @param low starting index
     * @param high ending index
     */
    public static void quickSort(double[] data, int low, int high)
    {
        // ensure indices are ordered correctly
        if (low < high)
        {
            // partition array and get pivot index
            int p = partition(data, low, high);

            // sort partitions
            quickSort(data, low, p - 1);
            quickSort(data, p + 1, high);
        }
    }


    /**
     * Quick Sort Sorting Algorithm
     * @param data data to sort
     * @param low starting index
     * @param high ending index
     */
    private void quickSort(ArrayList<Integer> data, int low, int high)
    {
        // ensure indices are ordered correctly
        if (low < high)
        {
            // partition array and get pivot index
            int p = partition(data, low, high);

            // sort partitions
            quickSort(data, low, p - 1);
            quickSort(data, p + 1, high);
        }
    }


    /**
     * Quick Sort Sorting Algorithm for Descending order
     * @param data data to sort
     * @param low starting index
     * @param high ending index
     */
    public static void quickSortDescending(double[] data, int low, int high)
    {
        // ensure indices are ordered correctly
        if (low < high)
        {
            // partition array and get pivot index
            int p = partitionDescending(data, low, high);

            // sort partitions
            quickSortDescending(data, low, p);
            quickSortDescending(data, p + 1, high);
        }
    }


    /**
     * Partition for Quick Sort
     * @param data data to sort
     * @param low starting index
     * @param high ending index
     * @return pivot index
     */
    private static int partition(double[] data, int low, int high)
    {
        double pivot = data[high];

        // temporary pivot index
        int i = low - 1;
        for (int j = low; j <= high - 1; j++)
        {
            // if the current element is less than or equal to the pivot
            // swap the current element with the element at the temporary pivot
            if (data[j] < pivot)
            {
                i++;
                swap(data, i, j);

            }
        }

        // swap pivot and last element
        swap(data, i+1, high);
        return i + 1;
    }


    /**
     * Partition for Quick Sort
     * @param data data to sort
     * @param low starting index
     * @param high ending index
     * @return pivot index
     */
    private int partition(ArrayList<Integer> data, int low, int high)
    {
        double pivot = data.get(high);

        // temporary pivot index
        int i = low - 1;
        for (int j = low; j <= high - 1; j++)
        {
            // if the current element is less than or equal to the pivot
            // swap the current element with the element at the temporary pivot
            if (data.get(j) < pivot)
            {
                i++;
                swap(data, i, j);
            }
        }

        // swap pivot and last element
        swap(data, i+1, high);
        return i + 1;
    }


    /**
     * Partition for Quick Sort for Descending order
     * @param data data to sort
     * @param low starting index
     * @param high ending index
     * @return pivot index
     */
    private static int partitionDescending(double[] data, int low, int high)
    {
        double pivot = data[low];

        // temporary pivot index
        int i = low;
        for (int j = low + 1; j <= high; j++)
        {
            // if the current element is greater than or equal to the pivot
            // swap the current element with the element at the temporary pivot
            if (data[j] > pivot)
            {
                i++;
                swap(data, i, j);
            }
        }

        // swap pivot and last element
        swap(data, i, low);
        return i;
    }


    /**
     * Swap elements in data, angles, fieldnames, and all data
     * @param data data to be swapped
     * @param i first index
     * @param j second index
     */
    private static void swap(double[] data, int i, int j)
    {
        double tmp1 = data[i];
        data[i] = data[j];
        data[j] = tmp1;

        if (data != DV.angles)
        {
            tmp1 = DV.angles[i];
            DV.angles[i] = DV.angles[j];
            DV.angles[j] = tmp1;
        }

        String tmp2 = DV.fieldNames.get(i);
        DV.fieldNames.set(i, DV.fieldNames.get(j));
        DV.fieldNames.set(j, tmp2);

        int tmp3 = DV.originalAttributeOrder.get(i);
        DV.originalAttributeOrder.set(i, DV.originalAttributeOrder.get(j));
        DV.originalAttributeOrder.set(j, tmp3);

        // reorder in all data
        for (int k = 0; k < DV.trainData.size(); k++)
        {
            for (int w = 0; w < DV.trainData.get(k).data.length; w++)
            {
                double tmp4 = DV.trainData.get(k).data[w][i];
                DV.trainData.get(k).data[w][i] = DV.trainData.get(k).data[w][j];
                DV.trainData.get(k).data[w][j] = tmp4;
            }
        }
    }


    /**
     * Swap elements in data, angles, fieldnames, and all data
     * @param data data to be swapped
     * @param i first index
     * @param j second index
     */
    private void swap(ArrayList<Integer> data, int i, int j)
    {
        int tmp0 = data.get(i);
        data.set(i, data.get(j));
        data.set(j, tmp0);

        double tmp1 = DV.angles[i];
        DV.angles[i] = DV.angles[j];
        DV.angles[j] = tmp1;

        String tmp2 = DV.fieldNames.get(i);
        DV.fieldNames.set(i, DV.fieldNames.get(j));
        DV.fieldNames.set(j, tmp2);

        int tmp3 = DV.originalAttributeOrder.get(i);
        DV.originalAttributeOrder.set(i, DV.originalAttributeOrder.get(j));
        DV.originalAttributeOrder.set(j, tmp3);

        // reorder in all data
        for (int k = 0; k < DV.trainData.size(); k++)
        {
            for (int w = 0; w < DV.trainData.get(k).data.length; w++)
            {
                double tmp4 = DV.trainData.get(k).data[w][i];
                DV.trainData.get(k).data[w][i] = DV.trainData.get(k).data[w][j];
                DV.trainData.get(k).data[w][j] = tmp4;
            }
        }
    }


    /**
     * Swap elements in angles, fieldnames, and all data
     * @param i first index
     * @param j second index
     */
    private void swap(int i, int j)
    {
        double tmp1 = DV.angles[i];
        DV.angles[i] = DV.angles[j];
        DV.angles[j] = tmp1;

        String tmp2 = DV.fieldNames.get(i);
        DV.fieldNames.set(i, DV.fieldNames.get(j));
        DV.fieldNames.set(j, tmp2);

        int tmp3 = DV.originalAttributeOrder.get(i);
        DV.originalAttributeOrder.set(i, DV.originalAttributeOrder.get(j));
        DV.originalAttributeOrder.set(j, tmp3);

        // reorder in all data
        for (int k = 0; k < DV.trainData.size(); k++)
        {
            for (int w = 0; w < DV.trainData.get(k).data.length; w++)
            {
                double tmp4 = DV.trainData.get(k).data[w][i];
                DV.trainData.get(k).data[w][i] = DV.trainData.get(k).data[w][j];
                DV.trainData.get(k).data[w][j] = tmp4;
            }
        }
    }


    /**
     * Creates button to remove attributes from visualization
     * @return button
     */
    private JButton removeAttributesButton()
    {
        JButton removeBtn = new JButton("Remove Attributes");
        removeBtn.setToolTipText("Remove attributes in visualization");
        removeBtn.setFont(removeBtn.getFont().deriveFont(12f));
        removeBtn.addActionListener(e ->
        {
            if (DV.trainData != null)
            {
                JPanel removal = new JPanel();
                removal.setLayout(new BoxLayout(removal, BoxLayout.PAGE_AXIS));
                removal.add(new JLabel("Checked attributes will be displayed."));

                JCheckBox[] attributes = new JCheckBox[DV.standardFieldNames.size()];
                for (String field : DV.standardFieldNames)
                {
                    final int index = DV.standardFieldNames.indexOf(field);
                    attributes[index] = new JCheckBox(field, DV.activeAttributes.get(index));
                    attributes[index].addActionListener(ae ->
                    {
                        DV.activeAttributes.set(index, attributes[index].isSelected());

                        // ensure there is at least 1 active attribute
                        int active = getActiveAttributes();
                        if (active > 0)
                            DV.fieldLength = active;
                        else
                        {
                            attributes[index].setSelected(true);
                            DV.activeAttributes.set(index, true);
                            return;
                        }

                        DataVisualization.updateGraphs();
                    });

                    removal.add(attributes[index]);
                }

                JOptionPane.showMessageDialog(DV.mainFrame, removal, "Remove Attributes", JOptionPane.PLAIN_MESSAGE);
            }
        });

        return removeBtn;
    }


    /**
     * Creates button to apply a scalar function to all data
     * @return button
     */
    private JButton setScalarFunctionButton()
    {
        JButton scalarVisFuncBtn = new JButton("Scalar Function");
        scalarVisFuncBtn.setToolTipText("Applies given function to all attributes of all data points");
        scalarVisFuncBtn.setFont(scalarVisFuncBtn.getFont().deriveFont(12f));
        scalarVisFuncBtn.addActionListener(e ->
        {
            // popup asking for number of folds
            JPanel funcPanel = new JPanel(new BorderLayout());

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField funcField = new JTextField();
            funcField.setPreferredSize(new Dimension(200, 30));
            funcField.setText(scalarFunction);
            JLabel funcLabel = new JLabel("Function: f(x) = ");
            funcLabel.setFont(funcLabel.getFont().deriveFont(12f));
            textPanel.add(funcLabel);
            textPanel.add(funcField);

            // add text panel
            funcPanel.add(textPanel, BorderLayout.SOUTH);

            Object[] funcButtons = { "Ok", "Cancel", "Help" };

            boolean notChosen = true;

            // loop until folds are valid or user quits
            while (notChosen)
            {
                int choice = JOptionPane.showOptionDialog(
                        DV.mainFrame, funcPanel,
                        "Enter function",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        funcButtons,
                        funcButtons[0]);

                switch (choice)
                {
                    case 0 ->
                    {
                        // get function and remove spaces
                        String func = funcField.getText();

                        try
                        {
                            // try new function with dummy variables
                            Map<String, Double> variables = new HashMap<>();
                            FunctionParser.Expression exp = FunctionParser.parseScalerExpression(func, variables);

                            variables.put("x", 0.5);
                            exp.eval();

                            // apply function if working
                            scalarFunction = func;

                            for (int i = 0; i < DV.trainData.size(); i++)
                            {
                                for (int j = 0; j < DV.trainData.get(i).data.length; j++)
                                {
                                    for (int k = 0; k < DV.fieldLength; k++)
                                    {
                                        variables.put("x", DV.trainData.get(i).data[j][k]);
                                        DV.trainData.get(i).data[j][k] = exp.eval();
                                    }
                                }
                            }

                            DV.crossValidationNotGenerated = true;
                            DataVisualization.optimizeSetup();
                            DataVisualization.drawGraphs();
                        }
                        catch (Exception exc)
                        {
                            // invalid function input
                            DV.warningPopup("Error",
                                    """
                                    Error: input is invalid.
                                    Please enter a valid function.
                                    Select "Help" for more info.
                                    """);
                        }

                        notChosen = false;
                    }
                    case 2 -> DV.informationPopup(
                            "Function Help",
                            """
                                    Enter a function with "x" as the only variable.
                                    All functions must use the symbols below.
                                    Symbols not included below cannot be used.
                                    
                                        Addition: +
                                        Subtraction: -
                                        Multiplication: *
                                        Division: /
                                        Exponent: ^
                                        Square Root: sqrt()
                                        Parenthesis: ( )
                                        Sine: sin()
                                        Cosine: cos()
                                        Tangent: tan()
                                        e: 2.7182818
                                    
                                    Example:
                                        f(x) = 2 * sqrt(sin(x^2))
                                    """);
                    default -> { return; }
                }
            }
        });
        return scalarVisFuncBtn;
    }


    /**
     * Creates button to apply a vector function to all data
     * @return button
     */
    private JButton setNDFunctionButton()
    {
        JButton vectorVisFuncBtn = new JButton("n-D Point Function");
        vectorVisFuncBtn.setToolTipText("Applies given function to all n-D points");
        vectorVisFuncBtn.setFont(vectorVisFuncBtn.getFont().deriveFont(12f));
        vectorVisFuncBtn.addActionListener(e ->
        {
            // popup asking for number of folds
            JPanel funcPanel = new JPanel();
            funcPanel.setLayout(new BoxLayout(funcPanel, BoxLayout.Y_AXIS));

            // svm vectors or input vectors
            ButtonGroup vectorType = new ButtonGroup();
            JRadioButton svmVec = new JRadioButton("SVM Support Vectors", true);
            JRadioButton userVec = new JRadioButton("User n-D Points");
            vectorType.add(svmVec);
            vectorType.add(userVec);

            // vector type panel
            JPanel vecPanel = new JPanel();
            JLabel vecLabel = new JLabel("n-D Point Type: ");
            vecLabel.setFont(vecLabel.getFont().deriveFont(12f));
            vecPanel.add(vecLabel);
            vecPanel.add(svmVec);
            vecPanel.add(userVec);
            vecPanel.add(getUserVectors());
            funcPanel.add(vecPanel);

            // radio button group
            ButtonGroup stockFunc = new ButtonGroup();
            JRadioButton svmPolyFunc = new JRadioButton("SVM - Polynomial Kernel");
            JRadioButton svmRBFFunc = new JRadioButton("SVM - RBF Kernel");
            JRadioButton customFunc = new JRadioButton("Custom");
            JRadioButton noFunc = new JRadioButton("None");
            stockFunc.add(svmPolyFunc);
            stockFunc.add(svmRBFFunc);
            stockFunc.add(customFunc);
            stockFunc.add(noFunc);

            // default function panel
            JPanel stockPanel = new JPanel();
            JLabel stockLabel = new JLabel("Build-In Function: ");
            stockLabel.setFont(stockLabel.getFont().deriveFont(12f));
            stockPanel.add(stockLabel);
            stockPanel.add(svmPolyFunc);
            stockPanel.add(svmRBFFunc);
            stockPanel.add(customFunc);
            stockPanel.add(noFunc);
            funcPanel.add(stockPanel);

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField funcField = new JTextField();
            funcField.setPreferredSize(new Dimension(200, 30));
            funcField.setText(vectorFunction);
            JLabel funcLabel = new JLabel("Function: f(x, y) = ");
            funcLabel.setFont(funcLabel.getFont().deriveFont(12f));
            textPanel.add(funcLabel);
            textPanel.add(funcField);

            // set text
            if (vectorFunction.equals("(1/" + DV.standardFieldLength + " * dot(x, y) + 1)^3"))
                svmPolyFunc.setSelected(true);
            else if (vectorFunction.equals("e^(-1/" + DV.standardFieldLength + " * norm(vSub(x, y))^2)"))
                svmRBFFunc.setSelected(true);
            else if (vectorFunction.equals("N/A"))
                noFunc.setSelected(true);
            else
                customFunc.setSelected(true);

            // add text panel
            funcPanel.add(textPanel);

            // add listeners
            svmPolyFunc.addActionListener(ev -> funcField.setText("(1/" + DV.standardFieldLength + " * dot(x, y) + 1)^3"));
            svmRBFFunc.addActionListener(ev -> funcField.setText("e^(-1/" + DV.standardFieldLength + " * norm(vSub(x, y))^2)"));
            noFunc.addActionListener(ev -> funcField.setText("N/A"));

            funcField.addKeyListener(new KeyListener()
            {
                @Override
                public void keyTyped(KeyEvent e)
                {
                    customFunc.setSelected(true);
                }

                @Override
                public void keyPressed(KeyEvent e) {}

                @Override
                public void keyReleased(KeyEvent e) {}
            });

            Object[] funcButtons = { "Ok", "Cancel", "Help" };

            boolean notChosen = true;

            // loop until folds are valid or user quits
            while (notChosen)
            {
                int choice = JOptionPane.showOptionDialog(
                        DV.mainFrame, funcPanel,
                        "Enter function",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        funcButtons,
                        funcButtons[0]);

                switch (choice)
                {
                    case 0 ->
                    {
                        if (svmVec.isSelected() && !DV.haveSVM)
                            DataVisualization.SVM();

                        // get function and remove spaces
                        String func = funcField.getText();
                        if (func.equals("N/A"))
                        {
                            DataSetup.setupWithData(DV.dataFiles.get(0));

                            DataVisualization.optimizeSetup();
                            DataVisualization.drawGraphs();
                        }
                        else
                        {
                            try
                            {
                                // try new function with dummy variables
                                Map<String, FunctionParser.VectorExpression> variables = new HashMap<>();
                                FunctionParser.Expression exp = FunctionParser.parseVectorExpression(func, variables);

                                variables.put("x", () -> new double[]{0, 0.3, 0.8});
                                variables.put("y", () -> new double[]{1, 0.6, 0.1});
                                exp.eval();

                                // apply function if working
                                vectorFunction = func;
                                ArrayList<double[][]> splitByClass = new ArrayList<>();
                                for (int i = 0; i < DV.trainData.size(); i++)
                                {
                                    ArrayList<double[]> classData = new ArrayList<>();
                                    for (int j = 0; j < DV.trainData.get(i).data.length; j++)
                                    {
                                        ArrayList<Double> newRow = new ArrayList<>();
                                        for (int k = 0; k < DV.supportVectors.data.length; k++)
                                        {
                                            final double[] x = DV.trainData.get(i).data[j];
                                            final double[] y = DV.supportVectors.data[k];

                                            variables.put("x", () -> x);
                                            variables.put("y", () -> y);
                                            newRow.add(exp.eval());
                                        }

                                        double[] newRowArray = new double[newRow.size()];

                                        for (int w = 0; w < newRow.size(); w++)
                                            newRowArray[w] = newRow.get(w);

                                        classData.add(newRowArray);
                                    }

                                    double[][] newClassData = new double[classData.size()][];

                                    for (int w = 0; w < classData.size(); w++)
                                        newClassData[w] = classData.get(w);

                                    splitByClass.add(newClassData);
                                }

                                DV.trainData = DataSetup.createDataObjects(splitByClass);
                                DV.fieldLength = splitByClass.get(0)[0].length;
                                DV.angles = new double[DV.fieldLength];
                                DV.prevAngles = new double[DV.fieldLength];
                                DV.fieldNames.clear();
                                DV.activeAttributes.clear();

                                for (int i = 0; i < DV.fieldLength; i++)
                                {
                                    DV.fieldNames.add("feature " + i);
                                    DV.angles[i] = 45;
                                    DV.prevAngles[i] = 45;
                                    DV.activeAttributes.add(true);
                                }

                                DV.crossValidationNotGenerated = true;

                                DataVisualization.optimizeSetup();
                                DataVisualization.drawGraphs();
                            }
                            catch (Exception exc)
                            {
                                // invalid function input
                                DV.warningPopup("Error",
                                        """
                                        Error: input is invalid.
                                        Please enter a valid function.
                                        Select "Help" for more info.
                                        """);
                            }
                        }

                        notChosen = false;
                    }
                    case 2 -> DV.informationPopup(
                            "Function Help",
                            """
                                Enter a function with "x" and "y" as the only variables.
                                "x" will be a vector in the dataset and "y" will be a support vector.
                                All functions must use the symbols below.
                                Symbols not included below cannot be used.
                                
                                    Addition: +
                                    Subtraction: -
                                    Multiplication: *
                                    Division: /
                                    Exponent: ^
                                    Square Root: sqrt()
                                    Parenthesis: ( )
                                    Sine: sin()
                                    Cosine: cos()
                                    Tangent: tan()
                                    Dot Product: dot(x,y)
                                    Vector Norm: norm(x,y)
                                    Vector Addition: vAdd(x,y)
                                    Vector Subtraction: vSub(x,y)
                                    e: 2.7182818
                                
                                Example:
                                    f(x) = e^(-1/9 * norm(vSub(x, y))^2)
                                """);
                    default -> { return; }
                }
            }
        });

        return vectorVisFuncBtn;
    }


    /**
     * Creates button to get user support vectors
     * @return button
     */
    private static JButton getUserVectors()
    {
        JButton userVecInput = new JButton("User n-D Points Input");
        userVecInput.addActionListener(e ->
        {
            if (!DV.trainData.isEmpty())
            {
                // set filter on file chooser
                JFileChooser fileDialog = new JFileChooser();
                fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileDialog.setCurrentDirectory(workingDirectory);

                // open file dialog
                int results = fileDialog.showOpenDialog(DV.mainFrame);

                if (results == JFileChooser.APPROVE_OPTION)
                {
                    File importFile = fileDialog.getSelectedFile();

                    // check if import was successful
                    boolean success = DataSetup.setupSupportVectors(importFile);

                    // create graphs
                    if (success)
                    {
                        DV.informationPopup(
                                "Vectors successfully imported.\n",
                                "Success: vectors imported");
                    }
                    else
                    {
                        // add blank graph
                        DV.warningPopup(
                                "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                                "Error: could not open file");
                    }
                }
                else if (results != JFileChooser.CANCEL_OPTION)
                {
                    DV.warningPopup(
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file");
                }
            }
            else
            {
                DV.warningPopup(
                        "Please create a project before importing data.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not import data");
            }

            // repaint and revalidate graph
            DV.graphPanel.repaint();
            DV.graphPanel.revalidate();
        });
        return userVecInput;
    }


    /**
     * Select whether to visualize only the overlap data or not
     * @return check box
     */
    private JCheckBox visOverlapBox()
    {
        JCheckBox visOverlapBox = new JCheckBox("Visualize Overlap");
        visOverlapBox.setToolTipText("Visualize the overlap area");
        visOverlapBox.setFont(visOverlapBox.getFont().deriveFont(12f));
        visOverlapBox.addActionListener(e ->
        {
            DV.drawOverlap = visOverlapBox.isSelected();

            if (DV.trainData != null)
                DataVisualization.drawGraphs();
        });
        
        return visOverlapBox;
    }


    /**
     * Select whether to visualize only SVM data or not
     * @return check box
     */
    private JCheckBox visOnlySVMBox()
    {
        JCheckBox visOnlySVMBox = new JCheckBox("Visualize Only SVM");
        visOnlySVMBox.setToolTipText("Visualize only support vectors.");
        visOnlySVMBox.setFont(visOnlySVMBox.getFont().deriveFont(12f));
        visOnlySVMBox.addActionListener(e ->
        {
            DV.drawOnlySVM = visOnlySVMBox.isSelected();

            if (DV.trainData != null && DV.supportVectors != null)
                DataVisualization.drawGraphs();
        });
        
        return visOnlySVMBox;
    }


    /**
     * Select whether to visualize the SVM data or not
     * @return check box
     */
    private JCheckBox visSVMBox()
    {
        JCheckBox svmVisBox = new JCheckBox("Visualize SVM", DV.drawSVM);
        svmVisBox.setToolTipText("Visualize support vectors along with all other data.");
        svmVisBox.setFont(svmVisBox.getFont().deriveFont(12f));
        svmVisBox.addActionListener(e ->
        {
            DV.drawSVM = svmVisBox.isSelected();

            if (DV.trainData != null && DV.supportVectors != null)
                DataVisualization.drawGraphs();
        });
        
        return svmVisBox;
    }


    /**
     * Select whether to activate the domain line or not
     * @return check box
     */
    private JCheckBox domainActiveBox()
    {
        JCheckBox domainActiveBox = new JCheckBox("Domain Active", DV.domainActive);
        domainActiveBox.setToolTipText("Whether the domain is active or not.");
        domainActiveBox.setFont(domainActiveBox.getFont().deriveFont(12f));
        domainActiveBox.addActionListener(e ->
        {
            DV.domainActive = domainActiveBox.isSelected();
            if (DV.trainData != null)
                DataVisualization.drawGraphs();
        });
        
        return domainActiveBox;
    }


    /**
     * Select whether to visualize the first line in a GLC-L visualization or not
     * @return check box
     */
    private JCheckBox drawFirstLineBox()
    {
        JCheckBox drawFirstLineBox = new JCheckBox("First Line", DV.showFirstSeg);
        drawFirstLineBox.setToolTipText("Whether to draw the first line segment of a graph or not.");
        drawFirstLineBox.setFont(drawFirstLineBox.getFont().deriveFont(12f));
        drawFirstLineBox.addActionListener(e ->
        {
            DV.showFirstSeg = drawFirstLineBox.isSelected();
            if (DV.trainData != null)
                DataVisualization.drawGraphs();
        });
        
        return drawFirstLineBox;
    }


    /**
     * Select whether to visualize midpoints in a GLC-L visualization or not
     * @return check box
     */
    private JCheckBox drawMidpointsBox()
    {
        JCheckBox drawMidpointsBox = new JCheckBox("Midpoints", DV.showFirstSeg);
        drawMidpointsBox.setToolTipText("Whether to draw midpoints when two angles are equal in a graph or not.");
        drawMidpointsBox.setFont(drawMidpointsBox.getFont().deriveFont(12f));
        drawMidpointsBox.addActionListener(e ->
        {
            DV.showMidpoints = drawMidpointsBox.isSelected();
            if (DV.trainData != null)
                DataVisualization.drawGraphs();
        });

        return drawMidpointsBox;
    }


    /**
     * Creates button to visualize graphs in a different window
     * @return button
     */
    private JButton separateVisButton()
    {
        JButton separateVisBtn = new JButton("Visualization Window");
        separateVisBtn.setToolTipText("Open another window displaying the visualization");
        separateVisBtn.setFont(separateVisBtn.getFont().deriveFont(12f));
        separateVisBtn.addActionListener(e ->
        {
            if (!DV.displayRemoteGraphs)
            {
                DV.displayRemoteGraphs = true;

                JOptionPane optionPane = new JOptionPane(DV.remoteGraphPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                JDialog dialog = optionPane.createDialog(DV.mainFrame, "Visualization");
                dialog.setSize(Resolutions.dvWindow[0] / 2, Resolutions.dvWindow[1] / 2);
                dialog.setResizable(true);
                dialog.setModal(false);
                dialog.setVisible(true);
                dialog.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosing(WindowEvent e)
                    {
                        DV.displayRemoteGraphs = false;
                        DV.remoteGraphPanel.removeAll();
                    }
                });

                if (DV.trainData != null)
                    DataVisualization.drawGraphs();
            }
        });

        return separateVisBtn;
    }


    /**
     * Gets the number of active attributes
     */
    private int getActiveAttributes()
    {
        int active = 0;
        for (int i = 0; i < DV.activeAttributes.size(); i++)
        {
            if (DV.activeAttributes.get(i))
                active++;
        }

        return active;
    }
}
