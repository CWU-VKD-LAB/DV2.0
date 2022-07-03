import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class VisOptionsMenu extends JPanel
{
    /**
     * Creates Visualization Options Menu on mouseLocation
     * @param mouseLocation location to create menu on
     */
    public VisOptionsMenu(Point mouseLocation)
    {
        super(new BorderLayout());

        // create popup window
        JFrame visOptionsFrame = new JFrame("Color Options");
        visOptionsFrame.setLocation(mouseLocation);

        // choose class to visualize as main
        JPanel chooseUpperClassPanel = new JPanel();
        JButton chooseUpperClassBtn = new JButton("Upper Class");
        chooseUpperClassBtn.setToolTipText("Choose class to be visualized on upper graph");
        chooseUpperClassBtn.addActionListener(e ->
        {
            int chosen = JOptionPane.showOptionDialog(
                    visOptionsFrame,
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
                DV.prevCM.clear();
                DV.prevAllDataClassifications.clear();

                // set upper class
                DV.upperClass = chosen;

                // lower class gets all others
                for (int i = 0; i < DV.classNumber; i++)
                {
                    if (i != chosen)
                        DV.lowerClasses.set(i, true);
                    else
                        DV.lowerClasses.set(i, false);
                }

                // optimize setup then draw graphs
                DataVisualization.optimizeSetup();
                DataVisualization.drawGraphs(0);

                visOptionsFrame.dispatchEvent(new WindowEvent(visOptionsFrame, WindowEvent.WINDOW_CLOSING));
            }
        });
        chooseUpperClassPanel.add(chooseUpperClassBtn);

        // specify visualization
        JPanel specifyVisPanel = new JPanel();
        JButton specifyVisBtn = new JButton("Specify Visualization");
        specifyVisBtn.setToolTipText("Removes one class from the lower graph");
        specifyVisBtn.addActionListener(e ->
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
                        visOptionsFrame,
                        removablePanel,
                        "Remove Class",
                        JOptionPane.OK_CANCEL_OPTION);

                if (choice == 0)
                {
                    // add previous analytics
                    DV.prevCM.add(DV.allDataCM);
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
                    DataVisualization.drawGraphs(0);
                }
            }
            else
            {
                JOptionPane.showMessageDialog(visOptionsFrame, "Classes cannot be further separated.");
            }
        });
        specifyVisPanel.add(specifyVisBtn);

        // visualize overlap area
        JPanel visOverlapPanel = new JPanel();
        JButton visOverlapBtn = new JButton("Visualize Overlap");
        visOverlapBtn.setToolTipText("Visualize the overlap area");
        visOverlapBtn.addActionListener(e ->
        {
            if (DV.classNumber > 1 && DV.accuracy < 100)
            {
                DV.drawOverlap = true;
                DataVisualization.drawGraphs(0);
            }
            else
                JOptionPane.showMessageDialog(visOptionsFrame, "No overlap area");
        });
        visOverlapPanel.add(visOverlapBtn);

        // stop visualizing overlap
        JPanel stopOverlapVisPanel = new JPanel();
        JButton stopOverlapVisBtn = new JButton("Stop Visualizing Overlap");
        stopOverlapVisBtn.setToolTipText("Visualize all data");
        stopOverlapVisBtn.addActionListener(e ->
        {
            DV.drawOverlap = false;
            DataVisualization.drawGraphs(0);
        });
        stopOverlapVisPanel.add(stopOverlapVisBtn);

        // options panel
        JPanel visOptions = new JPanel();
        visOptions.add(chooseUpperClassPanel);

        // if there are more than two classes
        visOptions.add(specifyVisPanel);

        // check if not drawing overlap
        if (DV.classNumber > 1 && !DV.drawOverlap)
            visOptions.add(visOverlapPanel);

        // if drawing overlap
        if (DV.classNumber > 1 && DV.drawOverlap)
            visOptions.add(stopOverlapVisPanel);

        visOptionsFrame.add(visOptions);
        visOptionsFrame.pack();
        visOptionsFrame.setVisible(true);
    }
}
