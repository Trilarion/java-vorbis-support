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
package com.github.trilarion.sound.vorbis.sampled;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import com.github.trilarion.sound.sampled.AsynchronousFilteredAudioInputStream;
import com.github.trilarion.sound.sampled.CircularBuffer;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Packet;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Page;
import com.github.trilarion.sound.vorbis.jcraft.jogg.StreamState;
import com.github.trilarion.sound.vorbis.jcraft.jogg.SyncState;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.Block;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.Comment;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.DspState;
import com.github.trilarion.sound.vorbis.jcraft.jorbis.Info;

/**
 * This class implements the Vorbis decoding.
 */
public class DecodedVorbisAudioInputStream extends AsynchronousFilteredAudioInputStream implements CircularBuffer.BufferListener {

    private static final Logger LOG = Logger.getLogger(DecodedVorbisAudioInputStream.class.getName());

    private InputStream oggBitStream_;

    private SyncState oggSyncState_ = null;
    private StreamState oggStreamState_ = null;
    private Page oggPage_ = null;
    private Packet oggPacket_ = null;
    private Info vorbisInfo = null;
    private Comment vorbisComment = null;
    private DspState vorbisDspState = null;
    private Block vorbisBlock = null;

    static final int playState_NeedHeaders = 0;
    static final int playState_ReadData = 1;
    static final int playState_WriteData = 2;
    static final int playState_Done = 3;
    static final int playState_BufferFull = 4;
    static final int playState_Corrupt = -1;
    private int playState;

    private final int bufferMultiple_ = 4;
    private final int bufferSize_ = bufferMultiple_ * 256 * 2;
    private int convsize = bufferSize_ * 2;
    private final byte[] convbuffer = new byte[convsize];
    private byte[] buffer = null;
    private int bytes = 0;
    private float[][][] _pcmf = null;
    private int[] _index = null;
    private int index = 0;
    private int i = 0;
    // bout is now a global so that we can continue from when we have a buffer full.
    int bout = 0;

    private long currentBytes = 0;

    /**
     * Constructor.
     *
     * @param outputFormat
     * @param bitStream
     */
    public DecodedVorbisAudioInputStream(AudioFormat outputFormat, AudioInputStream bitStream) {
        super(outputFormat, -1);
        this.oggBitStream_ = bitStream;
        init_jorbis();
        index = 0;
        playState = playState_NeedHeaders;
    }

    /**
     * Initializes all the jOrbis and jOgg vars that are used for song playback.
     */
    private void init_jorbis() {
        oggSyncState_ = new SyncState();
        oggStreamState_ = new StreamState();
        oggPage_ = new Page();
        oggPacket_ = new Packet();
        vorbisInfo = new Info();
        vorbisComment = new Comment();
        vorbisDspState = new DspState();
        vorbisBlock = new Block(vorbisDspState);
        buffer = null;
        bytes = 0;
        currentBytes = 0L;
        oggSyncState_.init();
    }

    /**
     * Main loop.
     */
    @Override
    public void dataReady() {
        switch (playState) {
            case playState_NeedHeaders:
                LOG.log(Level.FINE, "playState = playState_NeedHeaders");
                break;
            case playState_ReadData:
                LOG.log(Level.FINE, "playState = playState_ReadData");
                break;
            case playState_WriteData:
                LOG.log(Level.FINE, "playState = playState_WriteData");
                break;
            case playState_Done:
                LOG.log(Level.FINE, "playState = playState_Done");
                break;
            case playState_BufferFull:
                LOG.log(Level.FINE, "playState = playState_BufferFull");
                break;
            case playState_Corrupt:
                LOG.log(Level.FINE, "playState = playState_Corrupt");
                break;
        }
        // This code was developed by the jCraft group, as JOrbisPlayer.java,  slightly
        // modified by jOggPlayer developer and adapted by JavaZOOM to suit the JavaSound
        // SPI. Then further modified by Tom Kimpton to correctly play ogg files that
        // would hang the player.
        switch (playState) {
            case playState_NeedHeaders:
                try {
                    // Headers (+ Comments).
                    readHeaders();
                } catch (IOException ioe) {
                    playState = playState_Corrupt;
                    return;
                }
                playState = playState_ReadData;
                break;

            case playState_ReadData:
                int result;
                index = oggSyncState_.buffer(bufferSize_);
                buffer = oggSyncState_.data;
                bytes = readFromStream(buffer, index, bufferSize_);
                LOG.log(Level.FINE, "More data : {0}", bytes);
                if (bytes == -1) {
                    playState = playState_Done;
                    LOG.log(Level.FINE, "Ogg Stream empty. Settings playState to playState_Done.");
                    break;
                } else {
                    oggSyncState_.wrote(bytes);
                    if (bytes == 0) {
                        if ((oggPage_.eos() != 0) || (oggStreamState_.e_o_s != 0) || (oggPacket_.e_o_s != 0)) {
                            LOG.log(Level.FINE, "oggSyncState wrote 0 bytes: settings playState to playState_Done.");
                            playState = playState_Done;
                        }
                        LOG.log(Level.FINE, "oggSyncState wrote 0 bytes: but stream not yet empty.");
                        break;
                    }
                }

                result = oggSyncState_.pageout(oggPage_);
                if (result == 0) {
                    LOG.log(Level.FINE, "Setting playState to playState_ReadData.");
                    playState = playState_ReadData;
                    break;
                } // need more data
                if (result == -1) { // missing or corrupt data at this page position
                    LOG.log(Level.FINE, "Corrupt or missing data in bitstream; setting playState to playState_ReadData");
                    playState = playState_ReadData;
                    break;
                }

                oggStreamState_.pagein(oggPage_);

                LOG.log(Level.FINE, "Setting playState to playState_WriteData.");
                playState = playState_WriteData;
                break;

            case playState_WriteData:
                // Decoding !
                LOG.log(Level.FINE, "Decoding");
                label:
                while (true) {
                    result = oggStreamState_.packetout(oggPacket_);
                    switch (result) {
                        case 0:
                            LOG.log(Level.FINE, "Packetout returned 0, going to read state.");
                            playState = playState_ReadData;
                            break label;
                        case -1:
                            // missing or corrupt data at this page position
                            // no reason to complain; already complained above
                            LOG.log(Level.FINE, "Corrupt or missing data in packetout bitstream; going to read state...");
                            // playState = playState_ReadData;
                            // break;
                            // continue;
                            break;
                        default:
                            // we have a packet.  Decode it
                            if (vorbisBlock.synthesis(oggPacket_) == 0) { // test for success!
                                vorbisDspState.synthesis_blockin(vorbisBlock);
                            } else {
                                //if(TDebug.TraceAudioConverter) TDebug.out("vorbisBlock.synthesis() returned !0, going to read state");
                                LOG.log(Level.FINE, "VorbisBlock.synthesis() returned !0, continuing.");
                                continue;
                            }

                            outputSamples();
                            if (playState == playState_BufferFull) {
                                return;
                            }

                            break;
                    }
                } // while(true)
                if (oggPage_.eos() != 0) {
                    LOG.log(Level.FINE, "Settings playState to playState_Done.");
                    playState = playState_Done;
                }
                break;
            case playState_BufferFull:
                continueFromBufferFull();
                break;

            case playState_Corrupt:
                LOG.log(Level.FINE, "Corrupt Song.");
            // drop through to playState_Done...
            case playState_Done:
                oggStreamState_.clear();
                vorbisBlock.clear();
                vorbisDspState.clear();
                vorbisInfo.clear();
                oggSyncState_.clear();
                LOG.log(Level.FINE, "Done Song.");
                try {
                    if (oggBitStream_ != null) {
                        oggBitStream_.close();
                    }
                    getCircularBuffer().close();
                } catch (Exception e) {
                    LOG.log(Level.FINE, e.getMessage());
                }
                break;
        } // switch
    }

    /**
     * This routine was extracted so that when the output buffer fills up, we
     * can break out of the loop, let the music channel drain, then continue
     * from where we were.
     */
    private void outputSamples() {
        int samples;
        while ((samples = vorbisDspState.synthesis_pcmout(_pcmf, _index)) > 0) {
            float[][] pcmf = _pcmf[0];
            bout = (samples < convsize ? samples : convsize);
            // convert doubles to 16 bit signed ints (host order) and
            // interleave
            for (i = 0; i < vorbisInfo.channels; i++) {
                int pointer = i * 2;
                //int ptr=i;
                int mono = _index[i];
                for (int j = 0; j < bout; j++) {
                    double fVal = pcmf[i][mono + j] * 32767.;
                    int val = (int) (fVal);
                    if (val > 32767) {
                        val = 32767;
                    }
                    if (val < -32768) {
                        val = -32768;
                    }
                    if (val < 0) {
                        val = val | 0x8000;
                    }
                    convbuffer[pointer] = (byte) (val);
                    convbuffer[pointer + 1] = (byte) (val >>> 8);
                    pointer += 2 * (vorbisInfo.channels);
                }
            }
            LOG.log(Level.FINE, "about to write: {0}", 2 * vorbisInfo.channels * bout);
            if (getCircularBuffer().availableWrite() < 2 * vorbisInfo.channels * bout) {
                LOG.log(Level.FINE, "Too much data in this data packet, better return, let the channel drain, and try again...");
                playState = playState_BufferFull;
                return;
            }
            getCircularBuffer().write(convbuffer, 0, 2 * vorbisInfo.channels * bout);
            if (bytes < bufferSize_) {
                LOG.log(Level.FINE, "Finished with final buffer of music?");
            }
            if (vorbisDspState.synthesis_read(bout) != 0) {
                LOG.log(Level.FINE, "VorbisDspState.synthesis_read returned -1.");
            }
        } // while(samples...)
        playState = playState_ReadData;
    }

    private void continueFromBufferFull() {
        if (getCircularBuffer().availableWrite() < 2 * vorbisInfo.channels * bout) {
            LOG.log(Level.FINE, "Too much data in this data packet, better return, let the channel drain, and try again...");
            // Don't change play state.
            return;
        }
        getCircularBuffer().write(convbuffer, 0, 2 * vorbisInfo.channels * bout);
        // Don't change play state. Let outputSamples change play state, if necessary.
        outputSamples();
    }

    /**
     * Reads headers and comments.
     */
    private void readHeaders() throws IOException {
        LOG.log(Level.FINE, "readHeaders(");
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
        //int i = 0;
        i = 0;
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

        byte[][] ptr = vorbisComment.user_comments;

        for (byte[] ptr1 : ptr) {
            if (ptr1 == null) {
                break;
            }
            String currComment = (new String(ptr1, 0, ptr1.length - 1, Charset.forName("US-ASCII"))).trim();
            LOG.log(Level.FINE, "Comment: {0}", currComment);
        }
        convsize = bufferSize_ / vorbisInfo.channels;
        vorbisDspState.synthesis_init(vorbisInfo);
        vorbisBlock.init(vorbisDspState);
        _pcmf = new float[1][][];
        _index = new int[vorbisInfo.channels];
    }

    /**
     * Reads from the oggBitStream_ a specified number of Bytes(bufferSize_)
     * worth starting at index and puts them in the specified buffer[].
     *
     * @param buffer
     * @param index
     * @param bufferSize_
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
        currentBytes = currentBytes + bytes;
        return bytes;
    }

    /**
     * Close the stream.
     *
     * @throws java.io.IOException
     */
    @Override
    public void close() throws IOException {
        super.close();
        oggBitStream_.close();
    }
}
