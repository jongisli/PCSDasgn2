package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

public class ConcurrentTest4 {

	//private static boolean concurrentlocalTest = true; 
	private static StockManager storeManager;
	private static BookStore client;
	private static boolean testFailed;
	
	@BeforeClass
	public static void setUpBeforeClass() {
	
		storeManager = ConcurrentCertainBookStore.getInstance();
		client = ConcurrentCertainBookStore.getInstance();
		testFailed = false;
	}
	
	/**
	 * Here we test the functions updateEditorPicks() and getEditorPicks()
	 * for concurrency.
	 * 
	 * 1. Add five books to the bookstore
	 * 
	 * 2. Make Client1 set four of them as editor picks
	 * 
	 * 3. Make Client1 subsequently set the same four NOT as editor picks
	 * 
	 * 4. Make Client2 get editor picks and test that either all the four 
	 * books are there or none.
	 * 
	 */
	
	@Test
	public void TestAtomicity() {
		
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
		booksToAdd.add(new ImmutableStockBook(testISBN + 3,
				"Computational and Mathematicl Modeling",
				"Christian Igel", (float) 300, 1, 0, 0, 0,
				false));
		booksToAdd.add(new ImmutableStockBook(testISBN + 4,
				"Principles of computer systems design",
				"Vivek", (float) 200, 1, 0, 0, 0,
				false));
		try {
			storeManager.addBooks(booksToAdd);
		} catch (BookStoreException e) {
			e.printStackTrace();
			fail();
		}
		
		int repeats = 10;
		Thread client1 = new Thread(new SetEditorPicksClient(testISBN, repeats));
		Thread client2 = new Thread(new GetEditorPicksClient(testISBN, repeats));
		
		client1.start();
		client2.start();
		
		try {
			client1.join();
			client2.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			fail();
		}
		
		assertFalse(testFailed);
	}
	
	private class SetEditorPicksClient implements Runnable {
			private StockManager storeManager;
			private int repeats;
			private Set<BookEditorPick> editorPicksTrue;
			private Set<BookEditorPick> editorPicksFalse;
			
			public SetEditorPicksClient(int testISBN, int repeats)
			{
				this.storeManager = ConcurrentCertainBookStore.getInstance();
				this.repeats = repeats;
				
				this.editorPicksTrue = new HashSet<BookEditorPick>();
				editorPicksTrue.add(new BookEditorPick(testISBN, true));
				editorPicksTrue.add(new BookEditorPick(testISBN + 1, true));
				editorPicksTrue.add(new BookEditorPick(testISBN + 2, true));
				editorPicksTrue.add(new BookEditorPick(testISBN + 3, true));
				
				this.editorPicksFalse = new HashSet<BookEditorPick>();
				editorPicksFalse.add(new BookEditorPick(testISBN, false));
				editorPicksFalse.add(new BookEditorPick(testISBN + 1, false));
				editorPicksFalse.add(new BookEditorPick(testISBN + 2, false));
				editorPicksFalse.add(new BookEditorPick(testISBN + 3, false));
				
			}
			
			@Override
			public void run() {
				try {
					for (int i = 0; i < repeats; i++)
					{
						storeManager.updateEditorPicks(editorPicksTrue);
						storeManager.updateEditorPicks(editorPicksFalse);
					}
				} catch (BookStoreException e) {
					e.printStackTrace();
					testFailed = true;
					fail();
				}
			}
				
		}


	private class GetEditorPicksClient implements Runnable {
		private BookStore client;
		private int repeats;
		private int testISBN;
		
		public GetEditorPicksClient(int testISBN, int repeats)
		{
			this.client = ConcurrentCertainBookStore.getInstance();
			this.repeats = repeats;
			this.testISBN = testISBN;
		}
		
		@Override
		public void run() {
			for (int i = 0; i < repeats; i++)
			{
				List<Book> books = null;
				boolean book1IsThere = false;
				boolean book2IsThere = false;
				boolean book3IsThere = false;
				boolean book4IsThere = false;
				try {
					book1IsThere = false;
					book2IsThere = false;
					book3IsThere = false;
					book4IsThere = false;
					books = client.getEditorPicks(4);
					for (Book b : books)
					{
						if (b.getISBN() == testISBN)
						{
							book1IsThere = true;
						}
						if (b.getISBN() == testISBN + 1)
						{
							book2IsThere = true;
						}
						if (b.getISBN() == testISBN + 2)
						{
							book3IsThere = true;
						}
						if (b.getISBN() == testISBN + 3)
						{
							book4IsThere = true;
						}
					}
					boolean allAreThere = book1IsThere && book2IsThere && book3IsThere && book4IsThere;
					
					if (!allAreThere)
					{
						testFailed = true;
						break;
					}
					
				} catch (BookStoreException e) {
					// If a bookStoreException was thrown the number 
					// of editorPicks was less than 4 (should be 0 then)
					if (e.getNumberOfBooks() != 0)
					{
						testFailed = true;
						break;
					}
				}
			}
		}	
	}		
}
