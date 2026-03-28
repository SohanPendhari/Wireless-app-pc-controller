import pyautogui
import threading
import time
from collections import deque

pyautogui.FAILSAFE = False

# queue of finger deltas
delta_queue = deque()
lock = threading.Lock()

# trackpad sensitivity (tune this)
SENSITIVITY = 1.2

# optional smoothing (0 = raw, 0.2 = smooth)
SMOOTHING = 0.15

last_dx = 0.0
last_dy = 0.0


def trackpad_loop():
    global last_dx, last_dy

    while True:
        dx = dy = 0.0

        with lock:
            if delta_queue:
                dx, dy = delta_queue.popleft()

        if dx != 0.0 or dy != 0.0:
            # light smoothing (like mac/windows trackpad)
            dx = last_dx * SMOOTHING + dx * (1 - SMOOTHING)
            dy = last_dy * SMOOTHING + dy * (1 - SMOOTHING)

            pyautogui.moveRel(
                dx * SENSITIVITY,
                dy * SENSITIVITY
            )

            last_dx = dx
            last_dy = dy

        time.sleep(0.004)  # ~250 Hz (trackpad rate)


# start once
threading.Thread(target=trackpad_loop, daemon=True).start()


def execute_command(command):
    try:
        parts = command.strip().split()
        if not parts:
            return

        cmd = parts[0]

        # MOVE dx dy = finger movement since last frame
        if cmd == "MOVE" and len(parts) == 3:
            dx = float(parts[1])
            dy = float(parts[2])

            with lock:
                delta_queue.append((dx, dy))

        elif cmd == "CLICK":
            pyautogui.click()

        elif cmd == "RIGHT_CLICK":
            pyautogui.rightClick()

        elif cmd == "SCROLL" and len(parts) == 2:
            pyautogui.scroll(int(float(parts[1])))

        elif cmd == "KEY" and len(parts) == 2:
            pyautogui.press(parts[1])

        elif cmd == "COMBO":
            pyautogui.hotkey(*parts[1:])

    except Exception as e:
        print("Input error:", e)
