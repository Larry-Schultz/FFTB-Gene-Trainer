package fft_battleground;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class RaceConditionTime {

	public static ExecutorService executorService = Executors.newFixedThreadPool(10);
	public static int numberOfNumbers = 100;
	public static int numberOfLists = 25;
	public static int maxRange = 250;
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		List<List<Integer>> randomNumberLists = new ArrayList<>();
		Random random = new Random();
		for(int i = 0; i < numberOfLists; i++) {
			List<Integer> numberList = new LinkedList<>();
			for(int j = 0; j < numberOfNumbers; j++) {
				numberList.add(random.nextInt(maxRange));
			}
			randomNumberLists.add(numberList);
		}
		
		Lock lock = new ReentrantLock();
		
		//first try without lock
		System.out.println("first trying without lock");
		AtomicInteger highestSumWithoutLock = new AtomicInteger(0);
		List<Callable<Integer>> callablesWithoutLock = randomNumberLists.parallelStream()
				.map(numberList -> new SumCallable(numberList, highestSumWithoutLock, null)).collect(Collectors.toList());
		
		List<Future<Integer>> futureResults = executorService.invokeAll(callablesWithoutLock);
		List<Integer> results = getResults(futureResults);
		
		System.out.println("now try with lock");
		AtomicInteger highestSumWithLock = new AtomicInteger(0);
		List<Callable<Integer>> callablesWithLock = randomNumberLists.parallelStream()
				.map(numberList -> new SumCallable(numberList, highestSumWithLock, lock)).collect(Collectors.toList());
		
		
		futureResults = executorService.invokeAll(callablesWithLock);
		results = getResults(futureResults);
		
	}
	
	public static List<Integer> getResults(List<Future<Integer>> futures) throws InterruptedException, ExecutionException {
		List<Integer> results = new LinkedList<>();
		for(Future<Integer> possibleResult: futures) {
			results.add(possibleResult.get());
		}
		
		return results;
	}

}

class SumCallable implements Callable<Integer> {
	
	public List<Integer> numberList;
	public AtomicInteger largeNumberRef;
	public Lock largeNumberRefLock;
	
	public SumCallable(List<Integer> numberList, AtomicInteger largeNumberRef, Lock largeNumberLock) {
		this.numberList = numberList;
		this.largeNumberRef = largeNumberRef;
		this.largeNumberRefLock = largeNumberLock;
	}
	
	@Override
	public Integer call() {
		Integer sumOfNumbers = this.sum();
		if(this.largeNumberRefLock != null) {
			this.largeNumberRefLock.lock();
		}
		if(this.largeNumberRef.get() < sumOfNumbers) {
			System.out.println("Found new largest number " + sumOfNumbers.toString());
			this.largeNumberRef.set(sumOfNumbers);
		}
		if(this.largeNumberRefLock != null) {
			this.largeNumberRefLock.unlock();
		}
		
		return sumOfNumbers;
	}
	
	public Integer sum() {
		int sum = 0;
		for(Integer number: this.numberList) {
			sum += number;
		}
		
		return sum;
	}
}