package com.acertainbookstore.business;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The implementation of all parts of the book. Only parts of it are available
 * in the BookStoreClient and StockManager, cf. the Book interface and the
 * StockBook interface.
 * 
 */
public class BookStoreBook extends ImmutableBook {
	private int numCopies;
	private long totalRating;
	private long timesRated;
	private long saleMisses;
	private boolean editorPick;
	
	//define read and write locks
	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock r = rwl.readLock();
	private final Lock w = rwl.writeLock();

	/**
	 * Constructor to create a book object
	 */
	public BookStoreBook(int ISBN, String title, String author, float price,
			int numCopies) {
		super(ISBN, title, author, price);
		this.setSaleMisses(0);
		this.setTimesRated(0);
		this.setNumCopies(numCopies);
		this.setTotalRating(0);
		this.setEditorPick(false);
	}

	/**
	 * Constructor to create a book store book object from a stock book object
	 * 
	 * @param bookToCopy
	 */
	public BookStoreBook(StockBook bookToCopy) {
		super(bookToCopy.getISBN(), bookToCopy.getTitle(), bookToCopy
				.getAuthor(), bookToCopy.getPrice());
		this.setSaleMisses(bookToCopy.getSaleMisses());
		this.setTimesRated(bookToCopy.getTimesRated());
		this.setNumCopies(bookToCopy.getNumCopies());
		this.setTotalRating(bookToCopy.getTotalRating());
		this.setEditorPick(bookToCopy.isEditorPick());
	}

	//There has not been put locks on the functions regarding rating
	//since these functionalities has not been implemented in this solution
	public long getTotalRating() {
		return totalRating;
	}

	public long getTimesRated() {
		return timesRated;
	}

	public int getNumCopies() {
		r.lock();
		try { return numCopies; }
		finally { r.unlock(); }
	}

	public long getSaleMisses() {
		r.lock();
		try { return saleMisses; }
		finally { r.unlock(); }
	}
	public float getAverageRating() {
		return (float) (timesRated == 0 ? -1.0 : totalRating / timesRated);
	}

	public boolean isEditorPick() {
		r.lock();
		try { return editorPick; }
		finally { r.unlock(); }
	}

	
	/**
	 * Sets the total rating of the book.
	 * 
	 * @param totalRating
	 */
	private void setTotalRating(long totalRating) {
		this.totalRating = totalRating;
	}

	/**
	 * Sets the number of times that a book was rated.
	 * 
	 * @param timesRated
	 */
	private void setTimesRated(long timesRated) {
		this.timesRated = timesRated;
	}

	/**
	 * Sets the number of copies of a book, that is in stock.
	 * 
	 * @param numCopies
	 */
	private void setNumCopies(int numCopies) {
		w.lock();
		try { this.numCopies = numCopies; }
		finally { w.unlock(); }
	}

	/**
	 * Sets the number of times that a client wanted to buy a book when it was
	 * not in stock.
	 * 
	 * @param saleMisses
	 */
	private void setSaleMisses(long saleMisses) {
		w.lock();
		try { this.saleMisses = saleMisses; }
		finally { w.unlock(); }
	}

	/**
	 * Sets the book to be an editor pick if the boolean is true, otherwise the
	 * book is not an editor pick.
	 * 
	 * @param editorPick
	 */
	public void setEditorPick(boolean editorPick) {
		w.lock();
		try { this.editorPick = editorPick; }
		finally { w.unlock(); }
	}

	/**
	 * Checks if numCopies of the book are available
	 * @param numCopies
	 * @return
	 */
	public boolean areCopiesInStore(int numCopies) {
		r.lock();
		try { return (this.numCopies>=numCopies); }
		finally { r.unlock(); }
	}
	
	/**
	 * Reduces the number of copies of the books
	 * @param numCopies
	 * @return
	 */
	public boolean buyCopies(int numCopies) {
		w.lock();
		try {
			if(areCopiesInStore(numCopies)) {
				this.numCopies-=numCopies;
				return true;
			}
			return false;
		}
		finally { w.unlock(); }
	}
	
	/**
	 * Adds newCopies to the total number of copies of the book.
	 */
	public void addCopies(int newCopies) {
		w.lock();
		try {
			this.numCopies += newCopies;
			this.saleMisses = 0;
		}
		finally { w.unlock(); }
	}


	/**
	 * Increases the amount of missed sales of the book.
	 */
	public void addSaleMiss() {
		w.lock();
		try { this.saleMisses++; }
		finally { w.unlock(); }
	}

	/**
	 * Adds the rating to the total rating of the book.
	 * 
	 * @param rating
	 */
	
	//No locks on addRating since it has not been implemented in this solution
	public void addRating(int rating) {
		this.totalRating += rating;
		this.timesRated++;
	}

	/**
	 * Returns True if someone tried to buy the book, while the book was not in
	 * stock.
	 * 
	 * @return
	 */
	public boolean hadSaleMiss() {
		r.lock();
		try { return this.saleMisses > 0; }
		finally { r.unlock(); }
	}

	/**
	 * Returns a string representation of the book.
	 */
	public String toString() {
		String bookString = "ISBN = " + this.getISBN() + ", Title = "
				+ this.getTitle() + ", Author = " + this.getAuthor()
				+ ", Price = " + this.getPrice();
		return bookString;
	}

	/**
	 * Returns a ImmutableBook copy of the book.
	 * 
	 * @return
	 */
	public ImmutableBook immutableBook() {
		return new ImmutableBook(this.getISBN(), new String(this.getTitle()),
				new String(this.getAuthor()), this.getPrice());
	}

	/**
	 * Returns a ImmutableStockBook copy of the book.
	 * 
	 * @return
	 */
	public StockBook immutableStockBook() {
		return new ImmutableStockBook(this.getISBN(), new String(
				this.getTitle()), new String(this.getAuthor()),
				this.getPrice(), this.numCopies, this.saleMisses,
				this.timesRated, this.totalRating, this.editorPick);
	}

	/**
	 * Returns a copy of the book.
	 * 
	 * @return
	 */
	public BookStoreBook copy() {
		return new BookStoreBook(this.getISBN(), new String(this.getTitle()),
				new String(this.getAuthor()), this.getPrice(), this.numCopies);
	}

}
