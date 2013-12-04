package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

public class ConcurrentTest2 {
	
	private static StockManager storeManager;
	private static BookStore client;

	@BeforeClass
	public static void setUpBeforeClass() {
		storeManager = ConcurrentCertainBookStore.getInstance();
		client = ConcurrentCertainBookStore.getInstance();
	}
	
	/**
	 * Here we want to test the consistency of buying and adding books
	 * 
	 * 1. We add three books from the Sagas of Icelanders
	 * 
	 * 2. We start a client (thread1) which buys these three books and
	 * adds a copy of each one after having bought them all, multiple 
	 * times.
	 * 
	 * 3. We start a client (thread2) which calls getBooks() and makes
	 * sure either all the books from the Sagas are there or none, 
	 * multiple times.
	 *
	 */
	
	@Test
	public void TestConsistency() {
		int testISBN = 100;
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(testISBN,
				"Egils saga Skalla-Gr’mssonar",
				"Viking Vikingsson", (float) 100, 1, 0, 0, 0,
				false));
		booksToAdd.add(new ImmutableStockBook(testISBN + 1,
				"Brennu-Nj‡ls saga",
				"Viking Vikingsson", (float) 100, 1, 0, 0, 0,
				false));
		booksToAdd.add(new ImmutableStockBook(testISBN + 2,
				"G’sla saga Sœrssonar",
				"Viking Vikingsson", (float) 100, 1, 0, 0, 0,
				false));

		try {
			storeManager.addBooks(booksToAdd);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		int repeats = 1000;
		Thread client1 = new Thread(new BuyAddBooksClient(testISBN, repeats));
		Thread client2 = new Thread(new GetBooksClient(testISBN, repeats));
		
		client1.start();
		client2.start();
		
		try {
			client1.join();
			client2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
		
	}

	private class BuyAddBooksClient implements Runnable
	{
		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public BuyAddBooksClient(int testISBN, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN, 1));
			theSagas.add(new BookCopy(testISBN + 1, 1));
			theSagas.add(new BookCopy(testISBN + 2, 1));
		}
		
		@Override
		public void run() {
			try {
				for (int i = 0; i < repeats; i++)
				{
					client.buyBooks(theSagas);
					storeManager.addCopies(theSagas);
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
				fail();
			}
		}
	}
	
	private class GetBooksClient implements Runnable
	{
		private StockManager storeManager;
		private int repeats;
		private int testISBN;
		
		public GetBooksClient(int testISBN, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			this.testISBN = testISBN;
		}

		@Override
		public void run() {
			try {
				List<StockBook> books = null;
				boolean book1IsThere = false;
				boolean book2IsThere = false;
				boolean book3IsThere = false;
				for (int i = 0; i < repeats; i++)
				{

					book1IsThere = false;
					book2IsThere = false;
					book3IsThere = false;
					books = storeManager.getBooks();
					for (StockBook b : books)
					{
						if (b.getISBN() == testISBN)
						{
							book1IsThere = b.getNumCopies() == 1 ? true : false;
						}
						if (b.getISBN() == testISBN + 1)
						{
							book2IsThere = b.getNumCopies() == 1 ? true : false;
						}
						if (b.getISBN() == testISBN + 2)
						{
							book3IsThere = b.getNumCopies() == 1 ? true : false;
						}
					}
					
					boolean allAreThere = book1IsThere && book2IsThere && book3IsThere;
					boolean noneAreThere = !book1IsThere && !book2IsThere && !book3IsThere;
					
					assertTrue(allAreThere != noneAreThere);
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
				fail();
			}
		}
		
	}
}



