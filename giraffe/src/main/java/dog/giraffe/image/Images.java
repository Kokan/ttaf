package dog.giraffe.image;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.util.Hashtable;
import java.util.Map;

public class Images {
    private static final Map<Integer, Integer> TYPES_BY_DIMENSION=Map.ofEntries(
            Map.entry(1, ColorSpace.TYPE_GRAY),
            Map.entry(2, ColorSpace.TYPE_2CLR),
            Map.entry(3, ColorSpace.TYPE_3CLR),
            Map.entry(4, ColorSpace.TYPE_4CLR),
            Map.entry(5, ColorSpace.TYPE_5CLR),
            Map.entry(6, ColorSpace.TYPE_6CLR),
            Map.entry(7, ColorSpace.TYPE_7CLR),
            Map.entry(8, ColorSpace.TYPE_8CLR),
            Map.entry(9, ColorSpace.TYPE_9CLR),
            Map.entry(10, ColorSpace.TYPE_ACLR),
            Map.entry(11, ColorSpace.TYPE_BCLR),
            Map.entry(12, ColorSpace.TYPE_CCLR),
            Map.entry(13, ColorSpace.TYPE_DCLR),
            Map.entry(14, ColorSpace.TYPE_ECLR),
            Map.entry(15, ColorSpace.TYPE_FCLR));

    private Images() {
    }

    public static int[] createBandOffsets(int dimensions) {
        int[] result=new int[dimensions];
        for (int ii=0; dimensions>ii; ++ii) {
            result[ii]=ii;
        }
        return result;
    }

    public static ColorSpace createColorSpace(int dimensions) {
        Integer type=TYPES_BY_DIMENSION.get(dimensions);
        if (null==type) {
            throw new RuntimeException("unsupported number of dimensions "+dimensions);
        }
        return new ColorSpace(type, dimensions) {
            @Override
            public float[] toRGB(float[] colorValue) {
                throw new RuntimeException();
            }

            @Override
            public float[] fromRGB(float[] rgbValue) {
                throw new RuntimeException();
            }

            @Override
            public float[] toCIEXYZ(float[] colorValue) {
                throw new RuntimeException();
            }

            @Override
            public float[] fromCIEXYZ(float[] colorValue) {
                throw new RuntimeException();
            }
        };
    }

    public static BufferedImage createUnsignedByte(int width, int height, int dimensions) {
        return new BufferedImage(
                new ComponentColorModel(
                        createColorSpace(dimensions),
                        false,
                        false,
                        ColorModel.OPAQUE,
                        DataBuffer.TYPE_BYTE),
                Raster.createInterleavedRaster(
                        DataBuffer.TYPE_BYTE,
                        width,
                        height,
                        dimensions,
                        new Point(0, 0)),
                false,
                new Hashtable<>());
    }
}
