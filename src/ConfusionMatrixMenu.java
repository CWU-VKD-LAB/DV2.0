import javax.swing.*;
import java.awt.*;

public class ConfusionMatrixMenu extends JPanel
{
    /**
     * Creates Confusion Matrix Menu on mouseLocation
     * @param mouseLocation location to create menu on
     */
    public ConfusionMatrixMenu(Point mouseLocation)
    {
        super(new BorderLayout());

        // create popup window
        JFrame analyticsOptionFrame = new JFrame("Analytics");
        analyticsOptionFrame.setLocation(mouseLocation);

        // confusion matrix panel
        JPanel analyticsPanel = new JPanel();

        // set previous confusion matrices
        JCheckBox prevAllDataCheckBox = new JCheckBox("Previous All Data CM", DV.allDataChecked);
        prevAllDataCheckBox.setToolTipText("Toggle previous all data confusion matrices");
        prevAllDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.prevAllDataChecked = !DV.prevAllDataChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        analyticsPanel.add(prevAllDataCheckBox);

        // set all data confusion matrix
        JCheckBox allDataCheckBox = new JCheckBox("All Data CM", DV.allDataChecked);
        allDataCheckBox.setToolTipText("Toggle all data confusion matrix");
        allDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.allDataChecked = !DV.allDataChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        analyticsPanel.add(allDataCheckBox);

        // set data without overlap confusion matrix
        JCheckBox withoutOverlapCheckBox = new JCheckBox("Without Overlap CM", DV.withoutOverlapChecked);
        withoutOverlapCheckBox.setToolTipText("Toggle without overlap confusion matrix");
        withoutOverlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.withoutOverlapChecked = !DV.withoutOverlapChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        analyticsPanel.add(withoutOverlapCheckBox);

        // set overlap confusion matrix
        JCheckBox overlapCheckBox = new JCheckBox("Overlap CM", DV.overlapChecked);
        overlapCheckBox.setToolTipText("Toggle overlap confusion matrix");
        overlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.overlapChecked = !DV.overlapChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        analyticsPanel.add(overlapCheckBox);

        // set worst case confusion matrix
        JCheckBox worstCaseCheckBox = new JCheckBox("Worst Case CM", DV.worstCaseChecked);
        worstCaseCheckBox.setToolTipText("Toggle worst case confusion matrix");
        worstCaseCheckBox.addActionListener(e->
        {
            // reverse check
            DV.worstCaseChecked = !DV.worstCaseChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        analyticsPanel.add(worstCaseCheckBox);

        // set user validation set confusion matrix
        JCheckBox userValCheckBox = new JCheckBox("User Validation CM", DV.userValidationChecked);
        userValCheckBox.setToolTipText("Toggle user validation confusion matrix");
        userValCheckBox.addActionListener(e->
        {
            // reverse check
            DV.userValidationChecked = !DV.userValidationChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        analyticsPanel.add(userValCheckBox);

        // set cross validation
        JCheckBox crossValCheckBox = new JCheckBox("Cross Validation", DV.userValidationChecked);
        crossValCheckBox.setToolTipText("Toggle user k-fold cross validation table");
        crossValCheckBox.addActionListener(e->
        {
            // reverse check
            DV.crossValidationChecked = !DV.crossValidationChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.crossValidationPanel.repaint();
            DV.graphPanel.revalidate();
            DV.crossValidationPanel.revalidate();
        });
        analyticsPanel.add(crossValCheckBox);

        JButton kFoldsButton = new JButton("k-folds");
        kFoldsButton.setToolTipText("Number for folds for cross validation");
        kFoldsButton.addActionListener(e ->
        {
            // popup asking for number of folds
            JPanel foldPanel = new JPanel(new BorderLayout());

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField foldField = new JTextField();
            foldField.setPreferredSize(new Dimension(30, 30));
            textPanel.add(new JLabel("Number of Folds: "));
            textPanel.add(foldField);

            // add text panel
            foldPanel.add(textPanel, BorderLayout.SOUTH);

            int choice = -2;
            // loop until folds are valid or user quits
            while (choice == -2)
            {
                choice = JOptionPane.showConfirmDialog(DV.mainFrame, foldPanel, "Enter the number of folds.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (choice == 0)
                {
                    try
                    {
                        // get text field values
                        int folds = Integer.parseInt(foldField.getText());

                        if (folds > 0)
                        {
                            // set folds
                            DV.kFolds = folds;

                            // regenerate confusion matrices
                            ConfusionMatrices.generateConfusionMatrices();

                            // revalidate graphs and confusion matrices
                            DV.graphPanel.repaint();
                            DV.crossValidationPanel.repaint();
                            DV.graphPanel.revalidate();
                            DV.crossValidationPanel.revalidate();
                        }
                        else
                        {
                            // invalid fold input
                            JOptionPane.showMessageDialog(
                                    DV.mainFrame,
                                    "Error: input is invalid.\n" +
                                            "Please enter a whole number greater than 0.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);

                            choice = -2;
                        }
                    }
                    catch (NumberFormatException nfe)
                    {
                        JOptionPane.showMessageDialog(DV.mainFrame, "Error: please enter a whole numerical value.", "Error", JOptionPane.ERROR_MESSAGE);
                        choice = -2;
                    }
                }
            }
        });
        analyticsPanel.add(kFoldsButton);

        analyticsOptionFrame.add(analyticsPanel);
        analyticsOptionFrame.pack();
        analyticsOptionFrame.setVisible(true);
    }
}
