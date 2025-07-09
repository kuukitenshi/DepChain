package pt.ulisboa.tecnico.sec.depchain.node.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;

public class ServiceContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceContainer.class);

    private final Map<Class<?>, RegisteredService<?>> registered = new HashMap<>();
    private final Map<Class<?>, Object> resources = new HashMap<>();
    private final BlockingQueue<Object> messageQueue = new LinkedBlockingDeque<>();

    public ServiceContainer() {
        LOGGER.info("created new " + getClass().getSimpleName());
    }

    public <T> void register(Class<T> clazz) {
        if (this.registered.containsKey(clazz)) {
            throw new ServiceException("Service already registered!");
        }
        ServiceHandle<T> handle = new ServiceHandle<>(this);
        Constructor<T> constructor;
        try {
            constructor = clazz.getDeclaredConstructor(ServiceHandle.class);
            T service = constructor.newInstance(handle);
            this.registered.put(clazz, new RegisteredService<T>(service, handle));
            LOGGER.info("registered new service " + clazz.getSimpleName());
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new ServiceException("Failed to register service " + clazz, e);
        }
    }

    public <T> void addResource(Class<? super T> clazz, T instance) {
        if (this.resources.containsKey(clazz)) {
            throw new ServiceException(String.format("Resource %s already registered!", clazz.getSimpleName()));
        }
        this.resources.put(clazz, instance);
        LOGGER.info("added new resource " + clazz.getSimpleName());
    }

    public void init() {
        LOGGER.info("initializing services...");
        for (RegisteredService<?> service : this.registered.values()) {
            Object instance = service.instance();
            try {
                Optional<Method> initOpt = Arrays.stream(instance.getClass().getDeclaredMethods())
                        .filter(m -> m.getName().equals("init")).findFirst();
                if (initOpt.isEmpty()) {
                    continue;
                }
                Method method = initOpt.get();
                Class<?>[] types = method.getParameterTypes();
                Object[] args = new Object[types.length];
                for (int i = 0; i < types.length; i++) {
                    Class<?> type = types[i];
                    if (!this.resources.containsKey(type)) {
                        throw new RuntimeException("Resource not registered!");
                    }
                    args[i] = this.resources.get(type);
                }
                method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServiceException("Failed to initialize container!", e);
            }
        }
        LOGGER.info("service initialization finished");
    }

    public void start() {
        LOGGER.info("starting container");
        List<Runnable> runnables = this.registered.values().stream().map(x -> x.instance())
                .filter(Runnable.class::isInstance).map(Runnable.class::cast).toList();
        if (runnables.size() > 0) {
            ExecutorService es = Executors.newFixedThreadPool(runnables.size());
            runnables.forEach(x -> es.submit(() -> x.run()));
            es.shutdown();

            // to make all messages be sent by the same thread, 'main' thread in this case
            while (!es.isTerminated()) {
                try {
                    Object message = this.messageQueue.take();
                    this.registered.values().stream()
                            .map(x -> x.handle())
                            .filter(x -> x.getListens().containsKey(message.getClass()))
                            .map(x -> x.getListens().get(message.getClass()))
                            .forEach(x -> x.accept(message));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        LOGGER.info("container finished execution");
    }

    public void publish(Object message) {
        this.messageQueue.add(message);
    }

    public Object retrieve(Class<?> clazz) {
        Optional<Supplier<Object>> supplier = this.registered.values().stream()
                .map(x -> x.handle().getSupplies())
                .filter(x -> x.containsKey(clazz))
                .map(x -> x.get(clazz))
                .findFirst();
        if (supplier.isEmpty()) {
            throw new ServiceException("not supplier found for " + clazz.getSimpleName());
        }
        return supplier.get().get();
    }

}
