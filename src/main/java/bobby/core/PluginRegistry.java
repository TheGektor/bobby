package bobby.core;

import java.util.HashMap;
import java.util.Map;

public class PluginRegistry {
    
    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> clazz, T instance) {
        services.put(clazz, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        return (T) services.get(clazz);
    }
}
