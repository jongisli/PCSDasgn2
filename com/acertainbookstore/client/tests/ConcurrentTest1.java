package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

public class ConcurrentTest1 {

	//private static boolean concurrentlocalTest = true; 
	private static StockManager storeManager;
	private static BookStore client;
	
	
	@BeforeClass
	public static void setUpBeforeClass() {
	
		storeManager = ConcurrentCertainBookStore.getInstance();
		client = ConcurrentCertainBookStore.getInstance();
	}
	
	/**
	 * Here we want to test the atomicity of buying and adding books
	 * 
	 * 1. We add books from the Sagas of Icelanders
	 * 
	 * 2. We start a client (thread1) which buys one book multiple times
	 * 
	 * 3. We then start a client (thread2) which adds one copy multiple times
	 *
	 *
	 */
	
	@Test
	public void TestAtomicity() {
		
		int testISBN = 100;
		int amountOfBooks = 1000;
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(testISBN,
				"Egils saga Skalla-Gr’mssonar",
				"Viking Vikingsson", (float) 100, amountOfBooks, 0, 0, 0,
				false));
		try {
			storeManager.addBooks(booksToAdd);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		int repeats = 500;
		Thread client1 = new Thread(new buyBooksClient(testISBN, repeats));
		Thread client2 = new Thread(new addCopiesClient(testISBN, repeats));
		
		client1.start();
		client2.start();
		
		try {
			client1.join();
			client2.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			fail();
		}
		
		
		List<StockBook> listBooks = null;
		try {
			listBooks = storeManager.getBooks();
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		for (StockBook b : listBooks) {
			if (b.getISBN() == testISBN) {
				System.out.println(b.getNumCopies());
				assertTrue("Num copies  after buying one copy 10 times and adding one copy 10 times",
						b.getNumCopies() == amountOfBooks);
				break;
			}
		}
	}
	
	private class buyBooksClient implements Runnable {


			private StockManager storeManager;
			private BookStore client;
			private Set<BookCopy> theSagas;
			private int repeats;
			
			public buyBooksClient(int testISBN, int repeats)
			{
				this.storeManager = ConcurrentCertainBookStore.getInstance();
				this.client = ConcurrentCertainBookStore.getInstance();
				this.repeats = repeats;
				
				this.theSagas = new HashSet<BookCopy>();
				theSagas.add(new BookCopy(testISBN, 1));
			}
			
			@Override
			public void run() {
				try {
					for (int i = 0; i < repeats; i++)
					{
						client.buyBooks(theSagas);
					}
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
			}
				
		}


	private class addCopiesClient implements Runnable {
	
		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public addCopiesClient(int testISBN, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN, 1));
		}
		
		@Override
		public void run() {
			try {
				for (int i = 0; i < repeats; i++)
				{
					storeManager.addCopies(theSagas);
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
				fail();
			}
			
			
		}
			
	}		
}
