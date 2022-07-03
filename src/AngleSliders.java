import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class AngleSliders
{
    /**
     * Creates panel for a features angle/weight slider
     * @param fieldName name of feature to be weighted
     * @param angle weight of feature
     * @param index index of feature
     */
    public static void createSliderPanel(String fieldName, int angle, int index)
    {
        // text for angle
        JTextField angleText = new JTextField(4);
        angleText.setToolTipText("Change angles of visualization");
        angleText.setFont(angleText.getFont().deriveFont(12f));
        angleText.setText(Double.toString(angle / 100.0));

        // slider for angle
        JSlider angleSlider = new JSlider(0, 18000, angle);
        angleSlider.setToolTipText("Change angles of visualization");
        angleSlider.setMinorTickSpacing(1000);
        angleSlider.setPaintTicks(true);
        angleSlider.setPaintLabels(true);

        // label for angle
        JLabel angleLabel = new JLabel(fieldName + " Angle: " + angle / 100.0);
        angleLabel.setToolTipText("Change angles of visualization");

        // create table for slider labels
        Hashtable<Integer, JLabel> position = new Hashtable<>();
        position.put(0, new JLabel("0"));           // 0 degrees
        position.put(3000, new JLabel("30"));       // 30 degrees
        position.put(6000, new JLabel("60"));       // 60 degrees
        position.put(9000, new JLabel("90"));       // 90 degrees
        position.put(12000, new JLabel("120"));     // 120 degrees
        position.put(15000, new JLabel("150"));     // 150 degrees
        position.put(18000, new JLabel("180"));     // 180 degrees
        angleSlider.setLabelTable(position);

        // add listeners for text field and slider
        // action listener for text field
        angleText.addActionListener(e ->
        {
            // get raw value of inputted angle
            double fieldAngle = Double.parseDouble(angleText.getText());

            // transform inputted angle to a slider value
            int sliderValue = (int) (fieldAngle * 100);

            // check if angle is within 0 and 180 degrees
            if (sliderValue >= 0 && sliderValue <= 18000)
            {
                try
                {
                    // set slider to new value
                    angleSlider.setValue(sliderValue);

                    // set angle label to new value
                    angleLabel.setText(fieldName + " Angle: " + fieldAngle);

                    // set angle to new value
                    DV.angles[index] = fieldAngle;
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Illegal Input");
                }

                // redraw graphs
                DataVisualization.drawGraphs(0);
            }
        });

        // change listener for slider
        angleSlider.addChangeListener(e ->
        {
            // get new angle
            double fieldAngle = angleSlider.getValue() / 100.0;

            try
            {
                // set text field to new value
                angleText.setText(Double.toString(fieldAngle));

                // set angle label to new value
                angleLabel.setText(fieldName + " Angle: " + fieldAngle);

                // set angle to new value
                DV.angles[index] = fieldAngle;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw graphs
            DataVisualization.drawGraphs(0);
        });

        // main panel for angle
        // holds textField, slider, and label
        JPanel anglePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        anglePanel.add(angleSlider);
        anglePanel.add(angleText);
        anglePanel.add(angleLabel);

        // add to angle slider panel
        DV.angleSliderPanel.add(anglePanel);
    }
}
