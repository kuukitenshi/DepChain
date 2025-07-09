package pt.ulisboa.tecnico.sec.depchain.node.container;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServiceHandle<T> {

    private final Map<Class<?>, Consumer<Object>> listens = new HashMap<>();
    private final Map<Class<?>, Supplier<Object>> supplies = new HashMap<>();
    private final Map<Pair<Class<?>, Class<?>>, Function<Object, Object>> responds = new HashMap<>();
    private final Set<Class<?>> publishes = new HashSet<>();
    private final ServiceContainer container;

    private boolean initialized = false;

    public ServiceHandle(ServiceContainer container) {
        this.container = container;
    }

    public <M> void listens(Class<M> clazz, Consumer<M> consumer) {
        if (this.initialized) {
            throw new ServiceException("Service already initialized, cannot modify!");
        }
        if (this.listens.containsKey(clazz)) {
            throw new ServiceException("Service already listens to " + clazz);
        }
        this.listens.put(clazz, wrapConsumer(clazz, consumer));
    }

    public <M, N> void responds(Class<M> requestClass, Class<N> responseClass, Function<M, N> function) {
        if (this.initialized) {
            throw new ServiceException("Service already initialized, cannot modify!");
        }
        Pair<Class<?>, Class<?>> pair = new Pair<>(requestClass, responseClass);
        if (this.responds.containsKey(pair)) {
            throw new ServiceException("Responds pair already registered!");
        }
        this.responds.put(pair, wrapFunction(requestClass, responseClass, function));
    }

    public <M> void publishes(Class<M> clazz) {
        if (this.initialized) {
            throw new ServiceException("Service already initialized, cannot modify!");
        }
        if (this.publishes.contains(clazz)) {
            throw new ServiceException("Service already publishes " + clazz);
        }
        this.publishes.add(clazz);
    }

    public <M> void supplies(Class<M> clazz, Supplier<M> supplier) {
        if (this.initialized) {
            throw new ServiceException("Service already initialized, cannot modify!");
        }
        if (this.publishes.contains(clazz)) {
            throw new ServiceException("Service already publishes " + clazz);
        }
        this.supplies.put(clazz, wrapSupplier(clazz, supplier));
    }

    public void publish(Object message) {
        if (!this.publishes.contains(message.getClass())) {
            throw new ServiceException("Service tried to publish message not in publishes set!");
        }
        this.container.publish(message);
    }

    public <M> M retrieve(Class<M> clazz) {
        return clazz.cast(this.container.retrieve(clazz));
    }

    public void init() {
        this.initialized = true;
    }

    public Map<Class<?>, Consumer<Object>> getListens() {
        return listens;
    }

    public Set<Class<?>> getPublishes() {
        return publishes;
    }

    public Map<Class<?>, Supplier<Object>> getSupplies() {
        return supplies;
    }

    public Map<Pair<Class<?>, Class<?>>, Function<Object, Object>> getResponds() {
        return responds;
    }

    private <M> Consumer<Object> wrapConsumer(Class<M> clazz, Consumer<M> consumer) {
        return (object) -> consumer.accept(clazz.cast(object));
    }

    private <M, N> Function<Object, Object> wrapFunction(Class<M> first, Class<N> second, Function<M, N> function) {
        return (object) -> function.apply(first.cast(object));
    }

    private <M> Supplier<Object> wrapSupplier(Class<M> clazz, Supplier<M> supplier) {
        return () -> supplier.get();
    }

}
