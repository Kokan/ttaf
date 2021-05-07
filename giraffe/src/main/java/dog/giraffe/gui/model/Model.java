package dog.giraffe.gui.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Model {
    public String inputFile;
    public List<HalfPlane> mask;
    public List<Output> outputs;

    public static Model create() {
        Model model=new Model();
        model.fix();
        return model;
    }

    static <T> List<T> fix(List<T> list) {
        return (null==list)
                ?new ArrayList<>()
                :new ArrayList<>(list);
    }

    static <T> T fix(T value, T defaultValue) {
        return (null==value)
                ?defaultValue
                :value;
    }

    private void fix() {
        mask=fix(mask);
        outputs=fix(outputs);
        if (outputs.isEmpty()) {
            outputs.add(new Output());
        }
        outputs.forEach(Output::fix);
    }

    public static Model load(Path path) throws Throwable {
        Model model=objectMapper().readValue(path.toFile(), Model.class);
        model.fix();
        return model;
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper=new ObjectMapper();
        objectMapper=objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator());
        objectMapper=objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return objectMapper;
    }

    public void save(Path path) throws Throwable {
        objectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this);
    }
}
