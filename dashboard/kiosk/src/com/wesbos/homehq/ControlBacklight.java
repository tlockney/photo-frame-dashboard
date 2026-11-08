package com.wesbos.homehq;

/**
 * Pure helpers for turning the LCD backlight fully off on rooted frames. Android clamps the window
 * brightness override to its minimum (30/255 on the R1), so "screen off" needs a sysfs write as well.
 */
public final class ControlBacklight {
  private ControlBacklight() {}

  /** Shell script (run as `sh -c`, as root) that zeroes every backlight device. */
  public static final String OFF_SCRIPT =
      "for f in /sys/class/backlight/*/brightness; do echo 0 > \"$f\"; done";

  /**
   * A window brightness value that differs from {@code target}. Android does not re-apply its own
   * backlight value when the requested level looks unchanged, so on screen-on we apply this first and
   * then the real target. {@code target} is a 0..1 fraction or a negative "no override" value.
   */
  public static float nudge(float target) {
    if (target < 0f) return 0.5f;
    return target < 0.5f ? target + 0.2f : target - 0.2f;
  }
}
