package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegDecoderPolicyTest {
 @Test public void fixedVersionMatches(){assertTrue(JpegDecoderPolicy.matches("libjpeg-turbo-3.2.0-e01-strict-v1","m10-jpeg-normalize-v1"));}
 @Test public void missingDecoderRejected(){assertFalse(JpegDecoderPolicy.matches(null,JpegDecoderPolicy.NORMALIZATION));}
 @Test public void missingNormalizerRejected(){assertFalse(JpegDecoderPolicy.matches(JpegDecoderPolicy.DECODER,null));}
 @Test public void oldDecoderRejected(){assertFalse(JpegDecoderPolicy.matches("libjpeg-turbo-3.1.0",JpegDecoderPolicy.NORMALIZATION));}
 @Test public void unknownDecoderSuffixRejected(){assertFalse(JpegDecoderPolicy.matches(JpegDecoderPolicy.DECODER+"-other",JpegDecoderPolicy.NORMALIZATION));}
 @Test public void normalizerChangeRejected(){assertFalse(JpegDecoderPolicy.matches(JpegDecoderPolicy.DECODER,"m10-jpeg-normalize-v2"));}
 @Test public void whitespaceNotCanonicalized(){assertFalse(JpegDecoderPolicy.matches(" "+JpegDecoderPolicy.DECODER,JpegDecoderPolicy.NORMALIZATION));}
}
