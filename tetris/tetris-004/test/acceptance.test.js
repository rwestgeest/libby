import assert from 'node:assert/strict';
import test from 'node:test';
import { Game, TETROMINOES } from '../src/game.js';
import { renderFrame, TerminalRenderer } from '../src/render.js';

function withPiece(type) {
  const game = new Game({ random: () => 0.5 });
  game.active = null;
  assert.equal(game.spawn(type), true);
  return game;
}

function assertDisplay(game) {
  for (const columns of [22, 30, 80]) {
    const frame = renderFrame(game, { columns, rows: 24 });
    const rows = frame.split('\n');
    assert.equal(rows.length, 24);
    assert.ok(rows.every(row => row.length <= columns));
    assert.ok(!frame.endsWith('\n'));
    assert.equal(rows[22].includes('GAME OVER'), game.gameOver);

    let output = '';
    new TerminalRenderer({
      isTTY: true, columns, rows: 24,
      write: text => { output += text; },
    }).render(game);
    const positions = [...output.matchAll(/\x1b\[(\d+);1H/g)].map(match => Number(match[1]));
    assert.deepEqual(positions, Array.from({ length: 24 }, (_, i) => i + 1));
    assert.ok(!output.includes('\n'));
  }
}

test('every tetromino collides with walls and floor and locks exactly four cells', async t => {
  for (const type of Object.keys(TETROMINOES)) {
    await t.test(type, () => {
      const game = withPiece(type);
      // Rotate away from the template orientation before exercising boundaries.
      assert.equal(game.rotate(), true);
      while (game.move(-1)) { /* left boundary */ }
      const left = structuredClone(game.active);
      assert.equal(game.move(-1), false);
      assert.deepEqual(game.active, left);
      assert.equal(game.collides({ ...left, x: left.x - 1 }), true);
      while (game.move(1)) { /* right boundary */ }
      const right = structuredClone(game.active);
      assert.equal(game.move(1), false);
      assert.deepEqual(game.active, right);
      assert.equal(game.collides({ ...right, x: right.x + 1 }), true);
      assert.equal(game.lock(), false);
      while (game.move(0, 1)) { /* floor */ }
      assert.equal(game.move(0, 1), false);
      assert.equal(game.collides(game.active), false);
      const expected = game.visibleCells();
      assertDisplay(game);
      assert.equal(game.lock(), true);
      assert.deepEqual(game.board, expected);
      assert.equal(game.board.flat().filter(cell => cell === type).length, 4);
      assert.equal(game.active, null);
      assert.equal(game.spawn(), true);
      assert.equal(game.collides(game.active), false);
      assertDisplay(game);
    });
  }
});

test('successive clears accumulate line and drop scores and leave an empty board', () => {
  const game = withPiece('O');
  for (let round = 1; round <= 3; round++) {
    // Two nearly completed rows; an O fills both holes through normal drops.
    for (const y of [18, 19]) {
      game.board[y] = Array.from({ length: 10 }, (_, x) => x === 4 || x === 5 ? null : 'J');
    }
    assert.equal(game.softDrop(), true);
    assert.equal(game.hardDrop(), 17);
    assert.equal(game.lines, round * 2);
    assert.equal(game.score, round * (300 + 1 + 34));
    assert.ok(game.board.every(row => row.every(cell => cell === null)));
    assertDisplay(game);
    // Choose the next fixture piece without changing accumulated counters.
    game.active = null;
    assert.equal(game.spawn('O'), true);
  }
});

test('rotation, Tetris clear, repeated spawning and real top-out keep the display within 24 rows', () => {
  const game = withPiece('I');
  assertDisplay(game);
  assert.equal(game.rotate(), true);
  assert.equal(game.move(-1), true);
  assert.equal(game.move(1), true);
  const column = game.active.x + 2;
  for (let y = 16; y < 20; y++) {
    game.board[y] = Array.from({ length: 10 }, (_, x) => x === column ? null : 'J');
  }
  assertDisplay(game);
  assert.equal(game.hardDrop(), 16);
  assert.equal(game.lines, 4);
  assert.equal(game.score, 832);
  assert.ok(game.board.every(row => row.every(cell => cell === null)));
  assertDisplay(game);

  // Stack actual randomly spawned pieces without manually setting gameOver.
  let drops = 0;
  while (!game.gameOver && drops < 30) {
    game.hardDrop();
    drops++;
    assertDisplay(game);
  }
  assert.ok(drops > 1 && drops < 30, 'stacking must reach a blocked spawn');
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  const finalState = JSON.stringify(game);
  assert.equal(game.spawn(), false);
  assert.equal(game.move(1), false);
  assert.equal(game.rotate(), false);
  assert.equal(game.softDrop(), false);
  assert.equal(game.hardDrop(), 0);
  game.update(10000);
  assert.equal(JSON.stringify(game), finalState);
  assertDisplay(game);
});
