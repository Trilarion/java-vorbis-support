/*
 * Copyright (C) 1999 by Matthias Pfisterer
 *               2001 by Florian Bomers <http://www.bomers.de>
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
package com.github.trilarion.sound.sampled.spi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

/**
 * Base class for audio file readers. This is Tritonus' base class for classes
 * that provide the facility of detecting an audio file type and reading its
 * header. Classes should be derived from this class or one of its subclasses
 * rather than from javax.sound.sampled.spi.AudioFileReader.
 *
 * @author Matthias Pfisterer
 * @author Florian Bomers
 */
public abstract class TAudioFileReader extends AudioFileReader {

    private static final Logger LOG = Logger.getLogger(TAudioFileReader.class.getName());

    private int m_nMarkLimit = -1;
    private final boolean m_bRereading;

    /**
     *
     * @param nMarkLimit
     * @param bRereading
     */
    protected TAudioFileReader(int nMarkLimit, boolean bRereading) {
        m_nMarkLimit = nMarkLimit;
        m_bRereading = bRereading;
    }

    private int getMarkLimit() {
        return m_nMarkLimit;
    }

    private boolean isRereading() {
        return m_bRereading;
    }

    /**
     * Get an AudioFileFormat object for a File. This method calls
     * getAudioFileFormat(InputStream, long). Subclasses should not override
     * this method unless there are really severe reasons. Normally, it is
     * sufficient to implement getAudioFileFormat(InputStream, long).
     *
     * @param file	the file to read from.
     * @return	an AudioFileFormat instance containing information from the
     * header of the file passed in.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(File file)
            throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioFileFormat(File): begin (class: {0})", getClass().getSimpleName());
        long lFileLengthInBytes = file.length();
        AudioFileFormat audioFileFormat;
        try (InputStream inputStream = new FileInputStream(file)) {
            audioFileFormat = getAudioFileFormat(inputStream, lFileLengthInBytes);
        }
        LOG.log(Level.FINE, "TAudioFileReader.getAudioFileFormat(File): end");
        return audioFileFormat;
    }

    /**
     * Get an AudioFileFormat object for a URL. This method calls
     * getAudioFileFormat(InputStream, long). Subclasses should not override
     * this method unless there are really severe reasons. Normally, it is
     * sufficient to implement getAudioFileFormat(InputStream, long).
     *
     * @param url	the URL to read from.
     * @return	an AudioFileFormat instance containing information from the
     * header of the URL passed in.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(URL url)
            throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioFileFormat(URL): begin (class: {0})", getClass().getSimpleName());
        long lFileLengthInBytes = getDataLength(url);
        AudioFileFormat audioFileFormat;
        try (InputStream inputStream = url.openStream()) {
            audioFileFormat = getAudioFileFormat(inputStream, lFileLengthInBytes);
        }
        LOG.log(Level.FINE, "TAudioFileReader.getAudioFileFormat(URL): end");
        return audioFileFormat;
    }

    /**
     * Get an AudioFileFormat object for an InputStream. This method calls
     * getAudioFileFormat(InputStream, long). Subclasses should not override
     * this method unless there are really severe reasons. Normally, it is
     * sufficient to implement getAudioFileFormat(InputStream, long).
     *
     * @param inputStream	the stream to read from.
     * @return	an AudioFileFormat instance containing information from the
     * header of the stream passed in.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioFileFormat getAudioFileFormat(InputStream inputStream)
            throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioFileFormat(InputStream): begin (class: {0})", getClass().getSimpleName());
        long lFileLengthInBytes = AudioSystem.NOT_SPECIFIED;
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream, getMarkLimit());
        }
        inputStream.mark(getMarkLimit());
        AudioFileFormat audioFileFormat;
        try {
            audioFileFormat = getAudioFileFormat(inputStream, lFileLengthInBytes);
        } finally {
            /* TODO: required semantics is unclear: should reset()
             be executed only when there is an exception or
             should it be done always?
             */
            inputStream.reset();
        }
        LOG.log(Level.FINE, "TAudioFileReader.getAudioFileFormat(InputStream): end");
        return audioFileFormat;
    }

    /**
     * Get an AudioFileFormat (internal implementation). Subclasses must
     * implement this method in a way specific to the file format they handle.
     * Note that depending on the implementation of this method, you should or
     * should not override getAudioInputStream(InputStream, long), too (see
     * comment there).
     *
     * @param inputStream The InputStream to read from. It should be tested if
     * it is markable. If not, and it is re-reading, wrap it into a
     * BufferedInputStream with getMarkLimit() size.
     * @param lFileLengthInBytes The size of the originating file, if known. If
     * it isn't known, AudioSystem.NOT_SPECIFIED should be passed. This value
     * may be used for byteLength in AudioFileFormat, if this value can't be
     * derived from the informmation in the file header.
     * @return an AudioFileFormat instance containing information from the
     * header of the stream passed in as inputStream.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    protected abstract AudioFileFormat getAudioFileFormat(
            InputStream inputStream, long lFileLengthInBytes)
            throws UnsupportedAudioFileException, IOException;

    /**
     * Get an AudioInputStream object for a file. This method calls
     * getAudioInputStream(InputStream, long). Subclasses should not override
     * this method unless there are really severe reasons. Normally, it is
     * sufficient to implement getAudioFileFormat(InputStream, long) and perhaps
     * override getAudioInputStream(InputStream, long).
     *
     * @param file	the File object to read from.
     * @return	an AudioInputStream instance containing the audio data from this
     * file.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioInputStream getAudioInputStream(File file)
            throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(File): begin (class: {0})", getClass().getSimpleName());
        long lFileLengthInBytes = file.length();
        InputStream inputStream = new FileInputStream(file);
        AudioInputStream audioInputStream;
        try {
            audioInputStream = getAudioInputStream(inputStream, lFileLengthInBytes);
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(File): end");
        return audioInputStream;
    }

    /**
     * Get an AudioInputStream object for a URL. This method calls
     * getAudioInputStream(InputStream, long). Subclasses should not override
     * this method unless there are really severe reasons. Normally, it is
     * sufficient to implement getAudioFileFormat(InputStream, long) and perhaps
     * override getAudioInputStream(InputStream, long).
     *
     * @param url	the URL to read from.
     * @return	an AudioInputStream instance containing the audio data from this
     * URL.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioInputStream getAudioInputStream(URL url)
            throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(URL): begin (class: {0})", getClass().getSimpleName());
        long lFileLengthInBytes = getDataLength(url);
        InputStream inputStream = url.openStream();
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = getAudioInputStream(inputStream, lFileLengthInBytes);
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(URL): end");
        return audioInputStream;
    }

    /**
     * Get an AudioInputStream object for an InputStream. This method calls
     * getAudioInputStream(InputStream, long). Subclasses should not override
     * this method unless there are really severe reasons. Normally, it is
     * sufficient to implement getAudioFileFormat(InputStream, long) and perhaps
     * override getAudioInputStream(InputStream, long).
     *
     * @param inputStream	the stream to read from.
     * @return	an AudioInputStream instance containing the audio data from this
     * stream.
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    @Override
    public AudioInputStream getAudioInputStream(InputStream inputStream)
            throws UnsupportedAudioFileException, IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(InputStream): begin (class: {0})", getClass().getSimpleName());
        long lFileLengthInBytes = AudioSystem.NOT_SPECIFIED;
        AudioInputStream audioInputStream = null;
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream, getMarkLimit());
        }
        inputStream.mark(getMarkLimit());
        try {
            audioInputStream = getAudioInputStream(inputStream, lFileLengthInBytes);
        } catch (UnsupportedAudioFileException e) {
            inputStream.reset();
            throw e;
        } catch (IOException e) {
            try {
                inputStream.reset();
            } catch (IOException e2) {
                if (e2.getCause() == null) {
                    e2.initCause(e);
                    throw e2;
                }
            }
            throw e;
        }
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(InputStream): end");
        return audioInputStream;
    }

    /**
     * Get an AudioInputStream (internal implementation). This implementation
     * calls getAudioFileFormat() with the same arguments as passed in here.
     * Then, it constructs an AudioInputStream instance. This instance takes the
     * passed inputStream in the state it is left after getAudioFileFormat() did
     * its work. In other words, the implementation here assumes that
     * getAudioFileFormat() reads the entire header up to a position exactly
     * where the audio data starts. If this can't be realized for a certain
     * format, this method should be overridden.
     *
     * @param inputStream The InputStream to read from. It should be tested if
     * it is markable. If not, and it is re-reading, wrap it into a
     * BufferedInputStream with getMarkLimit() size.
     * @param lFileLengthInBytes The size of the originating file, if known. If
     * it isn't known, AudioSystem.NOT_SPECIFIED should be passed. This value
     * may be used for byteLength in AudioFileFormat, if this value can't be
     * derived from the information in the file header.
     * @return
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    protected AudioInputStream getAudioInputStream(InputStream inputStream,
            long lFileLengthInBytes) throws UnsupportedAudioFileException,
            IOException {
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(InputStream, long): begin (class: {0})", getClass().getSimpleName());
        if (isRereading()) {
            if (!inputStream.markSupported()) {
                inputStream = new BufferedInputStream(inputStream,
                        getMarkLimit());
            }
            inputStream.mark(getMarkLimit());
        }
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream,
                lFileLengthInBytes);
        if (isRereading()) {
            inputStream.reset();
        }
        AudioInputStream audioInputStream = new AudioInputStream(inputStream,
                audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
        LOG.log(Level.FINE, "TAudioFileReader.getAudioInputStream(InputStream, long): end");
        return audioInputStream;
    }

    /**
     *
     * @param url
     * @return
     * @throws IOException
     */
    private static long getDataLength(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.connect();
        int length = connection.getContentLength();
        return length > 0 ? length : AudioSystem.NOT_SPECIFIED;
    }
}
