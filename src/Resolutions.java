public class Resolutions
{
    public static int[] dvWindow = new int[]{1225, 900};
    public static int[] sliderPanel = new int[]{160, 650};
    public static int[] chartPanel = new int[]{1000, 630};
    public static int[] graphDomainPanel = new int[]{1000, 650};
    public static int[] anglesPane = new int[]{190, 650};
    public static int[] domainSlider = new int[]{950, 20};
    public static int[] confusionMatrixPane = new int[]{1200, 150};
    public static int[] singleChartPanel = new int[]{950, 270};

    /**
     * Sizes for portions of the DV Program for various screen sizes
     * @param resolution screen size
     */
    public static void setResolution(int resolution)
    {
        // 1920x1080
        if (resolution == 0)
        {
            dvWindow = new int[]{1920, 1080};
            sliderPanel = new int[]{240, 750};
            chartPanel = new int[]{1560, 750};
            graphDomainPanel = new int[]{1560, 770};
            anglesPane = new int[]{300, 750};
            domainSlider = new int[]{1480, 20};
            confusionMatrixPane = new int[]{1870, 180};
            singleChartPanel = new int[]{1480, 320};
        }
        // 1280x720
        else if(resolution == 1)
        {
            dvWindow = new int[]{1280, 720};
            sliderPanel = new int[]{190, 510};
            chartPanel = new int[]{1040, 500};
            graphDomainPanel = new int[]{1040, 510};
            anglesPane = new int[]{200, 510};
            domainSlider = new int[]{990, 15};
            confusionMatrixPane = new int[]{1250, 120};
            singleChartPanel = new int[]{990, 215};
        }
    }
}
