package org.rafaelia.rafffft;

/** Fixed-size, allocation-free, in-place radix-2 FFT. Data are signed Q16. */
public final class FftQ16 {
    private static final int Q = 65536;
    private static final int[] COS8 = {Q, 46341, 0, -46341};
    private static final int[] MINUS_SIN8 = {0, -46341, -Q, -46341};
    private FftQ16() {}

    /** Caller-owned arrays: 8 points, bounded magnitude (abs(input) <= Q). */
    public static void forward8(int[] real, int[] imaginary) {
        if (real == null || imaginary == null || real.length != 8 || imaginary.length != 8)
            throw new IllegalArgumentException("FFT_N8_ONLY");
        for (int i=0; i<8; i++) {
            if (real[i] < -Q || real[i] > Q || imaginary[i] < -Q || imaginary[i] > Q)
                throw new IllegalArgumentException("FFT_Q16_INPUT_RANGE");
        }
        // 3-bit reversal, in-place, no heap allocation.
        for (int i=0; i<8; i++) {
            int r = ((i&1)<<2) | (i&2) | ((i&4)>>2);
            if (r>i) {
                int t=real[i]; real[i]=real[r]; real[r]=t;
                t=imaginary[i]; imaginary[i]=imaginary[r]; imaginary[r]=t;
            }
        }
        for (int size=2; size<=8; size<<=1) {
            int half=size/2;
            for (int start=0; start<8; start+=size) {
                for (int j=0; j<half; j++) {
                    int k=j*(8/size);
                    int wr=COS8[k], wi=MINUS_SIN8[k];
                    int upper=start+j, lower=upper+half;
                    int ar=real[upper], ai=imaginary[upper];
                    int br=real[lower], bi=imaginary[lower];
                    int tr=(int)(((long)wr*br-(long)wi*bi)>>16);
                    int ti=(int)(((long)wr*bi+(long)wi*br)>>16);
                    real[upper]=ar+tr; imaginary[upper]=ai+ti;
                    real[lower]=ar-tr; imaginary[lower]=ai-ti;
                }
            }
        }
    }
}
