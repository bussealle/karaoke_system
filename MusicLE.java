package musicle;

import java.awt.BasicStroke;
import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.sqrt;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jfree.chart.JFreeChart;
import jp.ac.kyoto_u.kuis.le4music.Le4MusicUtils;
import jp.ac.kyoto_u.kuis.le4music.CheckAudioSystem;
import jp.ac.kyoto_u.kuis.le4music.Player;
import jp.ac.kyoto_u.kuis.le4music.Plot;
import jp.ac.kyoto_u.kuis.le4music.Recorder;
import jp.ac.kyoto_u.kuis.le4music.SingleXYArrayDataset;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.MathArrays;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

public final class MusicLE extends JFrame implements ActionListener, KeyListener {

    private static final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    private static JFrame frame = new JFrame();
    private static File wavename;
    private static final JPanel mainPnl = new JPanel();
    private static final JPanel subPnl = new JPanel();
    private static final JPanel volPnl = new JPanel();
    private static double wavlength;
    private static Player player;
    private static Recorder recorder;
    private static final JLabel lbNote = new JLabel();
    private static final JLabel lbTime = new JLabel();
    private static JButton btnPlay;
    private static JButton btnOpen;
    private static JButton btnStop;
    private static JLabel lbLyric = new JLabel();
    private static File fpLyric;
    private static BufferedReader rdrLyric;
    private static StringBuffer bufLyric;
    private static File writeFile;

    private void initialize() throws IOException, UnsupportedAudioFileException, LineUnavailableException {

        //フレームの初期設定
        frame.setName("KARA-OK");
        frame.setBounds(0, 0, d.width, d.height);
        frame.setBackground(new Color(105, 153, 174));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //メインパネルの設定
        mainPnl.setBounds((int) (d.width * 0.025), (int) (d.height * 0.2), (int) (d.width * 0.95), (int) (d.height * 0.7));
        mainPnl.setBackground(new Color(255, 255, 240));
        mainPnl.add(new JLabel("OPENボタンからファイルを開いてください"));

        //再生ボタン、開くボタンの設定
        btnPlay = new JButton("PLAY");
        btnOpen = new JButton("OPEN");
        btnStop = new JButton("STOP");
        btnOpen.setFont(new Font("Meiryo", Font.BOLD, 25));
        btnPlay.setFont(new Font("Meiryo", Font.BOLD, 25));
        btnStop.setFont(new Font("Meiryo", Font.BOLD, 25));
        btnOpen.setBackground(new Color(255, 255, 240));
        btnPlay.setBackground(new Color(255, 255, 240));
        btnStop.setBackground(new Color(255, 255, 240));
        btnOpen.setBounds((int) (d.width * 0.07), (int) (d.height * 0.08), (int) (d.width * 0.09), (int) (d.height * 0.09));
        btnPlay.setBounds((int) (d.width * 0.17), (int) (d.height * 0.08), (int) (d.width * 0.09), (int) (d.height * 0.09));
        btnStop.setBounds((int) (d.width * 0.27), (int) (d.height * 0.08), (int) (d.width * 0.09), (int) (d.height * 0.09));
        btnPlay.addActionListener(this);
        btnStop.addActionListener(this);
        btnOpen.addActionListener(this);

        //フレームにボタンとパネルをセット
        frame.getContentPane().add(btnPlay);
        frame.getContentPane().add(btnOpen);
        frame.getContentPane().add(btnStop);
        frame.getContentPane().add(volPnl);
        frame.getContentPane().add(subPnl);
        frame.getContentPane().add(mainPnl);
        frame.getContentPane().setBackground(new Color(105, 153, 174));
        frame.getContentPane().setLayout(null);

        //Note表示用Label
        lbNote.setText("pitch   " + "N/A");
        lbNote.setFont(new Font("Meiryo", Font.BOLD, 30));
        lbNote.setBounds((int) (d.width * 0.8), (int) (d.height * 0.08), (int) (d.width * 0.3), (int) (d.height * 0.1));
        frame.getContentPane().add(lbNote);

        //Time表示用Label
        lbTime.setText("time left   " + "00:00");
        lbTime.setFont(new Font("Meiryo", Font.BOLD, 28));
        lbTime.setBounds((int) (d.width * 0.6), (int) (d.height * 0.08), (int) (d.width * 0.3), (int) (d.height * 0.1));
        frame.getContentPane().add(lbTime);

        //Lylic表示用Label
        lbLyric.setText("");
        lbLyric.setBackground(new Color(105, 158, 200));
        lbLyric.setFont(new Font("Meiryo", Font.ITALIC, 20));
        lbLyric.setBounds((int) (d.width * 0.025), (int) (d.height * 0.87), (int) (d.width * 0.95), (int) (d.height * 0.1));
        lbLyric.setHorizontalAlignment(JLabel.CENTER);
        frame.getContentPane().add(lbLyric);

        //vol表示用Panel
        volPnl.setBounds((int) (d.width * 0.975), (int) (d.height * 0.205), (int) (d.width * 0.02), (int) (d.height * 0.656));
        volPnl.setBackground(new Color(105, 158, 180));
    }
    
    
    //歌詞表示のためのキーイベント処理
    @Override
    public void keyPressed(KeyEvent e) {
        if (player != null && e.getKeyCode() == KeyEvent.VK_SPACE && player.isActive()) {//Spaceが押されたら
            if (rdrLyric != null){
                try {
                    String readline;
                    readline = rdrLyric.readLine();
                    //改行を読み飛ばす
                    while(readline != null && readline.equals("")){
                        readline = rdrLyric.readLine();
                    }
                    //歌詞の切り替わり時間を示す"//"があるかどうか
                    if (readline != null && readline.contains("//")){
                        lbLyric.setText(readline.substring(0, readline.indexOf("//")));
                    }
                    else if (readline != null && !readline.contains("//")){
                        lbLyric.setText(readline);
                        String writebuf = readline+"//"+player.position()+System.getProperty("line.separator");
                        bufLyric.append(writebuf);//バッファに書き込み
                        System.out.print(writebuf);
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }

        }
        //開始位置付歌詞ファイルの書き込み
        else if (recorder != null &&  !recorder.isActive() && e.getKeyCode() == KeyEvent.VK_SPACE) {
            FileWriter fw;
            try {
                if (bufLyric.toString().contains("//")) {
                    fw = new FileWriter(fpLyric);
                    fw.write(bufLyric.toString());
                    fw.close();
                    lbLyric.setText("Lyric data was written");
                }
            } catch (IOException ex) {
                Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        //再生ボタンなら
        if (e.getSource() == btnPlay) {
            //playerが開かれていなければ
            if (player == null) {
                JOptionPane.showMessageDialog(this, "wavファイルを開いてください");
                frame.requestFocus();
                return;
            } else if (player.isActive()) {
                frame.requestFocus();
                return;
            }
            /* ウインドウ描画がひと通り完了してから再生&録音を開始する */
            SwingUtilities.invokeLater(player::start);
            SwingUtilities.invokeLater(recorder::start);
            //歌詞Labelの初期化
            if (fpLyric != null && fpLyric.exists()) {
                lbLyric.setText("♪~");
            } else {
                lbLyric.setText("");
            }
        } //停止ボタンなら
        else if (e.getSource() == btnStop) {
            //playerが開かれていなければ
            if (player == null) {
                frame.requestFocus();
                return;
            } //playerが再生中ならば
            else if (player.isActive()) {
                player.stop();
                player = null;
                recorder.stop();
                try {
                    if (rdrLyric != null) {
                        rdrLyric.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } //開くボタンなら
        else if (e.getSource() == btnOpen) {
            //playerが実行中の時は
            if (player != null && (player.isRunning() || player.isActive())) {
                player.stop();
                recorder.stop();
                player = null;
                try {
                    rdrLyric.close();
                } catch (IOException ex) {
                    Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //ファイルオープンダイアログの生成
            JFileChooser filechooser = new JFileChooser();
            //.wavのフィルターを設定
            wavFilter filter = new wavFilter();
            filechooser.addChoosableFileFilter(filter);
            filechooser.setFileFilter(filter);

            int selected = filechooser.showOpenDialog(this);
            switch (selected) {
                case JFileChooser.APPROVE_OPTION:
                    //ファイルを取得
                    wavename = filechooser.getSelectedFile();
                     {
                        try {
                            //録音ファイルのファイル名を決定
                            String wavpath = wavename.getAbsolutePath();
                            wavpath = wavpath.substring(0, wavpath.indexOf(".wav")) + "_rec.wav";
                            writeFile = new File(wavpath);
                            int i = 1;
                            while (true) {
                                if (!writeFile.exists()) {
                                    break;
                                }
                                wavpath = wavpath.substring(0, wavpath.indexOf("_rec") + 4) + i + ".wav";
                                writeFile = new File(wavpath);
                                i++;
                            }

                            //新たなplayerとrecorderを生成
                            player = Player.newPlayer(wavename, Player.Default.bufferDuration, Le4MusicUtils.frameDuration, null);
                            recorder = Recorder.newRecorder(
                                    /* FrameRate= */16000,
                                    /* FrameDuration= */ 2.0,
                                    /*  MixerInfo=*/ null,
                                    /*File=*/ writeFile
                            );

                            //音声ファイルの長さを取得
                            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wavename);
                            AudioFormat format = audioInputStream.getFormat();
                            long frames = audioInputStream.getFrameLength();
                            wavlength = (frames + 0.0) / format.getFrameRate();
                            lbTime.setText("time left   " + sec2mmss(wavlength));

                            //パネル内コンポーネントの除去
                            if (mainPnl.getComponentCount() != 0) {
                                mainPnl.removeAll();
                                mainPnl.revalidate();
                                subPnl.removeAll();
                                subPnl.revalidate();
                            }

                            //スペクトログラム表示をセット
                            setSpectrogram();
                            //ピッチ表示をセット
                            setPitch();

                            //歌詞表示用のテキストファイルの読み込み
                            fpLyric = new File(wavename.getParent() + "/" + wavename.getName().substring(0, wavename.getName().indexOf(".wav"))+".txt");
                            if (fpLyric != null && fpLyric.exists()) {
                                lbLyric.setText("Lyric data is loaded.");
                                rdrLyric = new BufferedReader(new FileReader(fpLyric));
                                bufLyric = new StringBuffer();
                            } else {
                                lbLyric.setText("There are't lyric data.");
                            }
                        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | ParseException ex) {
                            Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    break;

                case JFileChooser.CANCEL_OPTION:
                    break;

                case JFileChooser.ERROR_OPTION:
                    JOptionPane.showMessageDialog(this, "ファイルが開けません");
                    break;

                default:
                    break;
            }
        }
        frame.requestFocus();
    }

    public MusicLE() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        initialize();
    }

    public static void main(String[] args) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        CheckAudioSystem.main(null);
        MusicLE window = new MusicLE();
        frame.addKeyListener(window);
        frame.setFocusable(true);

        EventQueue.invokeLater(() -> {
            MusicLE.frame.setVisible(true);
            frame.requestFocus();
        });

    }

    /* スペクトログラム表示をセット */
    public static void setSpectrogram() throws IOException, ParseException, UnsupportedAudioFileException, LineUnavailableException {

        JFreeChart spChart = PlayMonitorSpectrogram2.main(player);
        spChart.setBackgroundPaint(new Color(105, 153, 174));
        mainPnl.setLayout(new BorderLayout());
        mainPnl.add(Plot.createChartPanel(spChart));
        mainPnl.setBounds((int) (d.width * 0.025), (int) (d.height * 0.2), (int) (d.width * 0.95), (int) (d.height * 0.7));

    }

    /* 音高のリアルタイム表示をセット */
    public static void setPitch() throws IOException {

        double windowDuration = 0.04;
        double windowShift = 0.02;
        double RMScut = -37.0;

        /* 窓 関 数 と FFTのサンプル数 */
        final int windowSize = (int) Math.round(windowDuration * recorder.getSampleRate());
        final int fftSize = 1 << Le4MusicUtils.nextPow2(windowSize);
        /* シフトのサンプル数 */
        final int shiftSize = (int) Math.round(windowShift * recorder.getSampleRate());
        /* 窓関数を求め,それを正規化する */
        final double[] window = MathArrays.normalizeArray(
                Arrays.copyOf(Le4MusicUtils.hanning(windowSize), fftSize), 1.0
        );

        System.out.println(windowSize);
        System.out.println(fftSize);
        System.out.println(shiftSize);
        System.out.println(new Rectangle((int) (d.width * 0.025), (int) (d.height * 0.2), (int) (d.width * 0.95), (int) (d.height * 0.7)));

        final JFreeChart pcChart = ChartFactory.createXYLineChart(
                /* title      = */null,
                /* xAxisLabel = */ null,
                /* yAxisLabel = */ null,
                /* dataset    = */ null
        );

        /* チャートの処理(背景の透明化)*/
        pcChart.removeLegend();
        pcChart.setBackgroundPaint(new Color(255, 255, 255, 0)); //透明
        pcChart.setBorderVisible(false);
        ChartPanel chpnl = Plot.createChartPanel(pcChart);
        chpnl.setOpaque(false);
        /* 透明パネルにChartをセット */
        subPnl.setLayout(new BorderLayout());
        subPnl.setOpaque(false);
        subPnl.add(chpnl);
        
        subPnl.setBounds((int) (d.width * 0.025), (int) (d.height * 0.2), (int) (d.width * 0.95), (int) (d.height * 0.7));
        /*  プロットの処理 (背景の透明化、線の非表示)*/
        final XYPlot pcPlot = pcChart.getXYPlot();
        pcPlot.setBackgroundAlpha(0.2f);
        pcPlot.setBackgroundPaint(new Color(255, 255, 255, 0));
        pcPlot.setOutlinePaint(new Color(255, 255, 255, 0));
        pcPlot.setOutlineVisible(false);
        pcPlot.setRangeGridlinesVisible(false);
        pcPlot.setDomainGridlinesVisible(false);
        pcPlot.setInsets(new RectangleInsets(0, 50, 36, 9));
        /* 線の太さの設定 */
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) pcPlot.getRenderer();
        renderer.setSeriesStroke(0, new BasicStroke(4));
        renderer.setSeriesPaint(0, new Color(144, 238, 144));
        /* 軸の設定&透明化 */
        final NumberAxis yAxis = (NumberAxis) pcPlot.getRangeAxis();
        final NumberAxis xAxis = (NumberAxis) pcPlot.getDomainAxis();
        yAxis.setRange(0.0, 1000);
        xAxis.setRange(-recorder.getFrameDuration(), 0.0);
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);
        xAxis.setUpperBound(0.0);
        yAxis.setLowerBound(0.0);
        yAxis.setAutoRangeIncludesZero(false);
        xAxis.setVisible(false);
        yAxis.setVisible(false);

        /*  vol用のchart*/
        DefaultCategoryDataset voldata = new DefaultCategoryDataset();
        JFreeChart volChart = ChartFactory.createBarChart("", "", "", voldata, PlotOrientation.VERTICAL, true, false, false);
        volChart.removeLegend();
        volChart.setBackgroundPaint(new Color(255, 255, 255, 0));
        volChart.setBorderVisible(false);
        CategoryPlot volPlot = (CategoryPlot) volChart.getPlot();
        BarRenderer barRenderer = ((BarRenderer) volPlot.getRenderer());
        barRenderer.setShadowVisible(false);
        barRenderer.setSeriesVisibleInLegend(false);
        volPlot.setOutlineVisible(false);
        volPlot.setBackgroundPaint(new Color(255, 255, 255, 0));
        volPlot.setOutlinePaint(new Color(255, 255, 255, 0));
        volPlot.setInsets(RectangleInsets.ZERO_INSETS);
        volPlot.setDomainGridlinesVisible(false);
        volPlot.setRangeGridlinesVisible(false);
        volPlot.getDomainAxis().setVisible(false);
        volPlot.getRangeAxis().setVisible(false);
        volPlot.getRangeAxis().setRange(0.0, 80.0);
        volPnl.setLayout(new BorderLayout());
        ChartPanel volChartPnl = Plot.createChartPanel(volChart);
        volChartPnl.setOpaque(false);
        volPnl.add(volChartPnl);

        final ScheduledExecutorService executor
                = Le4MusicUtils.newSingleDaemonThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(
                () -> {
                    if (!recorder.isUpdated()) {
                        return;
                    }
                    /* 最新フレーム */
                    final double[] fr = recorder.latestFrame();

                    /* 短時間フーリエ変換本体  */
                    final Stream<Complex[]> stft_stream
                    = Le4MusicUtils.sliding(fr, window, shiftSize)
                    .map(f -> Le4MusicUtils.rfft(f));

                    /* パワースペクトルの導出*/
                    final double[][] wkValue
                    = stft_stream.map(sp -> Arrays.stream(sp)
                            .mapToDouble(c -> c.abs())
                            .toArray())
                    .toArray(n -> new double[n][]);

                    /* 高周波ノイズ除去のため有効周波数の9/10をCutoff*/
                    for (int i = 0; i < wkValue.length; i++) {
                        for (int j = (int) (wkValue[i].length * 0.1); j < wkValue[i].length; j++) {
                            wkValue[i][j] = 0.0;
                        }
                    }

                    /* 時間軸 */
                    final double[] times
                    = IntStream.rangeClosed(-wkValue.length + 1, 0)
                    .mapToDouble(i -> i * windowShift)
                    .toArray();

                    final double[] f0 = new double[wkValue.length];//基本周波数

                    for (int i = 0; i < wkValue.length; i++) {

                        /* windowで切り出されたフレーム内RMSの導出 */
                        double rms_sum = 0.0;
                        for (int j = 0; j < wkValue[i].length; j++) {
                            rms_sum = rms_sum + wkValue[i][j];
                        }
                        double rms = 20.0 * Math.log10(sqrt(rms_sum / wkValue[i].length));
                        //音量表示のためのデータをセット
                        voldata.setValue(80 + rms - 10, "", "v");

                        /* RMSが規定値より小さければピッチ検出を行わない*/
                        if (rms < RMScut) {
                            f0[i] = 0.0;
                        } else {
                            /* Wiener-Khintchinの定理より自己相関関数を求める */
                            final Complex[] comp_tmp = Le4MusicUtils.ifft(Arrays.copyOf(wkValue[i], fftSize));
                            final double[] dbl_tmp = new double[(int) (comp_tmp.length / 2)];

                            double std = comp_tmp[0].getReal();
                            for (int j = 0; j < dbl_tmp.length; j++) {
                                dbl_tmp[j] = comp_tmp[j].getReal() / std; //正規化
                            }

                            int begin = 0;
                            for (int j = begin; j < dbl_tmp.length - 1; j++) {
                                if (dbl_tmp[j] < dbl_tmp[j + 1]) {
                                    begin = j; //初めて増加に転じる点
                                    break;
                                }
                            }
                            //以後の部分列から最大値のindexを求める
                            double[] subArray = Arrays.copyOfRange(dbl_tmp, begin, dbl_tmp.length);
                            int index = Le4MusicUtils.argmax(subArray) + begin;
                            //基本周波数の格納
                            if (index == 0) {
                                f0[i] = 0.0;
                            } else {
                                f0[i] = recorder.getSampleRate() / index;
                            }
                        }
                    }

                    /* Note表示のための処理*/
                    double fsum = 0.0;
                    for (int i = 0; i < f0.length; i++) {
                        fsum = fsum + f0[i];
                    }
                    if (fsum == 0.0) {//入力のないばあい
                        lbNote.setText("pitch   " + "N/A");
                    } else {//入力のあるばあい
                        lbNote.setText("pitch   " + midi2note(Le4MusicUtils.hz2nn(fsum / (double) f0.length)));
                        //lbNote.setText(""+(fsum / (double) f0.length));
                    }

                    //pitchデータ格納
                    pcPlot.setDataset(new SingleXYArrayDataset(times, f0));

                    //再生中なら
                    if (player != null && player.isActive()) {
                        //残り時間の表示
                        lbTime.setText("time left   " + sec2mmss(wavlength - (player.position() + 0.0) / player.getSampleRate()));
                        try {
                            //歌詞の表示
                            if (rdrLyric != null) {
                                rdrLyric.mark(1024);
                                String readline = rdrLyric.readLine();
                                if (readline != null && readline.contains("//")) {
                                    int lyrictime = Integer.parseInt(readline.substring(readline.indexOf("//") + 2, readline.length()));
                                    if (player.position() >= lyrictime) {
                                        lbLyric.setText(readline.substring(0, readline.indexOf("//")));
                                    } else {
                                        rdrLyric.reset();
                                    }
                                } else {
                                    rdrLyric.reset();
                                }
                            }
                            
                        } catch (IOException ex) {
                            Logger.getLogger(MusicLE.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        player.stop();
                        recorder.stop();
                        player = null;
                    }
                    

                },
                /* initialDelay = */ 0L,
                /* delay = */ 10L,
                TimeUnit.MILLISECONDS
        );
    }

    /* MIDIナンバーからNoteへの変換 */
    public static String midi2note(double midi) {
        if ((int) (midi + 0.5) < 12) {
            return "N/A";
        }
        String num = Integer.toString((int) (midi + 0.5) / 12 - 1);
        String alpha = "";
        switch ((int) (midi + 0.5) % 12) {
            case 0:
                alpha = "C";
                break;
            case 1:
                alpha = "Db";
                break;
            case 2:
                alpha = "D";
                break;
            case 3:
                alpha = "Eb";
                break;
            case 4:
                alpha = "E";
                break;
            case 5:
                alpha = "F";
                break;
            case 6:
                alpha = "Gb";
                break;
            case 7:
                alpha = "G";
                break;
            case 8:
                alpha = "Ab";
                break;
            case 9:
                alpha = "A";
                break;
            case 10:
                alpha = "Bb";
                break;
            case 11:
                alpha = "B";
                break;
        }
        return alpha + num;
    }

    /* 秒から00:00:形式で表示 */
    public static String sec2mmss(double sec) {
        String mm_s;
        String ss_s;
        int mm = (int) sec / 60;
        int ss = (int) sec % 60;
        if (mm < 10) {
            mm_s = "0" + mm;
        } else {
            mm_s = "" + mm;
        }
        if (ss < 10) {
            ss_s = "0" + ss;
        } else {
            ss_s = "" + ss;
        }
        return mm_s + ":" + ss_s;
    }
}
