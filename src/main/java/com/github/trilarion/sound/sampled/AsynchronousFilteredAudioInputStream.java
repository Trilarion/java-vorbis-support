/*
 * Copyright (C) 1999, 2000 by Matthias Pfisterer
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
package com.github.trilarion.sound.sampled;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * Base class for asynchronus converters. This class serves as base class for
 * converters that do not have a fixed ratio between the size of a block of
 * input data and the size of a block of output data. These types of converters
 * therefore need an internal buffer, which is realized in this class.
 *
 * @author Matthias Pfisterer
 */
public abstract class AsynchronousFilteredAudioInputStream
        extends AudioInputStream
        implements CircularBuffer.BufferListener {

    private static final Logger LOG = Logger.getLogger(AsynchronousFilteredAudioInputStream.class.getName());

    private static final int DEFAULT_BUFFER_SIZE = 327670;
    private static final int DEFAULT_MIN_AVAILABLE = 4096;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    // must be protected because it's accessed by the native CDDA lib 
    /**
     *
     */
    private final CircularBuffer m_circularBuffer;
    private byte[] m_abSingleByte;

    /**
     * Constructor. This constructor uses the default buffer size and the
     * default min available amount.
     *
     * @param outputFormat
     * @param lLength length of this stream in frames. May be
     * AudioSystem.NOT_SPECIFIED.
     */
    public AsynchronousFilteredAudioInputStream(AudioFormat outputFormat, long lLength) {
        this(outputFormat, lLength,
                DEFAULT_BUFFER_SIZE,
                DEFAULT_MIN_AVAILABLE);
    }

    /**
     * Constructor. With this constructor, the buffer size and the minimum
     * available amount can be specified as parameters.
     *
     * @param outputFormat
     * @param lLength length of this stream in frames. May be
     * AudioSystem.NOT_SPECIFIED.
     *
     * @param nBufferSize size of the circular buffer in bytes.
     * @param nMinAvailable
     */
    public AsynchronousFilteredAudioInputStream(
            AudioFormat outputFormat, long lLength,
            int nBufferSize,
            int nMinAvailable) {
        /*	The usage of a ByteArrayInputStream is a hack.
         *	(the infamous "JavaOne hack", because I did it on June
         *	6th 2000 in San Francisco, only hours before a
         *	JavaOne session where I wanted to show mp3 playback
         *	with Java Sound.) It is necessary because in the FCS
         *	version of the Sun jdk1.3, the constructor of
         *	AudioInputStream throws an exception if its first
         *	argument is null. So we have to pass a dummy non-null
         *	value.
         */
        super(new ByteArrayInputStream(EMPTY_BYTE_ARRAY),
                outputFormat,
                lLength);
        LOG.log(Level.FINE, "TAsynchronousFilteredAudioInputStream.<init>(): begin");
        m_circularBuffer = new CircularBuffer(
                nBufferSize,
                false, // blocking read
                true, // blocking write
                this);	// trigger
        LOG.log(Level.FINE, "TAsynchronousFilteredAudioInputStream.<init>(): end");
    }

    /**
     * Returns the circular buffer.
     *
     * @return
     */
    protected CircularBuffer getCircularBuffer() {
        return m_circularBuffer;
    }

    @Override
    public int read()
            throws IOException {
        // if (TDebug.TraceAudioConverter) { TDebug.out("TAsynchronousFilteredAudioInputStream.read(): begin"); }
        int nByte;
        if (m_abSingleByte == null) {
            m_abSingleByte = new byte[1];
        }
        int nReturn = read(m_abSingleByte);
        if (nReturn == -1) {
            nByte = -1;
        } else {
            //$$fb 2001-04-14 nobody really knows that...
            nByte = m_abSingleByte[0] & 0xFF;
        }
        // if (TDebug.TraceAudioConverter) { TDebug.out("TAsynchronousFilteredAudioInputStream.read(): end"); }
        return nByte;
    }

    @Override
    public int read(byte[] abData)
            throws IOException {
        LOG.log(Level.FINE, "TAsynchronousFilteredAudioInputStream.read(byte[]): begin");
        int nRead = read(abData, 0, abData.length);
        LOG.log(Level.FINE, "TAsynchronousFilteredAudioInputStream.read(byte[]): end");
        return nRead;
    }

    @Override
    public int read(byte[] abData, int nOffset, int nLength)
            throws IOException {
        LOG.log(Level.FINE, "TAsynchronousFilteredAudioInputStream.read(byte[], int, int): begin");
        //$$fb 2001-04-22: this returns at maximum circular buffer
        // length. This is not very efficient...
        //$$fb 2001-04-25: we should check that we do not exceed getFrameLength() !
        int nRead = m_circularBuffer.read(abData, nOffset, nLength);
        LOG.log(Level.FINE, "TAsynchronousFilteredAudioInputStream.read(byte[], int, int): end");
        return nRead;
    }

    @Override
    public long skip(long lSkip)
            throws IOException {
        // TODO: this is quite inefficient
        for (long lSkipped = 0; lSkipped < lSkip; lSkipped++) {
            int nReturn = read();
            if (nReturn == -1) {
                return lSkipped;
            }
        }
        return lSkip;
    }

    @Override
    public int available()
            throws IOException {
        return m_circularBuffer.availableRead();
    }

    @Override
    public void close()
            throws IOException {
        m_circularBuffer.close();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int nReadLimit) {
    }

    @Override
    public void reset()
            throws IOException {
        throw new IOException("mark not supported");
    }
}
