package com.wesbos.homehq;

public final class ControlBackoff {
  private final long first, max;
  private long next;

  public ControlBackoff(long first, long max) {
    this.first = first;
    this.max = max;
    this.next = first;
  }

  public long next() {
    long d = next;
    next = Math.min(next * 2, max);
    return d;
  }

  public void reset() {
    next = first;
  }
}
