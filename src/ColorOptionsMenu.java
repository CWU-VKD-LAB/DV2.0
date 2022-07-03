import javax.swing.*;
import java.awt.*;


public class ColorOptionsMenu extends JPanel
{
    public ColorOptionsMenu(Point mouseLocation)
    {
        super(new BorderLayout());

        // create popup window
        JFrame colorOptionsFrame = new JFrame("Color Options");
        colorOptionsFrame.setLocation(mouseLocation);

        // color panel
        JPanel colors = new JPanel();

        // set domain line color
        JButton domainLineColorBtn = new JButton("Domain Line Color");
        domainLineColorBtn.setToolTipText("Sets color of subset of utilized data lines");
        domainLineColorBtn.addActionListener(e ->
        {
            Color newColor = JColorChooser.showDialog(
                    colorOptionsFrame,
                    "Chose Domain Line Color",
                    colorOptionsFrame.getBackground());

            if (newColor != null)
                DV.domainLines = newColor;

            if (DV.data != null)
                DataVisualization.drawGraphs(0);
        });
        colors.add(domainLineColorBtn);

        // set overlap line color
        JButton overlapLineColorBtn = new JButton("Overlap Line Color");
        overlapLineColorBtn.setToolTipText("Sets color of overlap lines");
        overlapLineColorBtn.addActionListener(e ->
        {
            Color newColor = JColorChooser.showDialog(
                    colorOptionsFrame,
                    "Chose Domain Line Color",
                    colorOptionsFrame.getBackground());

            if (newColor != null)
                DV.overlapLines = newColor;

            if (DV.data != null)
                DataVisualization.drawGraphs(0);
        });
        colors.add(overlapLineColorBtn);

        // set threshold line color
        JButton thresholdLineColorBtn = new JButton("Threshold Line Color");
        thresholdLineColorBtn.setToolTipText("Sets color of threshold line");
        thresholdLineColorBtn.addActionListener(e ->
        {
            Color newColor = JColorChooser.showDialog(
                    colorOptionsFrame,
                    "Chose Domain Line Color",
                    colorOptionsFrame.getBackground());

            if (newColor != null)
                DV.thresholdLine = newColor;

            if (DV.data != null)
                DataVisualization.drawGraphs(0);
        });
        colors.add(thresholdLineColorBtn);

        // set background color
        JButton backgroundColorBtn = new JButton("Background Color");
        backgroundColorBtn.setToolTipText("Sets color of graph background");
        backgroundColorBtn.addActionListener(e ->
        {
            Color newColor = JColorChooser.showDialog(
                    colorOptionsFrame,
                    "Chose Background Color",
                    colorOptionsFrame.getBackground());

            if (newColor != null)
                DV.background = newColor;

            if (DV.data != null)
                DataVisualization.drawGraphs(0);
        });
        colors.add(backgroundColorBtn);

        // set upper graph color
        JButton upperGraphColorBtn = new JButton("Upper Graph Color");
        upperGraphColorBtn.setToolTipText("Sets color of upperGraph");
        upperGraphColorBtn.addActionListener(e ->
        {
            Color newColor = JColorChooser.showDialog(
                    colorOptionsFrame,
                    "Chose Domain Line Color",
                    colorOptionsFrame.getBackground());

            if (newColor != null)
                DV.graphColors[0] = newColor;

            if (DV.data != null)
                DataVisualization.drawGraphs(0);
        });
        colors.add(upperGraphColorBtn);

        // set lower graph color
        JButton lowerGraphColorBtn = new JButton("Lower Graph Color");
        lowerGraphColorBtn.setToolTipText("Sets color of lower graph");
        lowerGraphColorBtn.addActionListener(e ->
        {
            Color newColor = JColorChooser.showDialog(
                    colorOptionsFrame,
                    "Chose Domain Line Color",
                    colorOptionsFrame.getBackground());

            if (newColor != null)
                DV.graphColors[1] = newColor;

            if (DV.data != null)
                DataVisualization.drawGraphs(0);
        });
        colors.add(lowerGraphColorBtn);

        colorOptionsFrame.add(colors);
        colorOptionsFrame.pack();
        colorOptionsFrame.setVisible(true);
    }
}
