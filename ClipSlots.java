package com.acm.clip;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;

public class ClipSlots implements Serializable {

	private static final long serialVersionUID = 1L;
	protected int size;
	protected Clip[] clips = null;

	public ClipSlots(int size) {
		clips = new Clip[size];
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public void init() {
		for (int i = 0; i < size; i++) {
			Clip metaData = new Clip(-1, -1, true, false, false, false, false);
			setClip(i, metaData);
		}
	}

	public Clip getClip(int i) {
		return clips[i];
	}
	
	public void setClip(int i, Clip clip){
		this.clips[i] = clip;
	}

	public void deleteUnlockedSlots() {
		for (int i = 0; i < size; i++) {
			Clip clip = getClip(i);
			if (!clip.isLocked) {
				clip.deleteClip();
			}
		}
	}
	
	public void deleteAllSlots() {
		for (int i = 0; i < size; i++) {
			getClip(i).deleteClip();
		}
	}

	@SuppressWarnings("unchecked")
	public void performAwk() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		try {
			line = in.readLine();
			if (line.equals("")) {
				System.out.println("Please enter awk pattern: ");
				line = in.readLine();
			}
			in.close();
		} catch (Exception e) {
			System.out.println("Trouble Reading Awk Pattern");
			return;
			// this try catch should return as we dont need to process
			// anything in case of error
		}
		final int UNPROCESSED = 0;
		final int PROCESSED = 1;
		final int PROCESSING_SLOT = 2;
		final int PROCESSING_QUOTES = 3;
		final int PROCESSING_REGEX = 4;
		final int PROCESSING_OUTPUT_METHOD = 5;
		/* Format of quote: " something " */
		class Quote {
			String quote = "";
		}
		/* Format of slot: $<slot> */
		class Slot {
			String slot = "";
		}
		class OutputSlot extends Slot {
			boolean saveToClipboard;
		}
		class Regex {
			String regex = "";
		}
		int currentState = UNPROCESSED;
		ArrayList items = new ArrayList();
		Quote currentQuote = new Quote();
		Slot currentSlot = new Slot();
		Regex currentRegex = new Regex();
		boolean error = false, escaped = false, setEscaped = false;
		for (int i = 0; i < line.length() && !error; i++) {
			char currentChar = line.charAt(i);
			if (escaped) {
				switch (currentChar) {
				case 'b':
					currentChar = '\b';
					break;
				case 't':
					currentChar = '\t';
					break;
				case 'n':
					currentChar = '\n';
					break;
				case 'f':
					currentChar = '\f';
					break;
				case 'r':
					currentChar = '\r';
					break;
				case '\\':
					currentChar = '\\';
					break;
				case '\'':
					currentChar = '\'';
					break;
				case '\"':
					currentChar = '\"';
					break;
				default:
					System.out.println("Error: An invalid escape character '\\" + currentChar + "' was found at position " + (i + 1) + ".");
					error = true;
					break;
				}
			}
	
			if (currentState == UNPROCESSED) {
				if (currentChar == '"') {
					currentState = PROCESSING_QUOTES;
				} else if (currentChar == '$') {
					currentState = PROCESSING_SLOT;
				} else if (currentChar == '{') {
					System.out.println("Error: Encountered regex expression without accompanying slot at position " + (i + 1) + ".");
					error = true;
					break;
				} else if (currentChar == '>') {
					System.out.println("Error: Found redirection symbol '>' when no input was provided at position " + (i + 1) + ".");
					error = true;
					break;
				} else if (Character.isWhitespace(currentChar)) {
					// still unprocessed
				} else {
					System.out.println("Error: An invalid character '" + currentChar + "' was found at position " + (i + 1) + ".");
					error = true;
					break;
				}
			} else if (currentState == PROCESSED) {
				if (currentChar == '"') {
					currentState = PROCESSING_QUOTES;
				} else if (currentChar == '$') {
					currentState = PROCESSING_SLOT;
				} else if (currentChar == '{') {
					System.out.println("Error: Encountered regex expression without accompanying slot at position " + (i + 1) + ".");
					error = true;
					break;
				} else if (currentChar == '>') {
					currentState = PROCESSING_OUTPUT_METHOD;
					currentSlot = new OutputSlot();
				} else if (Character.isWhitespace(currentChar)) {
					// still in processed state
				} else {
					System.out.println("Error: An invalid character '" + currentChar + "' was found at position " + (i + 1) + ".");
					error = true;
					break;
				}
			} else if (currentState == PROCESSING_SLOT) {
				if (currentChar == '"') {
					if (currentSlot.slot.length() == 0) {
						System.out.println("Error: Found a slot symbol at position " + i + " without an accompanying slot number.");
						error = true;
						break;
					}
					int slot = Integer.parseInt(currentSlot.slot);
					if (slot < 0 || slot >= getSize()) {
						System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
						error = true;
						break;
					}
					if (!getClip(slot).isString) {
						System.out.println("Error: The slot number " + currentSlot.slot + " is currently isEmpty.");
						error = true;
						break;
					}
					items.add(currentSlot);
					currentSlot = new Slot();
					currentState = PROCESSING_QUOTES;
				} else if (currentChar == '{') {
					if (currentSlot.slot.length() == 0) {
						System.out.println("Error: Found a slot symbol at position " + i + " without an accompanying slot number.");
						error = true;
						break;
					}
					int slot = Integer.parseInt(currentSlot.slot);
					if (slot < 0 || slot >= getSize()) {
						System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
						error = true;
						break;
					}
					if (!getClip(slot).isString) {
						System.out.println("Error: The slot number " + currentSlot.slot + " is currently isEmpty.");
						error = true;
						break;
					}
					items.add(currentSlot);
					currentSlot = new Slot();
					currentState = PROCESSING_REGEX;
				} else if (currentChar == '>') {
					if (currentSlot.slot.length() == 0) {
						System.out.println("Error: Found a slot symbol at position " + i + " without an accompanying slot number.");
						error = true;
						break;
					}
					int slot = Integer.parseInt(currentSlot.slot);
					if (slot < 0 || slot >= getSize()) {
						System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
						error = true;
						break;
					}
					if (!getClip(slot).isString) {
						System.out.println("Error: The slot number " + currentSlot.slot + " is currently isEmpty.");
						error = true;
						break;
					}
					items.add(currentSlot);
					currentSlot = new OutputSlot();
					currentState = PROCESSING_OUTPUT_METHOD;
				} else if (Character.isWhitespace(currentChar)) {
					if (currentSlot.slot.length() == 0) {
						System.out.println("Error: Found a slot symbol at position " + i + " without an accompanying slot number.");
						error = true;
						break;
					}
					int slot = Integer.parseInt(currentSlot.slot);
					if (slot < 0 || slot >= getSize()) {
						System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
						error = true;
						break;
					}
					if (!getClip(slot).isString) {
						System.out.println("Error: The slot number " + currentSlot.slot + " is currently isEmpty.");
						error = true;
						break;
					}
					items.add(currentSlot);
					currentSlot = new Slot();
					currentState = PROCESSED;
				} else if (Character.isDigit(currentChar)) {
					currentSlot.slot += currentChar;
				} else if (currentChar == '$') {
					if (currentSlot.slot.length() == 0) {
						System.out.println("Error: Found a slot symbol at position " + i + " without an accompanying slot number.");
						error = true;
						break;
					}
					int slot = Integer.parseInt(currentSlot.slot);
					if (slot < 0 || slot >= getSize()) {
						System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
						error = true;
						break;
					}
					if (!getClip(slot).isString) {
						System.out.println("Error: The slot number " + currentSlot.slot + " is currently isEmpty.");
						error = true;
						break;
					}
					items.add(currentSlot);
					currentSlot = new Slot();
					currentState = PROCESSING_SLOT;
				} else {
					System.out.println("Error: An invalid character '" + currentChar + "' was found at position " + (i + 1) + ".");
					error = true;
					break;
				}
			} else if (currentState == PROCESSING_QUOTES) {
				if (currentChar == '"') {
					if (escaped) {
						currentQuote.quote += "\"";
					} else {
						items.add(currentQuote);
						currentQuote = new Quote();
						currentState = PROCESSED;
					}
				} else if (currentChar == '\\') {
					if (escaped) {
						currentQuote.quote += "\\";
					} else {
						setEscaped = true;
					}
				} else {
					currentQuote.quote += currentChar;
				}
			} else if (currentState == PROCESSING_REGEX) {
				if (currentChar == '}') {
					if (escaped) {
						currentRegex.regex += "}";
					} else {
						items.add(currentRegex);
						currentRegex = new Regex();
						currentState = PROCESSED;
					}
				} else if (currentChar == '\\') {
					if (escaped) {
						currentRegex.regex += "\\";
					} else {
						setEscaped = true;
					}
				} else {
					currentRegex.regex += currentChar;
				}
			} else if (currentState == PROCESSING_OUTPUT_METHOD) {
				if (Character.isWhitespace(currentChar)) {
					// still in same state
					if (currentSlot.slot.length() > 0) {
						items.add(currentSlot);
						currentSlot = new OutputSlot();
					}
				} else if (Character.isDigit(currentChar)) {
					currentSlot.slot += currentChar;
				} else if (Character.toLowerCase(currentChar) == 'c') {
					((OutputSlot) currentSlot).saveToClipboard = true;
				} else {
					System.out.println("Error: An invalid character '" + currentChar + "' was found at position " + (i + 1) + ". Expected any of: clipboard slot number or 'c'.");
					error = true;
					break;
				}
			} else {
				System.out.println("Unknown state encountered! currentState = " + currentState);
				error = true;
				break;
			}
	
			if (escaped) {
				escaped = false;
			}
			if (setEscaped) {
				escaped = true;
				setEscaped = false;
			}
		}
	
		if (currentState == PROCESSING_OUTPUT_METHOD) {
			if (currentSlot.slot.length() > 0) {
				int i = Integer.parseInt(currentSlot.slot);
				if (i < 0 || i >= getSize()) {
					System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
					error = true;
				} else {
					items.add(currentSlot);
				}
			} else if (((OutputSlot) currentSlot).saveToClipboard) {
				items.add(currentSlot);
			}
		} else if (currentState == UNPROCESSED) {
			System.out.println("Error: Nothing was entered!");
			error = true;
		} else if (currentState == PROCESSING_SLOT) {
			if (currentSlot.slot.length() == 0) {
				System.out.println("Error: Reached end of line before finding any slot number.");
				error = true;
			} else {
				int i = Integer.parseInt(currentSlot.slot);
				if (i < 0 || i >= getSize()) {
					System.out.println("Error: The slot number \'" + currentSlot.slot + "\' is not between the valid range of 0-" + (getSize() - 1) + ".");
					error = true;
				} else {
					items.add(currentSlot);
				}
			}
		} else if (currentState == PROCESSING_QUOTES) {
			System.out.println("Error: Reached end of line before terminating quote expression.");
			error = true;
		} else if (currentState == PROCESSING_REGEX) {
			System.out.println("Error: Reached end of line before terminating regex expression.");
			error = true;
		}
	
		if (!error) {
			/* Process the tags here. */
			String result = "";
			for (int i = 0; i < items.size(); i++) {
				Object o = items.get(i);
				if (o instanceof OutputSlot) {
					OutputSlot slot = (OutputSlot) o;
					if (slot.saveToClipboard) {
						ClipBoardOperator.clipOperation.setIgnoreThisString(result);
						ClipBoardOperator.clipOperation.setClipboardWithString(result);
					}
					if (slot.slot.length() > 0) {
						int slotNo = Integer.parseInt(slot.slot);
						getClip(slotNo).setNewString(result, null);
						ClipBoardOperator.clipOperation.fireClipItemProcesssed(slotNo);
					}
				} else if (o instanceof Slot) {
					Slot slot = (Slot) o;
					result += getClip(Integer.parseInt(slot.slot)).stringObject;
				} else if (o instanceof Quote) {
					Quote quote = (Quote) o;
					result += quote.quote;
				} else if (o instanceof Regex) {
					// Regex regex1 = (Regex) o;
					// we don't do anything with regex yet
				}
			}
			System.out.println("Result of awk expression: " + result);
		}
	}

	public void printSlots() {
		for (int i = 0; i < size; i++) {
			Clip clip = getClip(i);
			if (clip.isString) {
				System.out.println(i + ": " + clip.stringObject);
			}
		}
	}

	public String joinSlotsWithNewLine() {
		String join = "";
		for (int i = 0; i < size; i++) {
			Clip clip = getClip(i);
			if (clip.isString) {
				join = join + clip.stringObject + System.getProperty("line.separator");
			}
		}
		return join;
	}

	public String joinSlotsWithSpace() {
		String join = "";
		for (int i = 0; i < size; i++) {
			Clip clip = getClip(i);
			if (clip.isString) {
				join = join + " " + clip.stringObject;
			}
		}
		return join;
	}

	public String joinSlots() {
		String join = "";
		for (int i = 0; i < size; i++) {
			Clip clip = getClip(i);
			if (clip.isString) {
				join = join + clip.stringObject;
			}
		}
		return join;
	}

}