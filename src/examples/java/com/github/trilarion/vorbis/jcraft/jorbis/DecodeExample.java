/*
 * Copyright (C) 2000 ymnk
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
package com.github.trilarion.vorbis.jcraft.jorbis;

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
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Takes a vorbis bitstream from stdin and writes raw stereo PCM to stdout.
 * Decodes simple and chained OggVorbis files from beginning to end. is somewhat
 * more complex than the code below.
 */
class DecodeExample {

    private static final Logger LOG = Logger.getLogger(DecodeExample.class.getName());
    private static int convsize = 4096 * 2;
    private static final byte[] convbuffer = new byte[convsize]; // take 8k out of the data segment, not the stack

    public static void main(String[] arg) {
        InputStream input;
        try {
            input = new FileInputStream("Agogo.ogg");
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return;
        }

        SyncState oy = new SyncState(); // sync and verify incoming physical bitstream
        StreamState os = new StreamState(); // take physical pages, weld into a logical stream of packets
        Page og = new Page(); // one Ogg bitstream page.  Vorbis packets are inside
        Packet op = new Packet(); // one raw packet of data for decode

        Info vi = new Info(); // struct that stores all the static vorbis bitstream settings
        Comment vc = new Comment(); // struct that stores all the bitstream user comments
        DspState vd = new DspState(); // central working state for the packet->PCM decoder
        Block vb = new Block(vd); // local working space for packet->PCM decode

        byte[] buffer;
        int bytes;

        // Decode setup
        oy.init(); // Now we can read pages

        while (true) { // we repeat if the bitstream is chained
            int eos = 0;

            // grab some data at the head of the stream.  We want the first page
            // (which is guaranteed to be small and only contain the Vorbis
            // stream initial header) We need the first page to get the stream
            // serialno.
            // submit a 4k block to libvorbis' Ogg layer
            int index = oy.buffer(4096);
            buffer = oy.data;
            try {
                bytes = input.read(buffer, index, 4096);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            }
            oy.wrote(bytes);

            // Get the first page.
            if (oy.pageout(og) != 1) {
                // have we simply run out of data?  If so, we're done.
                if (bytes < 4096) {
                    break;
                }

                // error case.  Must not be Vorbis data
                LOG.log(Level.SEVERE, "Input does not appear to be an Ogg bitstream.");
                return;
            }

            // Get the serial number and set up the rest of decode.
            // serialno first; use it to set up a logical stream
            os.init(og.serialno());

            // extract the initial header from the first page and verify that the
            // Ogg bitstream is in fact Vorbis data
            // I handle the initial header first instead of just having the code
            // read all three Vorbis headers at once because reading the initial
            // header is an easy way to identify a Vorbis bitstream and it's
            // useful to see that functionality seperated out.
            vi.init();
            vc.init();
            if (os.pagein(og) < 0) {
                // error; stream version mismatch perhaps
                LOG.log(Level.SEVERE, "Error reading first page of Ogg bitstream data.");
                return;
            }

            if (os.packetout(op) != 1) {
                // no page? must not be vorbis
                LOG.log(Level.SEVERE, "Error reading initial header packet.");
                return;
            }

            if (vi.synthesis_headerin(vc, op) < 0) {
                // error case; not a vorbis header
                LOG.log(Level.SEVERE, "This Ogg bitstream does not contain Vorbis audio data.");
                return;
            }

            // At this point, we're sure we're Vorbis.  We've set up the logical
            // (Ogg) bitstream decoder.  Get the comment and codebook headers and
            // set up the Vorbis decoder
            // The next two packets in order are the comment and codebook headers.
            // They're likely large and may span multiple pages.  Thus we reead
            // and submit data until we get our two pacakets, watching that no
            // pages are missing.  If a page is missing, error out; losing a
            // header page is the only place where missing data is fatal. */
            int i = 0;
            while (i < 2) {
                while (i < 2) {

                    int result = oy.pageout(og);
                    if (result == 0) {
                        break; // Need more data
                    }          // Don't complain about missing or corrupt data yet.  We'll
                    // catch it at the packet output phase

                    if (result == 1) {
                        os.pagein(og); // we can ignore any errors here
                        // as they'll also become apparent
                        // at packetout
                        while (i < 2) {
                            result = os.packetout(op);
                            if (result == 0) {
                                break;
                            }
                            if (result == -1) {
                                // Uh oh; data at some point was corrupted or missing!
                                // We can't tolerate that in a header.  Die.
                                LOG.log(Level.SEVERE, "Corrupt secondary header.  Exiting.");
                                return;
                            }
                            vi.synthesis_headerin(vc, op);
                            i++;
                        }
                    }
                }
                // no harm in not checking before adding more
                index = oy.buffer(4096);
                buffer = oy.data;
                try {
                    bytes = input.read(buffer, index, 4096);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    return;
                }
                if (bytes == 0 && i < 2) {
                    LOG.log(Level.SEVERE, "End of file before finding all Vorbis headers!");
                    return;
                }
                oy.wrote(bytes);
            }

            // Throw the comments plus a few lines about the bitstream we're
            // decoding
            {
                byte[][] ptr = vc.user_comments;
                for (byte[] ptr1 : ptr) {
                    if (ptr1 == null) {
                        break;
                    }
                    LOG.log(Level.INFO, new String(ptr1, 0, ptr1.length - 1));
                }
                LOG.log(Level.INFO, "\nBitstream is {0} channel, {1}Hz", new Object[]{vi.channels, vi.rate});
                LOG.log(Level.INFO, "Encoded by: {0}\n", new String(vc.vendor, 0, vc.vendor.length - 1));
            }
            convsize = 4096 / vi.channels;

            // OK, got and parsed all three headers. Initialize the Vorbis
            //  packet->PCM decoder.
            vd.synthesis_init(vi); // central decode state
            vb.init(vd); // local state for most of the decode
            // so multiple block decodes can
            // proceed in parallel.  We could init
            // multiple vorbis_block structures
            // for vd here

            float[][][] _pcm = new float[1][][];
            int[] _index = new int[vi.channels];
            // The rest is just a straight decode loop until end of stream
            while (eos == 0) {
                while (eos == 0) {

                    int result = oy.pageout(og);
                    if (result == 0) {
                        break; // need more data
                    }
                    if (result == -1) { // missing or corrupt data at this page position
                        LOG.log(Level.INFO, "Corrupt or missing data in bitstream; continuing...");
                    } else {
                        os.pagein(og); // can safely ignore errors at
                        // this point
                        while (true) {
                            result = os.packetout(op);

                            if (result == 0) {
                                break; // need more data
                            }
                            if (result == -1) { // missing or corrupt data at this page position
                                // no reason to complain; already complained above
                            } else {
                                // we have a packet.  Decode it
                                int samples;
                                if (vb.synthesis(op) == 0) { // test for success!
                                    vd.synthesis_blockin(vb);
                                }

                                // **pcm is a multichannel float vector.  In stereo, for
                                // example, pcm[0] is left, and pcm[1] is right.  samples is
                                // the size of each channel.  Convert the float values
                                // (-1.<=range<=1.) to whatever PCM format and write it out
                                while ((samples = vd.synthesis_pcmout(_pcm, _index)) > 0) {
                                    float[][] pcm = _pcm[0];
                                    int bout = (samples < convsize ? samples : convsize);

                                    // convert floats to 16 bit signed ints (host order) and
                                    // interleave
                                    for (i = 0; i < vi.channels; i++) {
                                        int ptr = i * 2;
                                        //int ptr=i;
                                        int mono = _index[i];
                                        for (int j = 0; j < bout; j++) {
                                            int val = (int) (pcm[i][mono + j] * 32767.);
                                            //		      short val=(short)(pcm[i][mono+j]*32767.);
                                            //		      int val=(int)Math.round(pcm[i][mono+j]*32767.);
                                            // might as well guard against clipping
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

                                    System.out.write(convbuffer, 0, 2 * vi.channels * bout);

                                    // tell libvorbis how
                                    // many samples we
                                    // actually consumed
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
                    index = oy.buffer(4096);
                    buffer = oy.data;
                    try {
                        bytes = input.read(buffer, index, 4096);
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        return;
                    }
                    oy.wrote(bytes);
                    if (bytes == 0) {
                        eos = 1;
                    }
                }
            }

            // clean up this logical bitstream; before exit we see if we're
            // followed by another [chained]
            os.clear();

            // ogg_page and ogg_packet structs always point to storage in
            // libvorbis.  They're never freed or manipulated directly
            vb.clear();
            vd.clear();
            vi.clear(); // must be called last
        }

        // OK, clean up the framer
        oy.clear();
        LOG.log(Level.INFO, "Done.");
    }
}
