import sys
import threading
import time
from PIL import Image
import pystray
from pystray import MenuItem as item

from server.server import start_server, set_expected_token, command_queue
from input.controller import execute_command
from pairing.qr_manager import show_qr

connection_status = "Waiting"
icon = None


def log(msg):
    print(f"[TRAY] {msg}", flush=True)


def update_status(new_status):
    global connection_status
    connection_status = new_status
    log(f"Status updated -> {new_status}")
    refresh_menu()


def show_qr_action(icon, item):
    log("Show QR Code clicked")
    token = show_qr()
    set_expected_token(token)
    log(f"New token set -> {token}")


def refresh_menu():
    log("Refreshing tray menu")
    icon.menu = pystray.Menu(
        item(f"Status: {connection_status}", None, enabled=False),
        item("Show QR Code", show_qr_action),
        item("Exit", on_exit)
    )
    icon.update_menu()


def on_exit(icon_obj, item):
    log("Exit clicked, shutting down")
    icon_obj.stop()
    sys.exit(0)


# 🔥 MAIN INPUT LOOP (pyautogui runs here)
def input_loop():
    log("Input loop started")
    while True:
        try:
            cmd = command_queue.get()
            log(f"Command received from queue -> {cmd}")
            execute_command(cmd)
            log("Command executed successfully")
        except Exception as e:
            log(f"Input loop error -> {e}")
        time.sleep(0.001)


def start_tray():
    global icon
    log("Starting tray app")

    image = Image.open("assets/icon.png")

    icon = pystray.Icon(
        "PC Remote Input",
        image,
        "PC Remote Input",
        menu=pystray.Menu(
            item("Status: Waiting", None, enabled=False),
            item("Show QR Code", show_qr_action),
            item("Exit", on_exit)
        )
    )

    log("Starting server thread")
    server_thread = threading.Thread(
        target=start_server,
        args=(update_status,),
        daemon=True
    )
    server_thread.start()

    log("Starting input execution thread")
    input_thread = threading.Thread(
        target=input_loop,
        daemon=True
    )
    input_thread.start()

    log("Tray icon running")
    icon.run()
