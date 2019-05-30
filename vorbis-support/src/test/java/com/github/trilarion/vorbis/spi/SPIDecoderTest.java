/*
 * Copyright (C) 2005 Javazoom
 *               2013 Trilarion
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.trilarion.vorbis.spi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Simple player (based on SPI) unit test.
 */
public class SPIDecoderTest {

    private static final Logger LOG = Logger.getLogger(SPIDecoderTest.class.getName());

    private String filename = null;
    private String fileurl = null;

    /**
     *
     * @throws IOException
     */
    public SPIDecoderTest() throws IOException {

        Properties props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("spi.test.properties"));
        filename = props.getProperty("filename");
        fileurl = props.getProperty("stream");
    }

    /**
     *
     */
    @Test
    public void testPlayFile() {
        try {
            System.out.println("Start : " + filename);
            File file = new File("Agogo.ogg");

            AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
            System.out.println(" Audio Type : " + aff.getType());

            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                System.out.println(" Source Format : " + baseFormat.toString());

                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
                        16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                System.out.println(" Target Format : " + decodedFormat.toString());

                AudioInputStream dataIn = AudioSystem.getAudioInputStream(decodedFormat, in);
                // play
                rawplay(decodedFormat, dataIn);

                in.close();
                System.out.println("Stop : " + filename);

                // testPlay has been okay
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            Assert.fail("testPlay : " + e.getMessage());
        }
    }

    /**
     *
     */
    @Test
    public void testPlayURL() {
        try {
            System.out.println("Start : " + fileurl);
            URL url = new URL(fileurl);

            AudioFileFormat aff = AudioSystem.getAudioFileFormat(url);
            System.out.println(" Audio Type : " + aff.getType());

            AudioInputStream in = AudioSystem.getAudioInputStream(url);
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                System.out.println(" Source Format : " + baseFormat.toString());

                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                System.out.println(" Target Format : " + decodedFormat.toString());

                AudioInputStream dataIn = AudioSystem.getAudioInputStream(decodedFormat, in);
                rawplay(decodedFormat, dataIn);

                System.out.println("Stop : " + filename);

                // testPlay has been OK
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            Assert.fail("testPlay : " + e.getMessage());
        }
    }

    private void rawplay(AudioFormat targetFormat, AudioInputStream dataIn) throws IOException, LineUnavailableException {
        // get buffer
        byte[] data = new byte[4096];

        // get line
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

        if (line != null) {
            // open it
            line.open();

            // start
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = dataIn.read(data, 0, data.length);
                if (nBytesRead != -1) {
                    nBytesWritten = line.write(data, 0, nBytesRead);
                }
            }

            // stop line
            line.drain();
            line.stop();
            line.close();

            // stop input stream
            dataIn.close();
        }
    }
}
