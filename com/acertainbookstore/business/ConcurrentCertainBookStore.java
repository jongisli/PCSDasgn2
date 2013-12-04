package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

public class ConcurrentCertainBookStore implements BookStore, StockManager{
	private static ConcurrentCertainBookStore singleInstance;
	private static Map<Integer, BookStoreBook> bookMap;
	
	//define read and write locks
	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock r = rwl.readLock();
	private final Lock w = rwl.writeLock();
	
	private ConcurrentCertainBookStore() {
		// TODO Auto-generated constructor stub
	}

	public synchronized static ConcurrentCertainBookStore getInstance() {
		if (singleInstance != null) {
			return singleInstance;
		} else {
			singleInstance = new ConcurrentCertainBookStore();
			bookMap = new HashMap<Integer, BookStoreBook>();
		}
		return singleInstance;
	}

	public void addBooks(Set<StockBook> bookSet)
			throws BookStoreException {

		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check if all are there
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			String bookTitle = book.getTitle();
			String bookAuthor = book.getAuthor();
			int noCopies = book.getNumCopies();
			float bookPrice = book.getPrice();
			if (BookStoreUtility.isInvalidISBN(ISBN)
					|| BookStoreUtility.isEmpty(bookTitle)
					|| BookStoreUtility.isEmpty(bookAuthor)
					|| BookStoreUtility.isInvalidNoCopies(noCopies)
					|| bookPrice < 0.0) {
				throw new BookStoreException(BookStoreConstants.BOOK
						+ book.toString() + BookStoreConstants.INVALID);
					
			} 
			//readLock on bookMap for reading bookMap
			r.lock();
			try {
				if (bookMap.containsKey(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.DUPLICATED);
				}
			}
			//unlock readLock
			finally { r.unlock(); }
		}

		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			//writeLock on bookMap for putting book
			w.lock();
			try { bookMap.put(ISBN, new BookStoreBook(book)); }
			//unlock writeLock
			finally { w.unlock(); }
		}
		return;
	}
	
	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		int ISBN, numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			//readLock on bookMap
			r.lock();
			try {
				if (!bookMap.containsKey(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.INVALID);
			}
			//unlock readLock
			finally { r.unlock(); }
			
			if (BookStoreUtility.isInvalidNoCopies(numCopies))
				throw new BookStoreException(BookStoreConstants.NUM_COPIES
						+ numCopies + BookStoreConstants.INVALID);

		}

		BookStoreBook book;
		// Update the number of copies
		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			r.lock();
			try { book = bookMap.get(ISBN); }
			finally { r.unlock(); }
			w.lock();
			try { book.addCopies(numCopies); }
			finally { w.unlock(); }

		}
	}

	public List<StockBook> getBooks() {
		List<StockBook> listBooks = new ArrayList<StockBook>();
		r.lock();
		try { 
			Collection<BookStoreBook> bookMapValues = bookMap.values(); 
			for (BookStoreBook book : bookMapValues) {
				listBooks.add(book.immutableStockBook());
			}
			
			return listBooks;
			}
		//I think I can unlock here because listBooks is a temp of the Collection
		finally {r. unlock(); }
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int ISBNVal;

		for (BookEditorPick editorPickArg : editorPicks) {
			ISBNVal = editorPickArg.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.INVALID);
			r.lock();
			try {
				if (!bookMap.containsKey(ISBNVal))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
							+ BookStoreConstants.NOT_AVAILABLE);
			}
			finally { r.unlock(); }
		}

		for (BookEditorPick editorPickArg : editorPicks) {
			w.lock();
			try {
				bookMap.get(editorPickArg.getISBN()).setEditorPick(
						editorPickArg.isEditorPick());
			}
			finally { w.unlock(); }
		}
		return;
	}

	public void buyBooks(Set<BookCopy> bookCopiesToBuy)
			throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		Boolean saleMiss = false;
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			ISBN = bookCopyToBuy.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			r.lock();
				try {
				if (!bookMap.containsKey(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.NOT_AVAILABLE);
				book = bookMap.get(ISBN);
			}
			finally { r.unlock(); }
			//Should there be a readLock on book.areCopiesInStore() ?
			if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
				w.lock();
				try { book.addSaleMiss(); } // If we cannot sell the copies of the book
											// its a miss
				finally { w.unlock(); }
				saleMiss = true;
			}
		}

		// We throw exception now since we want to see how many books in the
		// order incurred misses which is used by books in demand
		if (saleMiss)
			throw new BookStoreException(BookStoreConstants.BOOK
					+ BookStoreConstants.NOT_AVAILABLE);

		// Then make purchase
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			r.lock();
			try { book = bookMap.get(bookCopyToBuy.getISBN()); }
			finally { r.unlock(); }
			
			w.lock();
			try { book.buyCopies(bookCopyToBuy.getNumCopies()); }
			finally {w.unlock(); }
		}
		return;
	}

	public List<Book> getBooks(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we rate are there first.
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			r.lock();
			try {
				if (!bookMap.containsKey(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.NOT_AVAILABLE);
			}
			finally { r.unlock(); }
		}

		List<Book> listBooks = new ArrayList<Book>();

		// Get the books
		for (Integer ISBN : isbnSet) {
			r.lock();
			try { listBooks.add(bookMap.get(ISBN).immutableBook()); }
			finally { r.unlock(); }
		}
		return listBooks;
	}


	public List<Book> getEditorPicks(int numBooks)
			throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks
					+ ", but it must be positive");
		}

		List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
		List<Book> listEditorPicks = new ArrayList<Book>();
		
		//Moved book out of try clause
		BookStoreBook book;
		//read lock on it
		r.lock();
		try {
			Iterator<Entry<Integer, BookStoreBook>> it = bookMap.entrySet()
					.iterator();
			
			
	
			// Get all books that are editor picks
			while (it.hasNext()) {
				Entry<Integer, BookStoreBook> pair = (Entry<Integer, BookStoreBook>) it
						.next();
				book = (BookStoreBook) pair.getValue();
				if (book.isEditorPick()) {
					listAllEditorPicks.add(book);
				}
			}
		}
		//unlock readLock on it
		finally { r.unlock(); }

		// Find numBooks random indices of books that will be picked
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<Integer>();
		int rangePicks = listAllEditorPicks.size();
		if (rangePicks < numBooks) {
			throw new BookStoreException("Only " + rangePicks
					+ " editor picks are available.");
		}
		int randNum;
		while (tobePicked.size() < numBooks) {
			randNum = rand.nextInt(rangePicks);
			tobePicked.add(randNum);
		}

		// Get the numBooks random books
		for (Integer index : tobePicked) {
			book = listAllEditorPicks.get(index);
			listEditorPicks.add(book.immutableBook());
		}
		return listEditorPicks;

	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	

}
