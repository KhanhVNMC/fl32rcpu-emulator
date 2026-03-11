package dev.gkvn.cpu.utils;

import java.util.function.Consumer;

public class SingletonEventSource<T> {
	private Consumer<T> consumer;
	
	public void dispatch(T data) {
		if (consumer != null) consumer.accept(data);
	}
	
	public void setListener(Consumer<T> consumer) {
		this.consumer = consumer;
	}
}
