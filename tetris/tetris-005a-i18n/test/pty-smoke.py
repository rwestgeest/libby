"""Optional POSIX smoke test: python3 test/pty-smoke.py (requires npm/node)."""
import os
import pty
import subprocess
import termios
import fcntl
import struct
import select
import time
import signal
import re


def run(command, sig=None):
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack('HHHH', 22, 60, 0, 0))
    before = termios.tcgetattr(slave)
    process = subprocess.Popen(command, stdin=slave, stdout=slave, stderr=slave,
                               start_new_session=True)
    data = bytearray()

    def drain(seconds=0.3):
        end = time.monotonic() + seconds
        while time.monotonic() < end:
            if select.select([master], [], [], 0.02)[0]:
                data.extend(os.read(master, 65536))

    try:
        drain(1)
        assert b'TETRIS' in data
        if sig:
            process.send_signal(sig)
        else:
            os.write(master, b'\x1b[Da\x1b[Cd\x1b[Awzs')
            drain()
            os.write(master, b'p')
            drain()
            assert b'PAUSED' in data
            os.write(master, b'p' + b' ' * 40)
            drain(1)
            assert b'GAME OVER' in data
            # Verify row addresses across the entire gameplay transcript.
            assert max(map(int, re.findall(rb'\x1b\[(\d+);1H', data))) <= 22
            data.clear()
            os.write(master, b'r')
            drain()
            assert b'Score: 0' in data and b'Playing' in data
            os.write(master, b'q')
        drain(1)
        process.wait(timeout=5)
        assert b'\x1b[?25h\x1b[?1049l' in data
        assert termios.tcgetattr(slave) == before
        expected = {signal.SIGINT: 130, signal.SIGTERM: 143, signal.SIGHUP: 129}
        assert process.returncode == (expected[sig] if sig else 0)
        print('PASS', command, sig or 'play/pause/game-over/restart/quit')
    finally:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        os.close(master)
        os.close(slave)


run(['npm', 'start'])
for shutdown_signal in [signal.SIGINT, signal.SIGTERM, signal.SIGHUP]:
    run(['node', 'src/cli.js'], shutdown_signal)
