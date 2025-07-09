package pt.ulisboa.tecnico.sec.depchain.node.container;

public record RegisteredService<T>(T instance, ServiceHandle<T> handle) {

}
