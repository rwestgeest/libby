import assert from 'node:assert/strict';
import { test } from 'node:test';
import { Game } from '../src/game.js';
import { drawFrame, renderFrame } from '../src/render.js';

test('gameplay frame fits 24 rows including borders, score and controls', () => {
  const game = new Game();
  game.spawn('O');
  game.board[19][0] = 'T';
  game.score = 123;
  game.lines = 4;
  const before = JSON.stringify(game);
  const rows = renderFrame(game).split('\n');
  assert.equal(rows.length, 24);
  assert.match(rows[0], /Score: 123 \| Lines: 4/);
  assert.equal(rows[1], '+--------------------+');
  assert.equal(rows[22], rows[1]);
  assert.equal(rows[2], '|        [][]        |');
  assert.equal(rows[21], '|[]                  |');
  assert.match(rows[23], /Left\/Right.*Up.*Down.*Space.*Q/);
  assert.ok(rows.every(row => row.length <= 80));
  assert.equal(JSON.stringify(game), before, 'rendering must not mutate the model');
});

test('game-over message stays inside the same layout', () => {
  const game = new Game();
  game.spawn('O');
  game.gameOver = true;
  const rows = renderFrame(game).split('\n');
  assert.equal(rows.length, 24);
  assert.match(rows[0], /GAME OVER/);
  assert.ok(rows.slice(2, 22).every(row => row === '|                    |'));
  assert.match(rows[23], /Q: quit/);
});

test('TTY redraws return to origin and erase stale content without a final newline', () => {
  const writes = [];
  const output = { isTTY: true, write: text => writes.push(text) };
  const game = new Game();
  drawFrame(game, output);
  game.gameOver = true;
  drawFrame(game, output);
  assert.equal(writes.length, 2);
  for (const frame of writes) {
    assert.ok(frame.startsWith('\x1b[H'));
    assert.ok(frame.endsWith('\x1b[K\x1b[J'));
    assert.equal(frame.split('\n').length, 24);
    assert.equal(frame.replace(/\x1b\[[A-Za-z]/g, '').split('\n').length, 24);
  }
});

test('every frame in deterministic gameplay through a blocked spawn fits 24 rows', () => {
  const game = new Game({ random: () => 0.5 });
  let frames = 0;
  const checkFrame = () => {
    const before = JSON.stringify(game);
    const frame = renderFrame(game);
    const rows = frame.split('\n');
    assert.equal(rows.length, 24);
    assert.ok(rows.every(row => row.length <= 80), 'no wrapping on an 80-column terminal');
    assert.equal(frame.endsWith('\n'), false);
    assert.match(rows[0], new RegExp(`Score: ${game.score} \\| Lines: ${game.lines}`));
    assert.match(rows[23], /Q: quit/);
    assert.equal(JSON.stringify(game), before);
    frames++;
    return rows;
  };
  for (let piece = 0; piece < 100 && !game.gameOver; piece++) {
    checkFrame();
    game.rotate();
    checkFrame();
    game.move(piece % 2 ? 1 : -1);
    checkFrame();
    game.softDrop();
    checkFrame();
    game.hardDrop();
    checkFrame();
  }
  assert.ok(frames > 5, 'exercise multiple pieces');
  assert.equal(game.gameOver, true, 'stack eventually blocks spawning');
  assert.equal(game.active, null);
  assert.match(checkFrame()[0], /GAME OVER/);
});

test('non-TTY output is a plain frame with no escape sequences', () => {
  const game = new Game();
  let text = '';
  drawFrame(game, { isTTY: false, write: chunk => { text += chunk; } });
  assert.equal(text, renderFrame(game));
  assert.ok(!text.includes('\x1b'));
});
