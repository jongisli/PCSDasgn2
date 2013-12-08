package com.acertainbookstore.utils;

/**
 * Exception to signal a book store error
 */
public class BookStoreException extends Exception {
	private static final long serialVersionUID = 1L;
	private int numberOfBooks;

	public BookStoreException() {
		super();
	}

	public BookStoreException(String message) {
		super(message);
	}

	public BookStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public BookStoreException(Throwable ex) {
		super(ex);
	}
	
	public BookStoreException(String message, int numberOfBooks) {
		super(message);
		this.numberOfBooks = numberOfBooks;
	}
	
	// Added this getter to be able to pass along the message 
	// how many books were available for return in for example
	// getEditorPicks when the amount of books asked for was greater
	public int getNumberOfBooks()
	{
		return numberOfBooks;
	}
}
