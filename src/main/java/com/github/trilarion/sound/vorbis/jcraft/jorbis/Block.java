/*
 * Copyright (C) 2000 ymnk<ymnk@jcraft.com>
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
package com.github.trilarion.sound.vorbis.jcraft.jorbis;

import java.util.logging.Logger;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Buffer;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Packet;

/**
 *
 */
public class Block {

    private static final Logger LOG = Logger.getLogger(Block.class.getName());

    ///necessary stream state for linking to the framing abstraction
    float[][] pcm = new float[0][]; // this is a pointer into local storage
    Buffer opb = new Buffer();

    int lW;
    int W;
    int nW;
    int pcmend;
    int mode;

    int eofflag;
    long granulepos;
    long sequence;
    DspState vd; // For read-only access of configuration

    // bitmetrics for the frame
    int glue_bits;
    int time_bits;
    int floor_bits;
    int res_bits;

    /**
     *
     * @param vd
     */
    public Block(DspState vd) {
        this.vd = vd;
        if (vd.analysisp != 0) {
            opb.writeinit();
        }
    }

    /**
     *
     * @param vd
     */
    public void init(DspState vd) {
        this.vd = vd;
    }

    /**
     *
     * @return
     */
    public int clear() {
        if (vd != null) {
            if (vd.analysisp != 0) {
                opb.writeclear();
            }
        }
        return (0);
    }

    /**
     *
     * @param op
     * @return
     */
    public int synthesis(Packet op) {
        Info vi = vd.vi;

        // first things first.  Make sure decode is ready
        opb.readinit(op.packet_base, op.packet, op.bytes);

        // Check the packet type
        if (opb.read(1) != 0) {
            // Oops.  This is not an audio data packet
            return (-1);
        }

        // read our mode and pre/post windowsize
        int _mode = opb.read(vd.modebits);
        if (_mode == -1) {
            return (-1);
        }

        mode = _mode;
        W = vi.mode_param[mode].blockflag;
        if (W != 0) {
            lW = opb.read(1);
            nW = opb.read(1);
            if (nW == -1) {
                return (-1);
            }
        } else {
            lW = 0;
            nW = 0;
        }

        // more setup
        granulepos = op.granulepos;
        sequence = op.packetno - 3; // first block is third packet
        eofflag = op.e_o_s;

        // alloc pcm passback storage
        pcmend = vi.blocksizes[W];
        if (pcm.length < vi.channels) {
            pcm = new float[vi.channels][];
        }
        for (int i = 0; i < vi.channels; i++) {
            if (pcm[i] == null || pcm[i].length < pcmend) {
                pcm[i] = new float[pcmend];
            } else {
                for (int j = 0; j < pcmend; j++) {
                    pcm[i][j] = 0;
                }
            }
        }

        // unpack_header enforces range checking
        int type = vi.map_type[vi.mode_param[mode].mapping];
        return (FuncMapping.mapping_P[type].inverse(this, vd.mode[mode]));
    }
}
