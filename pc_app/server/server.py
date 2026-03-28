import socket
import queue
from input.controller import execute_command

EXPECTED_TOKEN = None
command_queue = queue.Queue()


def set_expected_token(token):
    global EXPECTED_TOKEN
    EXPECTED_TOKEN = token


def start_server(status_callback):
    HOST = "0.0.0.0"
    PORT = 5000

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen(1)

    while True:
        status_callback("Waiting")
        conn, addr = server.accept()

        try:
            token = conn.recv(1024).decode().strip()
            if token != EXPECTED_TOKEN:
                conn.close()
                continue

            status_callback("Connected")

            while True:
                data = conn.recv(1024).decode().strip()
                if not data:
                    break

                if data == "PING":
                    continue  # keep connection alive

                command_queue.put(data)


        except Exception as e:
            print("Server error:", e)

        conn.close()
        status_callback("Waiting")
