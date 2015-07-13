/*
 * Copyright (C) 2010 Trilarion
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
package com.github.trilarion.vorbis.jcraft;

import com.github.trilarion.jcraft.jogg.Packet;
import com.github.trilarion.jcraft.jogg.Page;
import com.github.trilarion.jcraft.jogg.StreamState;
import com.github.trilarion.jcraft.jogg.SyncState;
import com.github.trilarion.jcraft.jorbis.Block;
import com.github.trilarion.jcraft.jorbis.Comment;
import com.github.trilarion.jcraft.jorbis.DspState;
import com.github.trilarion.jcraft.jorbis.Info;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Based on JOrbisPlayer, but reduced to pure ogg file playback.
 */
// TODO is multithreading incorporated correctly?
public class OggPlayer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(OggPlayer.class.getName());
    private static final int Channels = 2;
    private static final int Rate = 44100;
    private final int BUFSIZE = 4096 * 2;
    private int convsize = BUFSIZE * 2; // 2 channels at most
    private final byte[] convbuffer = new byte[convsize];
    private SyncState oy;
    private StreamState os;
    private Page og;
    private Packet op;
    private Info vi;
    private Comment vc;
    private DspState vd;
    private Block vb;
    private byte[] buffer = null;
    private int bytes = 0;
    private SourceDataLine outputLine = null;

    /**
     *
     * @param line
     */
    public OggPlayer(SourceDataLine line) {
        outputLine = line;
    }

    /**
     *
     * @param arg
     */
    public static void main(String[] arg) {
        // get a line and construct a new player
        AudioFormat requestedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, Rate, 16, Channels, 4, Rate, false);
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(requestedFormat);
            line.open();
        } catch (LineUnavailableException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return;
        }
        line.start();
        OggPlayer player = new OggPlayer(line);

        InputStream in;
        try {
            in = new FileInputStream("Agogo.ogg");
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return;
        }
        player.play(in);

        line.stop();
        line.close();
    }

    /**
     * inits jorbis
     */
    private void init_jorbis() {
        oy = new SyncState();
        os = new StreamState();
        og = new Page();
        op = new Packet();

        vi = new Info();
        vc = new Comment();
        vd = new DspState();
        vb = new Block(vd);

        buffer = null;
        bytes = 0;

        oy.init();
    }

    /**
     * Called from run
     *
     * @param is
     */
    public synchronized void play(InputStream is) {
        boolean chained = false;
        init_jorbis();

        LOG.log(Level.INFO, "play stream: {0}", is);

        loop:
        while (true) {
            int eos = 0;

            int index = oy.buffer(BUFSIZE);
            buffer = oy.data;
            try {
                bytes = is.read(buffer, index, BUFSIZE);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            }

            oy.wrote(bytes);

            if (chained) { //
                chained = false; //
            } //
            else { //
                if (oy.pageout(og) != 1) {
                    if (bytes < BUFSIZE) {
                        break;
                    }
                    LOG.info("Input does not appear to be an Ogg bitstream.");
                    return;
                }
            } //
            os.init(og.serialno());
            os.reset();

            vi.init();
            vc.init();

            if (os.pagein(og) < 0) {
                // error; stream version mismatch perhaps
                LOG.info("Error reading first page of Ogg bitstream data.");
                return;
            }

            if (os.packetout(op) != 1) {
                // no page? must not be vorbis
                LOG.info("Error reading initial header packet.");
                break;
                //      return;
            }

            if (vi.synthesis_headerin(vc, op) < 0) {
                // error case; not a vorbis header
                LOG.info("This Ogg bitstream does not contain Vorbis audio data.");
                return;
            }

            int i = 0;

            while (i < 2) {
                while (i < 2) {
                    int result = oy.pageout(og);
                    if (result == 0) {
                        break; // Need more data
                    }
                    if (result == 1) {
                        os.pagein(og);
                        while (i < 2) {
                            result = os.packetout(op);
                            if (result == 0) {
                                break;
                            }
                            if (result == -1) {
                                LOG.info("Corrupt secondary header.  Exiting.");
                                //return;
                                break loop;
                            }
                            vi.synthesis_headerin(vc, op);
                            i++;
                        }
                    }
                }

                index = oy.buffer(BUFSIZE);
                buffer = oy.data;
                try {
                    bytes = is.read(buffer, index, BUFSIZE);
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }

                if (bytes == 0 && i < 2) {
                    LOG.info("End of file before finding all Vorbis headers!");
                    return;
                }
                oy.wrote(bytes);
            }

            {
                byte[][] ptr = vc.user_comments;

                for (byte[] ptr1 : ptr) {
                    if (ptr1 == null) {
                        break;
                    }
                    LOG.log(Level.INFO, "Comment: {0}", new String(ptr1, 0, ptr1.length - 1));
                }
                LOG.log(Level.INFO, "Bitstream is {0} channel, {1}Hz", new Object[]{vi.channels, vi.rate});
                LOG.log(Level.INFO, "Encoded by: {0}\n", new String(vc.vendor, 0, vc.vendor.length - 1));
            }

            convsize = BUFSIZE / vi.channels;

            vd.synthesis_init(vi);
            vb.init(vd);

            float[][][] _pcmf = new float[1][][];
            int[] _index = new int[vi.channels];

            // test for rate, channels not 44100, 2
            if (vi.channels != Channels || vi.rate != Rate) {
                LOG.log(Level.SEVERE, "Encountered sound file with unusual channels and rates, should get new line!");
                return;
            }

            while (eos == 0) {
                while (eos == 0) {
                    int result = oy.pageout(og);
                    if (result == 0) {
                        break; // need more data
                    }
                    if (result == -1) { // missing or corrupt data at this page position
                        LOG.info("Corrupt or missing data in bitstream; continuing...");
                    } else {
                        os.pagein(og);

                        if (og.granulepos() == 0) { //
                            chained = true; //
                            eos = 1; //
                            break; //
                        } //

                        while (true) {
                            result = os.packetout(op);
                            if (result == 0) {
                                break; // need more data
                            }
                            if (result == -1) { // missing or corrupt data at this page position
                                // no reason to complain; already complained above
                                //System.err.println("no reason to complain; already complained above");
                            } else {
                                // we have a packet.  Decode it
                                int samples;
                                if (vb.synthesis(op) == 0) { // test for success!
                                    vd.synthesis_blockin(vb);
                                }
                                while ((samples = vd.synthesis_pcmout(_pcmf, _index)) > 0) {
                                    float[][] pcmf = _pcmf[0];
                                    int bout = (samples < convsize ? samples : convsize);

                                    // convert doubles to 16 bit signed ints (host order) and
                                    // interleave
                                    for (i = 0; i < vi.channels; i++) {
                                        int ptr = i * 2;
                                        //int ptr=i;
                                        int mono = _index[i];
                                        for (int j = 0; j < bout; j++) {
                                            int val = (int) (pcmf[i][mono + j] * 32767.);
                                            if (val > 32767) {
                                                val = 32767;
                                            }
                                            if (val < -32768) {
                                                val = -32768;
                                            }
                                            if (val < 0) {
                                                val |= 0x8000;
                                            }
                                            convbuffer[ptr] = (byte) (val);
                                            convbuffer[ptr + 1] = (byte) (val >>> 8);
                                            ptr += 2 * (vi.channels);
                                        }
                                    }
                                    outputLine.write(convbuffer, 0, 2 * vi.channels * bout);
                                    vd.synthesis_read(bout);
                                }
                            }
                        }
                        if (og.eos() != 0) {
                            eos = 1;
                        }
                    }
                }

                if (eos == 0) {
                    index = oy.buffer(BUFSIZE);
                    buffer = oy.data;
                    try {
                        bytes = is.read(buffer, index, BUFSIZE);
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        return;
                    }

                    if (bytes == -1) {
                        break;
                    }
                    oy.wrote(bytes);
                    if (bytes == 0) {
                        eos = 1;
                    }
                }
            }

            os.clear();
            vb.clear();
            vd.clear();
            vi.clear();
        }

        oy.clear();

        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
}
