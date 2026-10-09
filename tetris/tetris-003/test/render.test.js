import assert from 'node:assert/strict';
import test from 'node:test';
import { Game } from '../src/game.js';
import { drawFrame, renderFrame, FRAME_COLUMNS, FRAME_ROWS } from '../src/render.js';

test('normal and game-over frames include all information within 24 rows', () => {
  const game = new Game();
  game.spawn('T');
  game.score = 800;
  game.lines = 4;
  for (const gameOver of [false, true]) {
    game.gameOver = gameOver;
    const frame = renderFrame(game);
    const rows = frame.split('\n');
    assert.equal(rows.length, FRAME_ROWS);
    assert.equal(FRAME_ROWS, 24);
    assert.ok(rows.every(row => row.length <= FRAME_COLUMNS));
    assert.match(rows[0], gameOver ? /GAME OVER/ : /Terminal Tetris/);
    assert.match(rows[0], /Score: 800.*Lines: 4/);
    assert.equal(rows[1], '+--------------------+');
    assert.equal(rows[22], rows[1]);
    assert.match(rows[23], /move.*turn.*drop.*Space.*Q quit/);
    assert.ok(!frame.endsWith('\n'));
  }
});

test('renders active and settled cells without mutating the game', () => {
  const game = new Game();
  game.spawn('O');
  game.board[19][0] = 'I';
  const before = JSON.stringify(game);
  const rows = renderFrame(game).split('\n');
  assert.equal(rows[2], '|        [][]        |');
  assert.equal(rows[21], '|[]                  |');
  assert.equal(JSON.stringify(game), before);
});

test('terminal drawing replaces the screen without a trailing newline', () => {
  let text = '';
  drawFrame(new Game(), {
    isTTY: true, columns: 48, rows: 24,
    write: chunk => { text += chunk; },
  });
  assert.ok(text.startsWith('\x1b[H\x1b[2J'));
  assert.equal(text.split('\n').length, 24);
  assert.ok(!text.endsWith('\n'));
});

test('undersized terminals get a clipped one-line notice', () => {
  for (const [columns, rows] of [[20, 24], [80, 10]]) {
    let text = '';
    drawFrame(new Game(), {
      isTTY: true, columns, rows,
      write: chunk => { text += chunk; },
    });
    const visible = text.replace(/\x1b\[[0-9]*[HJ]/g, '');
    assert.ok(visible.length < columns);
    assert.ok(!visible.includes('\n'));
    assert.match(visible, /Resize terminal/);
  }
});
