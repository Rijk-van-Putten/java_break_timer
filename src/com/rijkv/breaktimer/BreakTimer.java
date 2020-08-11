package com.rijkv.breaktimer;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

import com.rijkv.breaktimer.filemanagement.FileManager;
import com.rijkv.breaktimer.input.KeyListener;
import com.rijkv.breaktimer.input.MouseListener;

enum TimerState {
	CountingDown, Break,
}

public class BreakTimer {

	private TimerState timerState;
	private HashMap<BreakInfo, Stopwatch> breaks = new HashMap<>();

	private MouseListener mouseListener;
	private KeyListener keyListener;

	private Stopwatch breakStopwatch = new Stopwatch();
	private Duration breakDuration;

	public BreakTimer() {
		// Load config
		var breaksList = FileManager.getBreakConfig();
		// Sort by interval
		Collections.sort(breaksList, (o1, o2) -> (int) o2.interval.toSeconds() - (int) o1.interval.toSeconds());

		for (BreakInfo breakInfo : breaksList) {
			System.out.println(breakInfo.interval.getSeconds());
			breaks.put(breakInfo, new Stopwatch());
		}

		timerState = TimerState.CountingDown;

		// Setup Keyboard & Mouse listeners
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException ex) {
			System.err.println("There was a problem registering the native hook.");
			System.err.println(ex.getMessage());

			System.exit(1);
		}

		// Mouse listener
		mouseListener = new MouseListener();
		GlobalScreen.addNativeMouseListener(mouseListener);
		GlobalScreen.addNativeMouseMotionListener(mouseListener);

		// Key listener
		keyListener = new KeyListener();
		GlobalScreen.addNativeKeyListener(keyListener);

		loop();
	}

	private void loop() {
		StartBreakStopwatches();

		final Timer time = new Timer();
		time.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				switch (timerState) {
					case CountingDown:
						// Check for breaks
						for (Map.Entry<BreakInfo, Stopwatch> entry : breaks.entrySet()) {
							BreakInfo info = entry.getKey();
							Stopwatch stopwatch = entry.getValue();
							if (stopwatch.elapsed() >= info.interval.toNanos()) {
								System.out.println(
										"ELAPSED: " + stopwatch.elapsed() + "  INTERVAL: " + info.interval.toNanos());
								System.out.println("BREAK!!! On " + info.name);
								timerState = TimerState.Break;

								// Stop all stopwatches smaller than this interval
								for (Map.Entry<BreakInfo, Stopwatch> entry2 : breaks.entrySet()) {
									BreakInfo info2 = entry2.getKey();
									Stopwatch stopwatch2 = entry2.getValue();
									if (info2.interval.toSeconds() <= info.interval.toSeconds()) {
										stopwatch2.stop();
									}
								}

								breakStopwatch.start();
								breakDuration = info.duration;
							}
						}
						break;
					case Break:
						// Check if the break is over
						if (breakStopwatch.elapsed() >= breakDuration.toNanos()) {
							timerState = TimerState.CountingDown;
							breakStopwatch.stop();
							StartBreakStopwatches();
							System.out.println("BREAK OVER!");
						}
						break;
				}

			}
		}, 0, 100); // Update every second
	}

	private void StartBreakStopwatches() {
		// Start the stopwatches
		for (Map.Entry<BreakInfo, Stopwatch> entry : breaks.entrySet()) {
			Stopwatch stopwatch = entry.getValue();
			if (!stopwatch.isRunning()) {
				stopwatch.start();
			}
		}
	}

}
