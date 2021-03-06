package dog.giraffe;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;

public class Icons {
    private static final List<Integer> SIZES=List.of(512, 256, 128, 64, 32);

    private Icons() {
    }

    public static List<Image> icons() throws Throwable {
        List<Image> list=new ArrayList<>(SIZES.size());
        ClassLoader classLoader=Thread.currentThread().getContextClassLoader();
        for (int size: SIZES) {
            String name=String.format("giraffe-%1$03d.png", size);

            try (InputStream is=Objects.requireNonNull(classLoader.getResourceAsStream(name), name);
                 InputStream bis=new BufferedInputStream(is)) {
                list.add(ImageIO.read(bis));
            }
        }
        return Collections.unmodifiableList(list);
    }
}
