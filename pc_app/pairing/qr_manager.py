import socket
import random
import string
import threading
import tkinter as tk
import qrcode
from PIL import ImageTk

PORT = 5000

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except:
        ip = "127.0.0.1"
    s.close()
    return ip

def generate_token(length=6):
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=length))

def _qr_window(qr_data):
    root = tk.Tk()
    root.title("Scan to Connect")
    root.resizable(False, False)

    img = ImageTk.PhotoImage(qrcode.make(qr_data))
    label = tk.Label(root, image=img)
    label.image = img
    label.pack(padx=20, pady=20)

    text = tk.Label(root, text=qr_data)
    text.pack(pady=(0, 10))

    tk.Button(root, text="Close", command=root.destroy).pack(pady=(0, 10))
    root.mainloop()

def show_qr():
    ip = get_local_ip()
    token = generate_token()
    qr_data = f"{ip}:{PORT}:{token}"

    threading.Thread(
        target=_qr_window,
        args=(qr_data,),
        daemon=True
    ).start()

    return token
