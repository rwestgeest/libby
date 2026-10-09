import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { startTerminal } from '../src/terminal.js';

function setup(previousRaw, flowing) {
  const input = new PassThrough();
  input.isRaw = previousRaw;
  input.setRawMode = raw => { input.isRaw = raw; };
  if (flowing) input.resume();
  else input.pause();
  const signals = new EventEmitter();
  const output = new EventEmitter();
  let screen = '';
  let stopped = 0;
  let quits = 0;
  output.write = text => { screen += text; };
  const terminal = startTerminal({
    input, output, signals,
    gravity: () => () => { stopped++; },
    onQuit: () => { quits++; },
  });
  return {
    input, output, signals, terminal,
    verify() {
      assert.equal(input.isRaw, previousRaw);
      assert.equal(input.isPaused(), !flowing);
      assert.equal(input.listenerCount('keypress'), 0);
      assert.equal(input.listenerCount('end'), 0);
      assert.equal(input.listenerCount('error'), 0);
      assert.equal(output.listenerCount('error'), 0);
      for (const event of ['SIGINT', 'SIGTERM', 'SIGHUP', 'uncaughtExceptionMonitor']) {
        assert.equal(signals.listenerCount(event), 0);
      }
      assert.ok(screen.endsWith('\x1b[?25h\n'));
      terminal.quit();
      assert.equal(stopped, 1);
      assert.equal(quits, 1);
    },
  };
}

for (const event of ['SIGINT', 'SIGTERM', 'SIGHUP', 'uncaughtExceptionMonitor', 'end', 'quit']) {
  for (const previousRaw of [false, true]) {
    for (const flowing of [false, true]) {
      test(`${event} restores raw=${previousRaw}, flowing=${flowing}`, () => {
        const s = setup(previousRaw, flowing);
        try {
          if (event === 'end') s.input.emit('end');
          else if (event === 'quit') s.terminal.quit();
          else s.signals.emit(event, new Error('external failure'));
          s.verify();
        } finally { s.terminal.quit(); }
      });
    }
  }
}

for (const stream of ['input', 'output']) {
  test(`${stream} error restores the terminal and propagates the error`, () => {
    const s = setup(false, true);
    const error = new Error('stream failure');
    assert.throws(() => s[stream].emit('error', error), thrown => thrown === error);
    s.verify();
  });
}

for (const previousRaw of [false, true]) {
  test(`uncaught exception survives failed raw restoration (raw=${previousRaw})`, () => {
    const child = spawnSync(process.execPath, ['--input-type=module', '--eval', `
      import { PassThrough } from 'node:stream';
      import { startTerminal } from ${JSON.stringify(new URL('../src/terminal.js', import.meta.url).href)};
      const input = new PassThrough();
      input.isRaw = ${previousRaw};
      input.setRawMode = raw => { input.isRaw = raw; };
      startTerminal({
        input,
        output: process.stdout,
        gravity: () => () => process.stdout.write('gravity stopped\\n'),
        onQuit: () => process.stdout.write('shutdown complete\\n'),
      });
      input.setRawMode = () => { throw new Error('cleanup failure'); };
      setImmediate(() => { throw new Error('original uncaught failure'); });
    `], { encoding: 'utf8', timeout: 5000 });

    assert.ifError(child.error);
    assert.equal(child.signal, null);
    assert.equal(child.status, 1, child.stderr);
    assert.match(child.stderr, /Error: original uncaught failure/);
    assert.doesNotMatch(child.stderr, /Error: cleanup failure/);
    assert.ok(child.stdout.includes('gravity stopped\n'));
    assert.ok(child.stdout.includes('\x1b[?25h\n'));
    assert.ok(child.stdout.includes('shutdown complete\n'));
  });
}

test('a failed raw-mode restoration does not prevent cursor or timer cleanup', () => {
  const s = setup(false, false);
  const error = new Error('raw restoration failure');
  s.input.setRawMode = () => { throw error; };
  assert.throws(() => s.terminal.quit(), thrown => thrown === error);
  // The failed setting cannot be restored, but all other cleanup still runs.
  s.input.isRaw = false;
  s.verify();
});
