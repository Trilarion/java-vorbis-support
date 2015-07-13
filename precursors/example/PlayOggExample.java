
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Put jogg-0.0.7.jar, jorbis-0.0.15.jar, vorbisspi1.0.3.jar, tritonus_share.jar
 * and Agogo.ogg in the same folder and run this example.
 */
public class PlayOggExample {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(new File("Agogo.ogg"));
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();

                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
                        16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);

                AudioInputStream dataIn = AudioSystem.getAudioInputStream(targetFormat, in);

                byte[] buffer = new byte[4096];

                // get a line from a mixer in the system with the wanted format
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                if (line != null) {
                    line.open();

                    line.start();
                    int nBytesRead = 0, nBytesWritten = 0;
                    while (nBytesRead != -1) {
                        nBytesRead = dataIn.read(buffer, 0, buffer.length);
                        if (nBytesRead != -1) {
                            nBytesWritten = line.write(buffer, 0, nBytesRead);
                        }
                    }

                    line.drain();
                    line.stop();
                    line.close();

                    dataIn.close();
                }

                in.close();
                // playback finished
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // failed
        }
    }

}
