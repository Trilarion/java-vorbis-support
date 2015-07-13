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
package com.github.trilarion.vorbis.spi.vorbis.sampled.file;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Test;

/**
 * This test case doesn't include assert, so it's not a jUnit test as you used
 * to have. However, it helps in testing all methods of SPI.
 */
public class VorbisAudioFileReaderTest {

    private final String[] names = {"Agogo.ogg"};

    /**
     *
     */
    @Test
    public void testGetAudioFileFormat() {
        for (String name : names) {
            _testGetAudioFileFormatFile(name);
            _testGetAudioFileFormatURL(name);
            _testGetAudioFileFormatInputStream(name);
        }
    }

    /**
     *
     */
    @Test
    public void testGetAudioInputStream() {
        for (String name : names) {
            _testGetAudioInputStreamFile(name);
            _testGetAudioInputStreamURL(name);
            _testGetAudioInputStreamInputStream(name);
        }
        //_testGetInfo();
    }

    /*
     * Test for AudioFileFormat getAudioFileFormat(File)
     */
    private void _testGetAudioFileFormatFile(String filename) {
        System.out.println("*** testGetAudioFileFormatFile ***");
        try {
            File file = new File(filename);
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
            dumpAudioFileFormat(baseFileFormat, file.toString());
            // TODO : Add assert();
        } catch (UnsupportedAudioFileException | IOException e) {
            // TODO e.printStackTrace();
        }
    }

    /*
     * Test for AudioFileFormat getAudioFileFormat(URL)
     */
    private void _testGetAudioFileFormatURL(String fileurl) {
        System.out.println("*** testGetAudioFileFormatURL ***");
        try {
            URL url = new URL(fileurl);
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(url);
            dumpAudioFileFormat(baseFileFormat, url.toString());
            // TODO : Add assert();
        } catch (UnsupportedAudioFileException | IOException e) {
            // TODO e.printStackTrace();
        }
    }

    /*
     * Test for AudioFileFormat getAudioFileFormat(InputStream)
     */
    private void _testGetAudioFileFormatInputStream(String filename) {
        System.out.println("*** testGetAudioFileFormatInputStream ***");
        try (InputStream in = new BufferedInputStream(new FileInputStream(filename))) {
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(in);
            dumpAudioFileFormat(baseFileFormat, in.toString());
        } catch (UnsupportedAudioFileException | IOException e) {
            // TODO e.printStackTrace();
        }
    }

    /*
     * Test for AudioInputStream getAudioInputStream(InputStream)
     */
    private void _testGetAudioInputStreamInputStream(String filename) {
        System.out.println("*** testGetAudioInputStreamInputStream ***");
        try {
            AudioInputStream in;
            try (InputStream fin = new BufferedInputStream(new FileInputStream(filename))) {
                in = AudioSystem.getAudioInputStream(fin);
                dumpAudioInputStream(in, fin.toString());
            }
            in.close();
            // TODO : Add assert();
        } catch (UnsupportedAudioFileException | IOException e) {
            // TODO e.printStackTrace();
        }
    }

    /*
     * Test for AudioInputStream getAudioInputStream(File)
     */
    private void _testGetAudioInputStreamFile(String filename) {
        System.out.println("*** testGetAudioInputStreamFile ***");
        try {
            File file = new File(filename);
            try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
                dumpAudioInputStream(in, file.toString());
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            // TODO e.printStackTrace();
        }
    }

    /*
     * Test for AudioInputStream getAudioInputStream(URL)
     */
    private void _testGetAudioInputStreamURL(String fileurl) {
        System.out.println("*** testGetAudioInputStreamURL ***");
        try {
            URL url = new URL(fileurl);
            try (AudioInputStream in = AudioSystem.getAudioInputStream(url)) {
                dumpAudioInputStream(in, url.toString());
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            // TODO e.printStackTrace();
        }
    }

    /**
     *
     * @param filename
     */
    public void _testGetInfo(String filename) {
        System.out.println("*** testGetInfo ***");
        try {
            byte[] audioData = getByteArrayFromFile(new File(filename));
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
        } catch (IOException | UnsupportedAudioFileException e) {
            // TODO e.printStackTrace();
        }
    }

    private static byte[] getByteArrayFromFile(final File file) throws FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            final byte[] buffer = new byte[1024];
            int cnt;
            while ((cnt = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, cnt);
            }
            return bos.toByteArray();
        }
    }

    private void dumpAudioFileFormat(AudioFileFormat baseFileFormat, String info) throws UnsupportedAudioFileException {
        // AudioFileFormat
        System.out.println("  -----  " + info + "  -----");
        System.out.println("    ByteLength=" + baseFileFormat.getByteLength());
        System.out.println("    getFrameLength=" + baseFileFormat.getFrameLength());
        System.out.println("    getType=" + baseFileFormat.getType());
        AudioFormat baseFormat = baseFileFormat.getFormat();
        // AudioFormat
        System.out.println("    Source Format=" + baseFormat.toString());
        System.out.println("    Channels=" + baseFormat.getChannels());
        System.out.println("    FrameRate=" + baseFormat.getFrameRate());
        System.out.println("    FrameSize=" + baseFormat.getFrameSize());
        System.out.println("    SampleRate=" + baseFormat.getSampleRate());
        System.out.println("    SampleSizeInBits=" + baseFormat.getSampleSizeInBits());
        System.out.println("    Encoding=" + baseFormat.getEncoding());
    }

    private void dumpAudioInputStream(AudioInputStream in, String info) throws IOException {
        System.out.println("  -----  " + info + "  -----");
        System.out.println("    Available=" + in.available());
        System.out.println("    FrameLength=" + in.getFrameLength());
        AudioFormat baseFormat = in.getFormat();
        // AudioFormat
        System.out.println("    Source Format=" + baseFormat.toString());
        System.out.println("    Channels=" + baseFormat.getChannels());
        System.out.println("    FrameRate=" + baseFormat.getFrameRate());
        System.out.println("    FrameSize=" + baseFormat.getFrameSize());
        System.out.println("    SampleRate=" + baseFormat.getSampleRate());
        System.out.println("    SampleSizeInBits=" + baseFormat.getSampleSizeInBits());
        System.out.println("    Encoding=" + baseFormat.getEncoding());
    }
    private static final Logger LOG = Logger.getLogger(VorbisAudioFileReaderTest.class.getName());
}
