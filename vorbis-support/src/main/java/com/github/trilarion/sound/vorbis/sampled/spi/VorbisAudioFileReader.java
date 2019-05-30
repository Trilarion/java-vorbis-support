/*
 * Copyright (C) 2004 - 2008 JavaZOOM : vorbisspi@javazoom.net
 *               2015 Trilarion
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
package com.github.trilarion.sound.vorbis.sampled.spi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.github.trilarion.sound.sampled.spi.TAudioFileReader;
import com.github.trilarion.sound.util.SoundException;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Packet;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Page;
import com.github.trilarion.sound.vorbis.jcraft.jogg.StreamState;
import com.github.trilarion.sound.vorbis.jcraft.jogg.SyncState;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.Comment;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.Info;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.VorbisFile;
import com.github.trilarion.sound.vorbis.sampled.VorbisAudioFileFormat;

/**
 * This class implements the AudioFileReader class and provides an Ogg Vorbis
 * file reader for use with the Java Sound Service Provider Interface.
 */
public class VorbisAudioFileReader extends TAudioFileReader {

    private static final Logger LOG = Logger.getLogger(VorbisAudioFileReader.class.getName());
    /**
     *
     */
    public static final AudioFormat.Encoding VORBISENC = new AudioFormat.Encoding("VORBISENC");
    
    /**
     * 
     */
    public static final AudioFileFormat.Type OGG_AUDIOFILEFORMAT_TYPE = new AudioFileFormat.Type("OGG", "ogg");    

    private SyncState oggSyncState_ = null;
    private StreamState oggStreamState_ = null;
    private Page oggPage_ = null;
    private Packet oggPacket_ = null;
    private Info vorbisInfo = null;
    private Comment vorbisComment = null;
    private final int bufferMultiple_ = 4;
    private byte[] buffer = null;
    private int bytes = 0;

    private int index = 0;
    private InputStream oggBitStream_ = null;

    private static final int INITAL_READ_LENGTH = 64000;
    private static final int MARK_LIMIT = INITAL_READ_LENGTH + 1;

    /**
     *
     */
    public VorbisAudioFileReader() {
        super(MARK_LIMIT, true);
    }

    /**
     * Return the AudioFileFormat from the given file.
     *
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioFileFormat(File file)");
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            inputStream.mark(MARK_LIMIT);
            getAudioFileFormat(inputStream);
            inputStream.reset();
            // Get Vorbis file info such as length in seconds.
            VorbisFile vf = new VorbisFile(file.getAbsolutePath());
            return getAudioFileFormat(inputStream, (int) file.length(), Math.round(vf.time_total(-1) * 1000));
        } catch (SoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Return the AudioFileFormat from the given URL.
     *
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioFileFormat(URL url)");
        try (InputStream inputStream = url.openStream()) {
            return getAudioFileFormat(inputStream);
        }
    }

    /**
     * Return the AudioFileFormat from the given InputStream.
     *
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioFileFormat(InputStream inputStream)");
        try {
            if (!inputStream.markSupported()) {
                inputStream = new BufferedInputStream(inputStream);
            }
            inputStream.mark(MARK_LIMIT);
            return getAudioFileFormat(inputStream, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
        } finally {
            inputStream.reset();
        }
    }

    /**
     * Return the AudioFileFormat from the given InputStream and length in
     * bytes.
     *
     * @param medialength
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(InputStream inputStream, long medialength) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(inputStream, (int) medialength, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Return the AudioFileFormat from the given InputStream, length in bytes
     * and length in milliseconds.
     *
     * @param bitStream
     * @param totalms
     * @param mediaLength
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    protected AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength, int totalms) throws UnsupportedAudioFileException, IOException {
        Map<String, Object> aff_properties = new HashMap<>();
        Map<String, Object> af_properties = new HashMap<>();
        if (totalms == AudioSystem.NOT_SPECIFIED) {
            totalms = 0;
        }
        if (totalms > 0) {
            aff_properties.put("duration", (long) totalms * 1000);
        }
        oggBitStream_ = bitStream;
        // init jorbis
        oggSyncState_ = new SyncState();
        oggStreamState_ = new StreamState();
        oggPage_ = new Page();
        oggPacket_ = new Packet();
        vorbisInfo = new Info();
        vorbisComment = new Comment();
        buffer = null;
        bytes = 0;
        oggSyncState_.init();

        index = 0;
        try {
            readHeaders(aff_properties, af_properties);
        } catch (IOException ioe) {
            LOG.log(Level.FINE, ioe.getMessage());
            throw new UnsupportedAudioFileException(ioe.getMessage());
        }

        String dmp = vorbisInfo.toString();
        LOG.log(Level.FINE, dmp);
        int ind = dmp.lastIndexOf("bitrate:");
        int minbitrate = -1;
        int nominalbitrate = -1;
        int maxbitrate = -1;
        if (ind != -1) {
            dmp = dmp.substring(ind + 8);
            StringTokenizer st = new StringTokenizer(dmp, ",");
            if (st.hasMoreTokens()) {
                minbitrate = Integer.parseInt(st.nextToken());
            }
            if (st.hasMoreTokens()) {
                nominalbitrate = Integer.parseInt(st.nextToken());
            }
            if (st.hasMoreTokens()) {
                maxbitrate = Integer.parseInt(st.nextToken());
            }
        }
        if (nominalbitrate > 0) {
            af_properties.put("bitrate", nominalbitrate);
        }
        af_properties.put("vbr", true);

        if (minbitrate > 0) {
            aff_properties.put("ogg.bitrate.min.bps", minbitrate);
        }
        if (maxbitrate > 0) {
            aff_properties.put("ogg.bitrate.max.bps", maxbitrate);
        }
        if (nominalbitrate > 0) {
            aff_properties.put("ogg.bitrate.nominal.bps", nominalbitrate);
        }
        if (vorbisInfo.channels > 0) {
            aff_properties.put("ogg.channels", vorbisInfo.channels);
        }
        if (vorbisInfo.rate > 0) {
            aff_properties.put("ogg.frequency.hz", vorbisInfo.rate);
        }
        if (mediaLength > 0) {
            aff_properties.put("ogg.length.bytes", mediaLength);
        }
        aff_properties.put("ogg.version", vorbisInfo.version);

        //AudioFormat.Encoding encoding = VorbisEncoding.VORBISENC;
        //AudioFormat format = new VorbisAudioFormat(encoding, vorbisInfo.rate, AudioSystem.NOT_SPECIFIED, vorbisInfo.channels, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true,af_properties);
        // Patch from MS to ensure more SPI compatibility ...
        float frameRate = -1;
        if (nominalbitrate > 0) {
            frameRate = nominalbitrate / 8;
        } else if (minbitrate > 0) {
            frameRate = minbitrate / 8;
        }

        // New Patch from MS:
        AudioFormat format = new AudioFormat(VORBISENC, vorbisInfo.rate, AudioSystem.NOT_SPECIFIED, vorbisInfo.channels, 1, frameRate, false, af_properties);
        // Patch end

        return new VorbisAudioFileFormat(OGG_AUDIOFILEFORMAT_TYPE, format, AudioSystem.NOT_SPECIFIED, mediaLength, aff_properties);
    }

    /**
     * Return the AudioInputStream from the given InputStream.
     *
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioInputStream getAudioInputStream(InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioInputStream(InputStream inputStream)");
        return getAudioInputStream(inputStream, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Return the AudioInputStream from the given InputStream.
     *
     * @param inputStream
     * @param totalms
     * @param medialength
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    public AudioInputStream getAudioInputStream(InputStream inputStream, int medialength, int totalms) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioInputStream(InputStream inputStreamint medialength, int totalms)");
        try {
            if (!inputStream.markSupported()) {
                inputStream = new BufferedInputStream(inputStream);
            }
            inputStream.mark(MARK_LIMIT);
            AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, medialength, totalms);
            inputStream.reset();
            return new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.reset();
            throw e;
        }
    }

    /**
     * Return the AudioInputStream from the given File.
     *
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioInputStream(File file)");
        InputStream inputStream = new FileInputStream(file);
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
    }

    /**
     * Return the AudioInputStream from the given URL.
     *
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "getAudioInputStream(URL url)");
        InputStream inputStream = url.openStream();
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            if (inputStream != null) {
                inputStream.close();
            }
            throw e;
        }
    }

    /**
     * Reads headers and comments.
     */
    private void readHeaders(Map<String, Object> aff_properties, Map<String, Object> af_properties) throws IOException {
        LOG.log(Level.FINE, "readHeaders(");
        int bufferSize_ = bufferMultiple_ * 256 * 2;
        index = oggSyncState_.buffer(bufferSize_);
        buffer = oggSyncState_.data;
        bytes = readFromStream(buffer, index, bufferSize_);
        if (bytes == -1) {
            LOG.log(Level.FINE, "Cannot get any data from selected Ogg bitstream.");
            throw new IOException("Cannot get any data from selected Ogg bitstream.");
        }
        oggSyncState_.wrote(bytes);
        if (oggSyncState_.pageout(oggPage_) != 1) {
            if (bytes < bufferSize_) {
                throw new IOException("EOF");
            }
            LOG.log(Level.FINE, "Input does not appear to be an Ogg bitstream.");
            throw new IOException("Input does not appear to be an Ogg bitstream.");
        }
        oggStreamState_.init(oggPage_.serialno());
        vorbisInfo.init();
        vorbisComment.init();
        aff_properties.put("ogg.serial", oggPage_.serialno());
        if (oggStreamState_.pagein(oggPage_) < 0) {
            // error; stream version mismatch perhaps
            LOG.log(Level.FINE, "Error reading first page of Ogg bitstream data.");
            throw new IOException("Error reading first page of Ogg bitstream data.");
        }
        if (oggStreamState_.packetout(oggPacket_) != 1) {
            // no page? must not be vorbis
            LOG.log(Level.FINE, "Error reading initial header packet.");
            throw new IOException("Error reading initial header packet.");
        }
        if (vorbisInfo.synthesis_headerin(vorbisComment, oggPacket_) < 0) {
            // error case; not a vorbis header
            LOG.log(Level.FINE, "This Ogg bitstream does not contain Vorbis audio data.");
            throw new IOException("This Ogg bitstream does not contain Vorbis audio data.");
        }
        int i = 0;
        while (i < 2) {
            while (i < 2) {
                int result = oggSyncState_.pageout(oggPage_);
                if (result == 0) {
                    break;
                } // Need more data
                if (result == 1) {
                    oggStreamState_.pagein(oggPage_);
                    while (i < 2) {
                        result = oggStreamState_.packetout(oggPacket_);
                        if (result == 0) {
                            break;
                        }
                        if (result == -1) {
                            LOG.log(Level.FINE, "Corrupt secondary header.  Exiting.");
                            throw new IOException("Corrupt secondary header.  Exiting.");
                        }
                        vorbisInfo.synthesis_headerin(vorbisComment, oggPacket_);
                        i++;
                    }
                }
            }
            index = oggSyncState_.buffer(bufferSize_);
            buffer = oggSyncState_.data;
            bytes = readFromStream(buffer, index, bufferSize_);
            if (bytes == -1) {
                break;
            }
            if (bytes == 0 && i < 2) {
                LOG.log(Level.FINE, "End of file before finding all Vorbis headers!");
                throw new IOException("End of file before finding all Vorbis  headers!");
            }
            oggSyncState_.wrote(bytes);
        }
        // Read Ogg Vorbis comments.
        byte[][] ptr = vorbisComment.user_comments;
        int c = 0;
        for (byte[] ptr1 : ptr) {
            if (ptr1 == null) {
                break;
            }
            String currComment = new String(ptr1, 0, ptr1.length - 1, StandardCharsets.UTF_8).trim();
            LOG.log(Level.FINE, currComment);
            if (currComment.toLowerCase(Locale.ENGLISH).startsWith("artist")) {
                aff_properties.put("author", currComment.substring(7));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("title")) {
                aff_properties.put("title", currComment.substring(6));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("album")) {
                aff_properties.put("album", currComment.substring(6));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("date")) {
                aff_properties.put("date", currComment.substring(5));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("copyright")) {
                aff_properties.put("copyright", currComment.substring(10));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("comment")) {
                aff_properties.put("comment", currComment.substring(8));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("genre")) {
                aff_properties.put("ogg.comment.genre", currComment.substring(6));
            } else if (currComment.toLowerCase(Locale.ENGLISH).startsWith("tracknumber")) {
                aff_properties.put("ogg.comment.track", currComment.substring(12));
            } else {
                c++;
                aff_properties.put("ogg.comment.ext." + c, currComment);
            }
            aff_properties.put("ogg.comment.encodedby", new String(vorbisComment.vendor, 0, vorbisComment.vendor.length - 1, Charset.forName("US-ASCII")));
        }
    }

    /**
     * Reads from the oggBitStream_ a specified number of Bytes(bufferSize_)
     * worth starting at index and puts them in the specified buffer[].
     *
     * @return the number of bytes read or -1 if error.
     */
    private int readFromStream(byte[] buffer, int index, int bufferSize_) {
        int bytes;
        try {
            bytes = oggBitStream_.read(buffer, index, bufferSize_);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Cannot Read Selected Song");
            bytes = -1;
        }
        return bytes;
    }

}
