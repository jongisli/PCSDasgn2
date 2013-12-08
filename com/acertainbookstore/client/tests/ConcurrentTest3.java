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

public class ConcurrentTest3 {

	//private static boolean concurrentlocalTest = true; 
	
	private static StockManager storeManagerA;
	private static StockManager storeManagerB;
	private static StockManager storeManagerC;
	private static BookStore clientA;
	private static BookStore clientB;
	private static BookStore clientC;
	private static BookStore clientD;
	private static BookStore clientE;
	private static BookStore clientF;
	
	
	@BeforeClass
	public static void setUpBeforeClass() {
	
		storeManagerA = ConcurrentCertainBookStore.getInstance();
		storeManagerB = ConcurrentCertainBookStore.getInstance();
		storeManagerC = ConcurrentCertainBookStore.getInstance();
		clientA = ConcurrentCertainBookStore.getInstance();
		clientB = ConcurrentCertainBookStore.getInstance();
		clientC = ConcurrentCertainBookStore.getInstance();
		clientD = ConcurrentCertainBookStore.getInstance();
		clientE = ConcurrentCertainBookStore.getInstance();
		clientF = ConcurrentCertainBookStore.getInstance();
	}
	
	/**
	 * Here we add one more test to test concurrency
	 * 
	 * 1. We add three books with ISBN 100, 200 and 300 with copies number 100, 20 and 2
	 * 
	 * 2. We start a client (thread1) which buys five copies from ISBN 200
	 * 
	 * 3. We then start a client (thread2) which buys four copies from ISBN 200
	 * 
	 * 4. We then start a client (thread3) which buys two copies from ISBN 200
	 * 
	 * 5. We then start a client (thread4) which buys forty six copies from ISBN 100
	 * 
	 * 6. We then start a client (thread5) which buys two copies from ISBN 300
	 * 
	 * 7. We then start a client (thread6) which buys five copies from ISBN 300
	 * 
	 * 8. We start a client (thread 7) which adds ten copies to ISBN 300
	 * 
	 * 9. We start a client (thread 8) which adds ten copies to ISBN 100
	 * 
	 * 10. Finally we start a client (thread 9) which adds ten copies to ISBN 
	 *     100 and one copy to ISBN 300
	 *
	 *
	 */
	
	@Test
	public void TestAtomicity() {
		
		// add books with ISBN 100 50 copies.
		int testISBN = 100;
		int amountOfBooks = 10000;
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(testISBN,
				"Egils saga Skalla-Gr’mssonar",
				"Viking Vikingsson", (float) 100, amountOfBooks, 0, 0, 0,
				false));
		try {
			storeManagerA.addBooks(booksToAdd);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		
		// add books with ISBN 200 20 copies 
		int testISBN2 = 200;
		int amountOfBooks2 = 2000;
		Set<StockBook> booksToAdd2 = new HashSet<StockBook>();
		booksToAdd2.add(new ImmutableStockBook(testISBN2,
				"Principles of computer system and design",
				"Vivek", (float) 200, amountOfBooks2, 0, 0, 0,
				false));
		try {
			storeManagerA.addBooks(booksToAdd2);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		// add books with ISBN 300 only 3 copies 
		int testISBN3 = 300;
		int amountOfBooks3 = 200;
		Set<StockBook> booksToAdd3 = new HashSet<StockBook>();
		booksToAdd3.add(new ImmutableStockBook(testISBN3,
				"Computational and Mathematicl Modeling",
				"Christian Igel", (float) 300, amountOfBooks3, 0, 0, 0,
				false));
		try {
			storeManagerA.addBooks(booksToAdd3);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}

		
		
		
		int repeats = 100;
		Thread client1 = new Thread(new buyBooksClientA(testISBN2, repeats));
		Thread client2 = new Thread(new buyBooksClientB(testISBN2, repeats));
		Thread client3 = new Thread(new buyBooksClientC(testISBN2, repeats));
		Thread client4 = new Thread(new buyBooksClientD(testISBN, repeats));
		Thread client5 = new Thread(new buyBooksClientE(testISBN3, repeats));
		Thread client6 = new Thread(new buyBooksClientF(testISBN3, repeats));
		Thread client7 = new Thread(new addCopiesClientA(testISBN3, repeats));
		Thread client8 = new Thread(new addCopiesClientB(testISBN, repeats));
		Thread client9 = new Thread(new addCopiesClientC(testISBN, repeats));
		
		client7.start();
		client1.start();
		client2.start();
		client3.start();
		client4.start();
		client5.start();
		client6.start();

		client8.start();
		client9.start();
	
		try {
			client1.join();
			client2.join();
			client3.join();
			client4.join();
			client5.join();
			client6.join();
			client7.join();
			client8.join();
			client9.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			fail();
		}
		
		
		List<StockBook> listBooks = null;
		try {
			listBooks = storeManagerA.getBooks();
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		for (StockBook b : listBooks) {
			if (b.getISBN() == testISBN) {
				System.out.println(b.getNumCopies());
				assertTrue("Number of copies",
						b.getNumCopies() == 7400);
				break;
			}
		}
		
		
		List<StockBook> listBooks2 = null;
		try {
			listBooks2 = storeManagerB.getBooks();
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		for (StockBook b : listBooks2) {
			if (b.getISBN() == testISBN2) {
				System.out.println(b.getNumCopies());
				assertTrue("Number of copies",
						b.getNumCopies() == 900);
				break;
			}
		}
		
		
		List<StockBook> listBooks3 = null;
		try {
			listBooks3 = storeManagerC.getBooks();
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		for (StockBook b : listBooks3) {
			if (b.getISBN() == testISBN3) {
				System.out.println(b.getNumCopies());
				assertTrue("Number of copies",
						b.getNumCopies() == 600);
				break;
			}
		}
		
	}
	
	private class buyBooksClientA implements Runnable {


			private StockManager storeManager;
			private BookStore client;
			private Set<BookCopy> theSagas;
			private int repeats;
			
			public buyBooksClientA(int testISBN2, int repeats)
			{
				this.storeManager = ConcurrentCertainBookStore.getInstance();
				this.client = ConcurrentCertainBookStore.getInstance();
				this.repeats = repeats;
				
				this.theSagas = new HashSet<BookCopy>();
				theSagas.add(new BookCopy(testISBN2, 5));
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
	
	private class buyBooksClientB implements Runnable {


		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public buyBooksClientB(int testISBN2, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN2, 4));
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
	
	private class buyBooksClientC implements Runnable {


		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public buyBooksClientC(int testISBN2, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN2, 2));
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
	
	private class buyBooksClientD implements Runnable {


		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public buyBooksClientD(int testISBN, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN, 46));
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
	
	private class buyBooksClientE implements Runnable {


		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public buyBooksClientE(int testISBN3, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN3, 2));
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
	
	private class buyBooksClientF implements Runnable {


		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public buyBooksClientF(int testISBN3, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN3, 5));
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


	private class addCopiesClientA implements Runnable {
	
		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public addCopiesClientA(int testISBN3, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN3, 10));
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
	
	
	private class addCopiesClientB implements Runnable {
		
		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public addCopiesClientB(int testISBN, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN, 10));
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
	
	
	private class addCopiesClientC implements Runnable {
		
		private StockManager storeManager;
		private BookStore client;
		private Set<BookCopy> theSagas;
		private int repeats;
		
		public addCopiesClientC(int testISBN, int repeats)
		{
			this.storeManager = ConcurrentCertainBookStore.getInstance();
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			int isbn3 = 300;
			
			this.theSagas = new HashSet<BookCopy>();
			theSagas.add(new BookCopy(testISBN, 10));
			theSagas.add(new BookCopy(isbn3, 1));
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




