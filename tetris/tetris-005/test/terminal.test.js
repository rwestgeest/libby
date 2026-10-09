import assert from 'node:assert/strict';
import { PassThrough } from 'node:stream';
import test from 'node:test';
import { Game } from '../src/game.js';
import { startGravity } from '../src/gravity.js';
import { CONTROLS, render, startTerminal } from '../src/terminal.js';

for (const state of ['playing', 'game-over']) {
  test(`complete ${state} display fits within 24 terminal rows`, () => {
    const game = new Game({ random: () => 0.2 });
    game.score = 1200;
    game.lines = 8;
    game.board[19][0] = 'T';
    if (state === 'game-over') {
      game.active = null;
      game.board[0].fill('I');
      assert.equal(game.spawn('O'), false);
      assert.equal(game.gameOver, true);
    }

    const display = render(game);
    const rows = display.split('\n');
    assert.ok(rows.length <= 24, `complete display uses ${rows.length} rows`);
    assert.equal(rows.length, 24);
    assert.equal(rows[0], '+--------------------+');
    assert.equal(rows[21], rows[0]);
    assert.equal(rows.slice(1, 21).length, 20);
    for (const row of rows.slice(1, 21)) {
      assert.match(row, /^\|(?:\[\]|  ){10}\|$/);
    }
    assert.ok(rows[20].startsWith('|[]'), 'settled board is displayed');
    assert.ok(rows[22].startsWith('Score: 1200  Lines: 8'));
    assert.equal(rows[23], CONTROLS);
    if (state === 'game-over') {
      assert.match(rows[22], /GAME OVER — R restart/);
    } else {
      assert.ok(rows.slice(1, 3).some(row => row.includes('[]')), 'active piece is displayed');
      assert.ok(!display.includes('GAME OVER'));
    }
  });
}

function setup() {
  const input = new PassThrough();
  input.setRawMode = raw => { input.isRaw = raw; };
  let screen = '';
  const timers = [];
  let quits = 0;
  const terminal = startTerminal({
    input,
    output: { write: text => { screen += text; } },
    createGame: () => new Game({ random: () => 0.2 }),
    gravity: (game, { onUpdate }) => {
      const timer = { game, onUpdate, stopped: false };
      timers.push(timer);
      return () => { timer.stopped = true; };
    },
    onQuit: () => { quits++; },
  });
  return {
    terminal, input, timers,
    key: name => input.emit('keypress', '', { name }),
    get screen() { return screen; },
    get quits() { return quits; },
  };
}

test('arrow keys move, rotate and soft drop; screen lists controls', () => {
  const s = setup();
  try {
    const x = s.terminal.game.active.x;
    s.key('left');
    assert.equal(s.terminal.game.active.x, x - 1);
    s.key('right');
    assert.equal(s.terminal.game.active.x, x);
    // Select a non-square piece to verify rotation through the controller.
    s.terminal.game.active = null;
    s.terminal.game.spawn('T');
    const before = s.terminal.game.active.matrix;
    s.key('up');
    assert.notDeepEqual(s.terminal.game.active.matrix, before);
    s.key('down');
    assert.equal(s.terminal.game.active.y, 1);
    assert.ok(s.screen.includes(CONTROLS));
  } finally { s.terminal.quit(); }
});

test('hard drop locks and spawns immediately; restart replaces game and timer', () => {
  const s = setup();
  try {
    s.key('space');
    assert.equal(s.terminal.game.board.flat().filter(Boolean).length, 4);
    assert.equal(s.terminal.game.active.y, 0);
    const oldGame = s.terminal.game;
    oldGame.score = 100;
    oldGame.gameOver = true;
    s.key('left');
    assert.equal(oldGame.active.x, 4);
    s.key('r');
    assert.notEqual(s.terminal.game, oldGame);
    assert.equal(s.terminal.game.score, 0);
    assert.equal(s.terminal.game.gameOver, false);
    assert.equal(s.terminal.game.board.flat().filter(Boolean).length, 0);
    assert.equal(s.timers[0].stopped, true);
    assert.equal(s.timers.length, 2);
    assert.equal(s.timers[1].game, s.terminal.game);
  } finally { s.terminal.quit(); }
});

test('Q and Ctrl-C quit, stop gravity and detach input', () => {
  for (const key of [{ name: 'q' }, { name: 'c', ctrl: true }]) {
    const s = setup();
    s.input.emit('keypress', '', key);
    assert.equal(s.quits, 1);
    assert.equal(s.timers[0].stopped, true);
    assert.equal(s.input.isRaw, false);
    assert.equal(s.input.listenerCount('keypress'), 0);
    s.terminal.quit();
    assert.equal(s.quits, 1);
    assert.ok(s.screen.endsWith('\x1b[?25h\n'));
  }
});

for (const failure of ['initial render', 'keypress operation', 'gravity render', 'gravity operation', 'restart']) {
  for (const previousRaw of [false, true]) {
    test(`${failure} error restores terminal (previous raw mode: ${previousRaw})`, () => {
      const input = new PassThrough();
      input.isRaw = previousRaw;
      input.setRawMode = raw => { input.isRaw = raw; };
      const error = new Error(`injected ${failure} failure`);
      let screen = '';
      let tick;
      let canceled = 0;
      let quits = 0;
      let failRender = failure === 'initial render';
      let creations = 0;
      const options = {
        input,
        output: { write(text) {
          if (failRender && text.startsWith('\x1b[H')) throw error;
          screen += text;
        } },
        createGame: () => {
          if (++creations > 1) throw error;
          return new Game({ random: () => 0.2 });
        },
        gravity: (game, callbacks) => startGravity(game, {
          ...callbacks,
          now: () => 1000,
          schedule: callback => { tick = callback; return 1; },
          cancel: () => { canceled++; },
        }),
        onQuit: () => { quits++; },
      };
      let terminal;
      const trigger = () => {
        terminal = startTerminal(options);
        assert.equal(input.isRaw, true, 'failure happens after raw mode is enabled');
        if (failure === 'keypress operation') {
          terminal.game.move = () => { throw error; };
          input.emit('keypress', '', { name: 'left' });
        } else if (failure === 'restart') {
          input.emit('keypress', '', { name: 'r' });
        } else {
          terminal.game.advance = () => {
            if (failure === 'gravity operation') throw error;
            return true;
          };
          failRender = true;
          tick();
        }
      };
      assert.throws(trigger, thrown => thrown === error);
      assert.equal(canceled, 1, 'gravity is canceled');
      assert.equal(input.isRaw, previousRaw);
      assert.equal(input.isPaused(), true);
      assert.equal(input.listenerCount('keypress'), 0);
      assert.ok(screen.includes('\x1b[?25l'), 'cursor was hidden before failure');
      assert.ok(screen.endsWith('\x1b[?25h\n'), 'cursor is shown on failure');
      assert.equal(quits, 1);
      tick(); // A queued timer callback cannot update after cleanup.
      terminal?.quit();
      assert.equal(canceled, 1);
      assert.equal(quits, 1);
    });
  }
}

test('terminal escape sequences are decoded into movement and quit', async () => {
  const s = setup();
  try {
    const x = s.terminal.game.active.x;
    s.input.write('\x1b[D');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(s.terminal.game.active.x, x - 1);
    s.input.write('q');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(s.quits, 1);
  } finally { s.terminal.quit(); }
});
