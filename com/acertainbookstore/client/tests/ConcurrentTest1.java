package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

public class ConcurrentTest1 {

	//private static boolean concurrentlocalTest = true; 
	private static StockManager concurrentstoreManager;
	private static BookStore concurrentclientA;
	
	
	@BeforeClass
	public static void setUpBeforeClass() {
	
		concurrentstoreManager = CertainBookStore.getInstance();
		concurrentclientA = CertainBookStore.getInstance();
	}
	
	
	public void TestAtomicity() {
		
		Thread client1 = new Thread(new buyBooksRunnable());
		Thread client2 = new Thread(new addCopiesRunnable());
		
		client1.start();
		client2.start();
	}
	
	public class buyBooksRunnable implements Runnable {

		public void run() {

				Integer testISBN = 300;
				Integer numCpies = 5;
				int buyCopies = 2;
				
				Set<StockBook> booksToAdd = new HashSet<StockBook>();
				booksToAdd.add(new ImmutableStockBook(testISBN, "Book Name",
						"Author Name", (float) 100, numCpies, 0, 0, 0, false));
				try {
					concurrentstoreManager.addBooks(booksToAdd);
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
				
				Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
				List<StockBook> listBooks = null;
				booksToBuy.add(new BookCopy(testISBN, buyCopies));
				try {
					concurrentclientA.buyBooks(booksToBuy);
					listBooks = concurrentstoreManager.getBooks();
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
				
				for (StockBook b : listBooks) {
					if (b.getISBN() == testISBN) {
						assertTrue("Num copies  after buying one copy",
								b.getNumCopies() == numCpies);
						break;
					}
				}
				
		}
		
	}
	public class addCopiesRunnable implements Runnable {

		public void run() {
			
			Integer testISBN = 300;
			Integer totalNumCopies = 5;
			
			Set<StockBook> booksToAdd = new HashSet<StockBook>();
			booksToAdd.add(new ImmutableStockBook(testISBN, "Book Name",
					"Book Author", (float) 100, 5, 0, 0, 0, false));
			try {
				concurrentstoreManager.addBooks(booksToAdd);
			} catch (BookStoreException e) {
				e.printStackTrace();
				fail();
			}
			
			BookCopy bookCopy = new BookCopy(testISBN, 2);
			Set<BookCopy> bookCopyList = new HashSet<BookCopy>();
			bookCopyList.add(bookCopy);
			List<StockBook> listBooks = null;
			try {
				concurrentstoreManager.addCopies(bookCopyList);
				listBooks = concurrentstoreManager.getBooks();

				for (StockBook b : listBooks) {
					if (b.getISBN() == testISBN) {
						assertTrue("Number of copies!",
								b.getNumCopies() == totalNumCopies);
						break;
					}
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
				fail();
			}
				
		}
		
	}


}
