package com.yiqiniu.easytrans.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureAdapter<R> implements Future<R> {

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new RuntimeException("Not support Opeartion");
	}

	@Override
	public boolean isCancelled() {
		throw new RuntimeException("Not support Opeartion");
	}

	@Override
	public boolean isDone() {
		throw new RuntimeException("Not support Opeartion");
	}

	@Override
	public R get() throws InterruptedException, ExecutionException {
		throw new RuntimeException("Not support Opeartion");
	}

	@Override
	public R get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException,
			TimeoutException {
		throw new RuntimeException("Not support Opeartion");
	}

}
