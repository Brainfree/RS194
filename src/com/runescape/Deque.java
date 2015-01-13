package com.runescape;

/* Class24 - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */

public final class Deque {
	CacheLink head = new CacheLink();

	public Deque() {
		head.next = head;
		head.previous = head;
	}

	public void push(CacheLink l) {
		if (l.previous != null) {
			l.uncache();
		}
		l.previous = head.previous;
		l.next = head;
		l.previous.next = l;
		l.next.previous = l;
	}

	public CacheLink pull() {
		CacheLink l = head.next;
		if (l == head) {
			return null;
		}
		l.uncache();
		return l;
	}
}