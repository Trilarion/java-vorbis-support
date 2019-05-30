/*
 * Copyright (C) 1999 by Matthias Pfisterer
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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author
 */
public class CircularBuffer {

    private static final Logger LOG = Logger.getLogger(CircularBuffer.class.getName());

    private final boolean m_bBlockingRead;
    private final boolean m_bBlockingWrite;
    private final byte[] m_abData;
    private final int m_nSize;
    private long m_lReadPos;
    private long m_lWritePos;
    private final BufferListener m_trigger;
    private boolean m_bOpen;

    /**
     *  Listener
     */
    public interface BufferListener {

        /**
         * Data is ready.
         */
        void dataReady();

    }

    /**
     *
     * @param nSize
     * @param bBlockingRead
     * @param bBlockingWrite
     * @param trigger
     */
    public CircularBuffer(int nSize, boolean bBlockingRead, boolean bBlockingWrite, BufferListener trigger) {
        m_bBlockingRead = bBlockingRead;
        m_bBlockingWrite = bBlockingWrite;
        m_nSize = nSize;
        m_abData = new byte[m_nSize];
        m_lReadPos = 0;
        m_lWritePos = 0;
        m_trigger = trigger;
        m_bOpen = true;
    }

    /**
     * Close
     */
    public void close() {
        m_bOpen = false;
        // TODO: call notify() ?
    }

    /**
     *
     */
    private boolean isOpen() {
        return m_bOpen;
    }

    /**
     *
     * @return
     */
    public int availableRead() {
        return (int) (m_lWritePos - m_lReadPos);
    }

    /**
     *
     * @return
     */
    public int availableWrite() {
        return m_nSize - availableRead();
    }

    private int getReadPos() {
        return (int) (m_lReadPos % m_nSize);
    }

    private int getWritePos() {
        return (int) (m_lWritePos % m_nSize);
    }

    /**
     *
     * @param abData
     * @param nOffset
     * @param nLength
     * @return
     */
    public int read(byte[] abData, int nOffset, int nLength) {
        LOG.log(Level.FINE, ">TCircularBuffer.read(): called.");
        dumpInternalState();
        if (!isOpen()) {
            if (availableRead() > 0) {
                nLength = Math.min(nLength, availableRead());
                LOG.log(Level.FINE, "reading rest in closed buffer, length: {0}", nLength);
            } else {
                LOG.log(Level.FINE, "< not open. returning -1.");
                return -1;
            }
        }
        synchronized (this) {
            if (m_trigger != null && availableRead() < nLength) {
                LOG.log(Level.FINE, "executing trigger.");
                m_trigger.dataReady();
            }
            if (!m_bBlockingRead) {
                nLength = Math.min(availableRead(), nLength);
            }
            int nRemainingBytes = nLength;
            while (nRemainingBytes > 0) {
                while (availableRead() == 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        LOG.log(Level.FINE, e.getMessage());
                    }
                }
                int nAvailable = Math.min(availableRead(), nRemainingBytes);
                while (nAvailable > 0) {
                    int nToRead = Math.min(nAvailable, m_nSize - getReadPos());
                    System.arraycopy(m_abData, getReadPos(), abData, nOffset, nToRead);
                    m_lReadPos += nToRead;
                    nOffset += nToRead;
                    nAvailable -= nToRead;
                    nRemainingBytes -= nToRead;
                }
                notifyAll();
            }
            LOG.log(Level.FINE, "After read:");
            // dumpInternalState();
            LOG.log(Level.FINE, "< completed. Read {0} bytes", nLength);
            return nLength;
        }
    }

    /**
     *
     * @param abData
     * @param nOffset
     * @param nLength
     * @return
     */
    public int write(byte[] abData, int nOffset, int nLength) {
        LOG.log(Level.FINE, ">TCircularBuffer.write(): called; nLength: {0}", nLength);
        dumpInternalState();
        synchronized (this) {
            LOG.log(Level.FINE, "entered synchronized block.");
            if (!m_bBlockingWrite) {
                nLength = Math.min(availableWrite(), nLength);
            }
            int nRemainingBytes = nLength;
            while (nRemainingBytes > 0) {
                while (availableWrite() == 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        LOG.log(Level.FINE, e.getMessage());
                    }
                }
                int nAvailable = Math.min(availableWrite(), nRemainingBytes);
                while (nAvailable > 0) {
                    int nToWrite = Math.min(nAvailable, m_nSize - getWritePos());
                    //TDebug.out("src buf size= " + abData.length + ", offset = " + nOffset + ", dst buf size=" + m_abData.length + " write pos=" + getWritePos() + " len=" + nToWrite);
                    System.arraycopy(abData, nOffset, m_abData, getWritePos(), nToWrite);
                    m_lWritePos += nToWrite;
                    nOffset += nToWrite;
                    nAvailable -= nToWrite;
                    nRemainingBytes -= nToWrite;
                }
                notifyAll();
            }
            LOG.log(Level.FINE, "After write:");
            dumpInternalState();
            LOG.log(Level.FINE, "< completed. Wrote {0} bytes", nLength);
            return nLength;
        }
    }

    private void dumpInternalState() {
        LOG.log(Level.FINE, "m_lReadPos  = {0} ^= {1}", new Object[]{m_lReadPos, getReadPos()});
        LOG.log(Level.FINE, "m_lWritePos = {0} ^= {1}", new Object[]{m_lWritePos, getWritePos()});
        LOG.log(Level.FINE, "availableRead()  = {0}", availableRead());
        LOG.log(Level.FINE, "availableWrite() = {0}", availableWrite());
    }
}
