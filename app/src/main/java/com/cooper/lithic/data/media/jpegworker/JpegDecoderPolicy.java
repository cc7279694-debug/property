package com.cooper.lithic.data.media.jpegworker;

/** Fixed candidate decoder/normalization policies; unknown versions are never silently accepted. */
public final class JpegDecoderPolicy {
    public static final String DECODER="libjpeg-turbo-3.2.0-e01-strict-v1";
    public static final String NORMALIZATION="m10-jpeg-normalize-v1";
    private JpegDecoderPolicy() {}
    public static boolean matches(String decoder,String normalization) {
        return DECODER.equals(decoder)&&NORMALIZATION.equals(normalization);
    }
}
