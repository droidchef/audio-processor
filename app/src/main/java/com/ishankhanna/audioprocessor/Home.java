package com.ishankhanna.audioprocessor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ishankhanna.audioprocessing.fftutils.FFT;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.NumberFormat;


public class Home extends Activity implements View.OnClickListener, SurfaceHolder.Callback{

    static final String TAG = "AudioProcessor";

    static final String TAG_START_RECORDING = "Start Rec.";
    static final String TAG_STOP_RECORDING = "Stop Rec.";
    static final String TAG_START_PLAYBACK = "Start Play.";
    static final String TAG_STOP_PLAYBACK = "Stop Play.";

    int RECORDER_SAMPLERATE = 44100;
    int MAX_FREQUENCY = RECORDER_SAMPLERATE/2;
    final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    final int PEAK_THRESH = 20;

    short[] buffer = null;
    int bufferReadResult = 0;
    AudioRecord audioRecord = null;
    boolean aRecStarted = false;
    int bufferSize = 2048;
    int minBufferSize = 0;
    float volume = 0;
    FFT fft = null;
    float[] fftRealArray = null;
    int mainFreq = 0;

    float drawScaleH = 1.5f;
    float drawScaleW = 1.0f;
    int drawStepW = 2;
    float maxFrequencyToDraw = 2500;
    int drawBaseLine = 0;

    TextView tv_status;
    Button bt_recording, bt_playback, bt_analyse;
    String outputFile;
    MediaRecorder myAudioRecorder;

    SurfaceView surfaceView;

    Paint paint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        tv_status = (TextView) findViewById(R.id.tv_status);
        bt_recording = (Button) findViewById(R.id.bt_recording);
        bt_playback = (Button) findViewById(R.id.bt_playback);
        bt_analyse = (Button) findViewById(R.id.bt_analyse);
        paint = new Paint();

        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.BLACK);

        bt_recording.setText(TAG_START_RECORDING);
        bt_playback.setText(TAG_START_PLAYBACK);
        bt_playback.setEnabled(false);

        bt_playback.setOnClickListener(this);
        bt_recording.setOnClickListener(this);
        bt_analyse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                plotValues();
            }
        });

        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";


    }

    public void startRecording(){

        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_WB);


        myAudioRecorder.setOutputFile(outputFile);

        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        bt_playback.setEnabled(false);
        bt_recording.setEnabled(true);
        bt_recording.setText(TAG_STOP_RECORDING);
        Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();

    }

    public void stopRecording(){
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder  = null;
        bt_playback.setEnabled(true);
        bt_recording.setEnabled(true);
        bt_recording.setText(TAG_START_RECORDING);
        Toast.makeText(getApplicationContext(), "Audio recorded successfully",
                Toast.LENGTH_LONG).show();
    }

    public void startPlayback() throws IOException {
        MediaPlayer m = new MediaPlayer();
        m.setDataSource(outputFile);
        m.prepare();
        m.start();
        Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Button b = (Button) findViewById(v.getId());

        if(b.getId() == bt_recording.getId()) {

            if (b.getText().equals(TAG_START_RECORDING)) {
                //Start Recording
                startRecording();
                //stop();
            } else {
                //Stop Recording
                stopRecording();
                //stop();
            }

        }else{

            if(b.getId() == bt_playback.getId()) {

                if (b.getText().equals(TAG_START_PLAYBACK)) {
                    //start Playback
                    try {
                        startPlayback();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        drawBaseLine = surfaceView.getHeight() - 100;

        Canvas canvas = holder.lockCanvas();
        canvas = null;
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null");
        } else {

            InputStream inStream = null;
            try {

                canvas.drawRGB(255, 128, 128);
                canvas.drawLine(0,drawBaseLine, surfaceView.getWidth(),drawBaseLine, paint);
                inStream = new FileInputStream(outputFile);
                byte[] music = new byte[inStream.available()];
                music = convertStreamToByteArray(inStream);

                System.out.println("No of Bytes = "+music.length);

                buffer = new short[music.length/2];
                ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);
                bufferReadResult = buffer.length;

                if((bufferReadResult & -bufferReadResult - 1) != bufferReadResult) bufferReadResult = 2 << (int)(Math.log(bufferReadResult)/Math.log(2));

                fft = new FFT(bufferReadResult, RECORDER_SAMPLERATE);
                fftRealArray = new float[bufferReadResult];

                volume = 0;

                try {
                    for (int i = 0; i < bufferReadResult; i++) {
                        fftRealArray[i] = (float) buffer[i] / Short.MAX_VALUE;// 32768.0;
                        volume += Math.abs(fftRealArray[i]);
                    }
                }catch(ArrayIndexOutOfBoundsException e){

                }finally {


                    volume = (float) Math.log10(volume / bufferReadResult);
                    int i = 0;
                    // apply windowing
                    for (i = 0 ; i < bufferReadResult / 2; ++i) {
                        // Calculate & apply window symmetrically around center point
                        // Hanning (raised cosine) window
                        float winval = (float) (0.5 + 0.5 * Math.cos(Math.PI * (float) i / (float) (bufferReadResult / 2)));
                        if (i > bufferReadResult / 2) winval = 0;
                        fftRealArray[bufferReadResult / 2 + i] *= winval;
                        fftRealArray[bufferReadResult / 2 - i] *= winval;
                    }
                    // zero out first point (not touched by odd-length window)
                    fftRealArray[0] = 0;
                    fft.forward(fftRealArray);

                    System.out.println("Length of fftRealArray = " + fftRealArray.length);
                    System.out.println("Spectrum Size = " + fft.specSize());
                    System.out.println("Surface View WIdth = "+surfaceView.getWidth());
                    surfaceView.invalidate();

                    paint.setStrokeWidth(1.1f);

                    NumberFormat format = new DecimalFormat("#.##");

                    double increment = (double)surfaceView.getWidth()/(double)fft.specSize();
                    System.out.println("Increment = "+increment);
                    float x = 0;
                    for(i = 0; i<fft.specSize(); i++){
                        float val = 0;
                        val += fft.getFreq(fft.indexToFreq(i));
                        x += increment;
                        if(val>27)
                        {
                            System.out.println("Amplitude of Frequency Band "+i+" is "+val);
                            System.out.println("X = "+x);
                            canvas.drawLine(x, drawBaseLine, x, drawBaseLine-(val*4), paint);
                        }

                    }

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void plotValues(){

        InputStream inStream = null;
        try {
            inStream = new FileInputStream(outputFile);
            byte[] music = new byte[inStream.available()];
            music = convertStreamToByteArray(inStream);

            System.out.println("No of Bytes = "+music.length);

            buffer = new short[music.length/2];
            ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);
            bufferReadResult = buffer.length;

            if((bufferReadResult & -bufferReadResult - 1) != bufferReadResult) bufferReadResult = 2 << (int)(Math.log(bufferReadResult)/Math.log(2));

            fft = new FFT(bufferReadResult, RECORDER_SAMPLERATE);
            fftRealArray = new float[bufferReadResult];

            volume = 0;

            try {
                for (int i = 0; i < bufferReadResult; i++) {
                    fftRealArray[i] = (float) buffer[i] / Short.MAX_VALUE;// 32768.0;
                    volume += Math.abs(fftRealArray[i]);
                }
            }catch(ArrayIndexOutOfBoundsException e){

            }finally {


                volume = (float) Math.log10(volume / bufferReadResult);
                int i = 0;
                // apply windowing
                for (i = 0 ; i < bufferReadResult / 2; ++i) {
                    // Calculate & apply window symmetrically around center point
                    // Hanning (raised cosine) window
                    float winval = (float) (0.5 + 0.5 * Math.cos(Math.PI * (float) i / (float) (bufferReadResult / 2)));
                    if (i > bufferReadResult / 2) winval = 0;
                    fftRealArray[bufferReadResult / 2 + i] *= winval;
                    fftRealArray[bufferReadResult / 2 - i] *= winval;
                }
                // zero out first point (not touched by odd-length window)
                fftRealArray[0] = 0;
                fft.forward(fftRealArray);

                System.out.println("Length of fftRealArray = " + fftRealArray.length);

                double increment = (double)surfaceView.getWidth()/(double)fft.specSize();
                System.out.println("Increment = "+increment);
                float x = 0;

                float[] spectralLinesHeights = new float[fft.specSize()];

                for(i = 0; i<fft.specSize(); i++){
                    spectralLinesHeights[i] = fft.getFreq(fft.indexToFreq(i));

                 }

                SpectralGraphActivity.spectralLinesArray = spectralLinesHeights;

                startActivity(new Intent(Home.this,SpectralGraphActivity.class));

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[10240];
        int i = Integer.MAX_VALUE;
        while ((i = is.read(buff, 0, buff.length)) > 0) {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function
    }

    public void stop(){
        audioRecord.stop();
        audioRecord.release();
    }



}
