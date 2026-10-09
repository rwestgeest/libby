import assert from 'node:assert/strict';
import test from 'node:test';
import { Game } from '../src/game.js';
import { DISPLAY_ROWS, renderFrame, TerminalRenderer } from '../src/render.js';

function gameWithO() {
  const game = new Game();
  game.active = null;
  game.spawn('O');
  return game;
}

test('frame contains 22 board/border rows and two status/control rows', () => {
  const game = gameWithO();
  game.score = 123;
  game.lines = 4;
  const frame = renderFrame(game);
  const lines = frame.split('\n');
  assert.equal(DISPLAY_ROWS, 24);
  assert.equal(lines.length, 24);
  assert.equal(lines[0], '+--------------------+');
  assert.equal(lines[21], lines[0]);
  assert.ok(lines.slice(1, 21).every(line => line.length === 22));
  assert.match(lines[22], /Score:123 Lines:4/);
  for (const hint of ['Left/Right', 'Up', 'Down', 'Space', 'R:', 'Q:']) {
    assert.ok(lines[23].includes(hint));
  }
  assert.ok(!frame.endsWith('\n'));
});

test('active and locked cells are visible without mutating game state', () => {
  const game = gameWithO();
  game.board[19][0] = 'T';
  const before = JSON.stringify(game);
  const lines = renderFrame(game).split('\n');
  assert.equal(lines[1], '|        [][]        |');
  assert.equal(lines[2], lines[1]);
  assert.equal(lines[20], '|[]                  |');
  assert.equal(JSON.stringify(game), before);
});

test('renderer consumes visible cells without accessing board or piece geometry', () => {
  let queries = 0;
  const snapshot = Array.from({ length: 20 }, () => Array(10).fill(null));
  snapshot[7][2] = 'J';
  const game = {
    score: 0, lines: 0, gameOver: false,
    get board() { throw new Error('Renderer must not inspect the board'); },
    get active() { throw new Error('Renderer must not inspect the active piece'); },
    visibleCells() { queries++; return snapshot; },
  };
  const before = structuredClone(snapshot);
  const lines = renderFrame(game).split('\n');
  assert.equal(queries, 1);
  assert.equal(lines[8], '|    []              |');
  assert.deepEqual(snapshot, before);
});

test('game-over message replaces status rather than adding rows', () => {
  const game = gameWithO();
  game.active = null;
  game.gameOver = true;
  game.score = 2000;
  const lines = renderFrame(game).split('\n');
  assert.equal(lines.length, 24);
  assert.match(lines[22], /GAME OVER Score:2000/);
  assert.match(lines[23], /R: restart.*Q: quit/);
});

test('normal and game-over frames fit narrow terminals without wrapping', () => {
  const game = gameWithO();
  for (const gameOver of [false, true]) {
    game.gameOver = gameOver;
    for (const columns of [22, 30, 48, 60, 80]) {
      const lines = renderFrame(game, { columns }).split('\n');
      assert.equal(lines.length, 24);
      assert.ok(lines.every(line => line.length <= columns));
      if (gameOver) assert.match(lines[22], /GAME OVER/);
    }
  }
});

test('undersized terminals receive a bounded resize prompt instead of scrolling', () => {
  for (const [columns, rows] of [[21, 24], [80, 23], [10, 5]]) {
    const lines = renderFrame(gameWithO(), { columns, rows }).split('\n');
    assert.ok(lines.length <= rows);
    assert.ok(lines.every(line => line.length <= columns));
    assert.match(lines[0], /Resize/);
  }
});

test('TTY updates overwrite fixed rows without accumulating output', () => {
  const writes = [];
  const renderer = new TerminalRenderer({
    isTTY: true, columns: 80, rows: 24, write: text => writes.push(text),
  });
  const game = gameWithO();
  renderer.render(game);
  game.hardDrop();
  renderer.render(game);
  assert.equal(writes.length, 2);
  for (const text of writes) {
    assert.ok(text.startsWith('\x1b[1;1H\x1b[2K'));
    assert.ok(text.includes('\x1b[24;1H\x1b[2K'));
    assert.ok(!text.includes('\n'));
    assert.ok(!text.includes('\x1b[25;'));
    assert.ok(text.endsWith('\x1b[J'));
  }
});
