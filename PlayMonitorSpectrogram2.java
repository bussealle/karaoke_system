package musicle;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.MathArrays;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.general.DatasetChangeEvent;

import jp.ac.kyoto_u.kuis.le4music.Le4MusicUtils;
import jp.ac.kyoto_u.kuis.le4music.Player;
import jp.ac.kyoto_u.kuis.le4music.ColorMapIntARGB;

import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import static jp.ac.kyoto_u.kuis.le4music.Le4MusicUtils.nn2hz;
import org.apache.commons.cli.ParseException;

public final class PlayMonitorSpectrogram2 {
    
    public static final class Default {
        public static final double duration = 7.0;   /* seconds */
        public static final double freqMax = 1000.0; /* Hz */
        public static final double specLogMin = -70.0;
        public static final double specLogMax = -15.0;
        private static final int maxNote = 65; /* Max note*/
        private static final int minNote = 50; /* Min note */
        
    }
    
    private static final class OnlineSpectrogramPlot extends XYPlot {
        
        private static final long serialVersionUID = 3023546826798901921L;
        
        
        private final DatasetChangeEvent event = new DatasetChangeEvent(this, null);
        
        private final int frames;
        private final int bins;
        private final ColorMapIntARGB colormap;
        
        private final BufferedImage image;
        private final int[] dataElements;
        
        private OnlineSpectrogramPlot(
                                      final NumberAxis timeAxis,
                                      final NumberAxis freqAxis,
                                      final int frames,
                                      final int bins,
                                      final ColorMapIntARGB colormap
                                      ) {
            super(
                  /* dataset    = */ null,
                  /* domainAxis = */ timeAxis,
                  /* rangeAxis  = */ freqAxis,
                  /* renderer   = */ null
                  );
            this.frames = frames;
            this.bins = bins;
            this.colormap = colormap;
            this.image = new BufferedImage(
                                           /* width      = */ frames,
                                           /* height     = */ bins,
                                           /* imageType  = */ BufferedImage.TYPE_INT_ARGB
                                           );
            this.dataElements = new int[frames * bins];
            
            setDataset(new DefaultXYDataset()); /* render() を呼び出させるためのダミー */
        }
        
        public final void addSpectrum(final Complex[] spectrum, final double[] noteList, double freqtoIndex) {

            final double[] specPower = Arrays.stream(spectrum)
                    .limit(bins)
                    .mapToDouble(Complex::abs)
                    .toArray();
            /* 主旋律抽出の為の処理 */
           final double[] noteSum = new double[noteList.length];
            for (int i=0; i < noteSum.length; i++){
                noteSum[i] =0.0;
            }
            for (int i=0; i < noteList.length; i++){
                double freq = noteList[i];
                int over = 2; //整数倍音
                while(true){
                    if (freq*freqtoIndex > specPower.length){
                        break;
                    }
                    noteSum[i] = noteSum[i] + specPower[(int)(freq * freqtoIndex)] / (over-1);//重み付け
                    freq = noteList[i] * over;
                    over++;
                }
            }
            double f0 = noteList[Le4MusicUtils.argmax(noteSum)];
            /* 見やすさのため幅をもたせて白で描画する */
            specPower[(int)(f0 * freqtoIndex)] = 1.0;
            specPower[(int)(f0 * freqtoIndex) + 1] = 1.0;
            specPower[(int)(f0 * freqtoIndex) - 1] = 1.0;
            specPower[(int)(f0 * freqtoIndex) + 2] = 1.0;
            specPower[(int)(f0 * freqtoIndex) - 2] = 1.0;
            
            final int[] specImg = Arrays.stream(specPower)
            .limit(bins)
            .map(x -> 20.0 * Math.log10(x))
            .mapToInt(colormap::map)
            .toArray();
            for (int i = 0; i < bins; i++) {
                System.arraycopy(dataElements, (i * frames) + 1,
                                 dataElements, (i * frames) + 0,
                                 frames - 1);
                dataElements[(i * frames) + frames - 1] = specImg[bins - 1 - i];
            }
            datasetChanged(event); /* render() を呼び出す */
        }
        
        @Override /* XYPlot */
        public final boolean render(
                                    final Graphics2D g2,
                                    final Rectangle2D dataArea,
                                    final int index,
                                    final org.jfree.chart.plot.PlotRenderingInfo info,
                                    final org.jfree.chart.plot.CrosshairState crosshairState
                                    ) {
            image.getRaster().setDataElements(
                                              /* x = */ 0,
                                              /* y = */ 0,
                                              /* w = */ frames,
                                              /* h = */ bins,
                                              /* inData = */ dataElements
                                              );
            return g2.drawImage(
                                /* img   = */ image,
                                /* xform = */ new AffineTransform(
                                                                  /* m00 = */ dataArea.getWidth() / frames,
                                                                  /* m10 = */ 0.0,
                                                                  /* m01 = */ 0.0,
                                                                  /* m11 = */ dataArea.getHeight() / bins,
                                                                  /* m02 = */ dataArea.getX(),
                                                                  /* m12 = */ dataArea.getY()
                                                                  ),
                                /* obs   = */ null
                                );
        }
        
    }
    
    public final JFreeChart run(Player player)
    throws IOException,
    ParseException,
    UnsupportedAudioFileException,
    LineUnavailableException {
        
        /* シフト長＝フレーム長の8分の1 */
        final double shiftDuration = Le4MusicUtils.frameDuration / 8.0;
        final double duration = Default.duration;
        final double freqMax =  Default.freqMax;
        
        
        final int frames = (int)(duration / shiftDuration);
        final double[] times =
        IntStream.rangeClosed(-frames + 1, 0)
        .mapToDouble(i -> i * shiftDuration)
        .toArray();
        
        final int fftSize = 1 << Le4MusicUtils.nextPow2(player.getFrameSize());
        final int bins = (int)(fftSize * freqMax / player.getSampleRate());
        final double[] window =
        MathArrays.normalizeArray(Le4MusicUtils.hanning(player.getFrameSize()), 1.0);
        final double[] freqs =
        IntStream.range(0, bins)
        .mapToDouble(i -> i * player.getSampleRate() / fftSize)
        .toArray();
        
        final NumberAxis timeAxis = new NumberAxis("Time-index");
        timeAxis.setLowerMargin(0.0);
        timeAxis.setUpperMargin(0.0);
        
        final NumberAxis freqAxis = new NumberAxis("Frequency-index");
        freqAxis.setLowerMargin(0.0);
        freqAxis.setUpperMargin(0.0);
        
        final NumberAxis timeAxis1 = new NumberAxis("Time [sec.]");
        timeAxis1.setLowerBound((-frames + 1) * shiftDuration);
        timeAxis1.setUpperBound(0.0);
        
        final NumberAxis freqAxis1 = new NumberAxis("Frequency [Hz]");
        freqAxis1.setLowerBound(0.0);
        freqAxis1.setUpperBound(freqMax);
        
        final NumberAxis freqAxis2 = new NumberAxis("Frequency [Hz]");
        freqAxis2.setLowerBound(0.0);
        freqAxis2.setUpperBound(freqMax);
        
        final ColorMapIntARGB colormap =
        Le4MusicUtils.hotColorMap(Default.specLogMin, Default.specLogMax);
        final OnlineSpectrogramPlot spgPlot =
        new OnlineSpectrogramPlot(timeAxis, freqAxis, frames, bins, colormap);
        final String axisType = "NORMAL"; /* NORMAL | INDEX | BOTH */
        switch (axisType) {
            case "INDEX":
                break;
            case "BOTH":
                spgPlot.setDomainAxis(1, timeAxis1);
                spgPlot.setDomainAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);
                spgPlot.setRangeAxis(1, freqAxis1);
                spgPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);
                break;
            case "NORMAL":
                spgPlot.setDomainAxis(1, timeAxis1);
                spgPlot.setDomainAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);
                spgPlot.setRangeAxis(1, freqAxis1);
                spgPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);
                spgPlot.getDomainAxis().setVisible(false);
                spgPlot.getRangeAxis().setVisible(false);
                break;
        }
        
        final JFreeChart spgChart = new JFreeChart(spgPlot);
        spgChart.removeLegend();
        
        final double[] noteList = IntStream.rangeClosed(Default.minNote*10, Default.maxNote*10)
                    .mapToDouble(i -> (double)i / 10.0)
                    .map(dbl -> nn2hz(dbl))
                    .toArray();

        
        /////* スレッド処理 */////
        final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(
                                        () -> {
                                           if (!player.isUpdated()){
                                                return;
                                            }
                                            final double[] frame = player.latestFrame();
                                            final double[] wframe = MathArrays.ebeMultiply(frame, window);
                                            final Complex[] spectrum = Le4MusicUtils.rfft(Arrays.copyOf(wframe, fftSize));
                                            spgPlot.addSpectrum(spectrum, noteList, fftSize/player.getSampleRate());
                                            
                                            /* Playerの再生停止に合わせて描画も停止 */
                                            if (!player.isOpen()) executor.shutdown();
                                        },
                                        /* initialDelay = */ 0L,
                                        /* period       = */ (long)(shiftDuration * 1000000),
                                        TimeUnit.MICROSECONDS
                                        );
        return spgChart;
    }
    
    public static final JFreeChart main(Player player)
    throws IOException,
    ParseException,
    UnsupportedAudioFileException,
    LineUnavailableException {
        return new PlayMonitorSpectrogram2().run(player);
    }
    
}