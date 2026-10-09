import assert from 'node:assert/strict';
import test from 'node:test';
import { Game, GRAVITY_INTERVAL_MS } from '../src/game.js';
import { startGravity } from '../src/gravity.js';

function squareGame() {
  return new Game({ random: () => 1 / 7 });
}

function clock(game) {
  let time = 0;
  let callback;
  let scheduled = 0;
  let cancelled = 0;
  let updates = 0;
  const stop = startGravity(game, {
    now: () => time,
    schedule(fn, interval) {
      assert.equal(interval, GRAVITY_INTERVAL_MS);
      callback = fn;
      scheduled++;
      return 42;
    },
    cancel(id) { assert.equal(id, 42); cancelled++; },
    onUpdate(state) { assert.equal(state, game); updates++; },
  });
  return {
    stop,
    advance(ms) { time += ms; callback?.(); },
    get scheduled() { return scheduled; },
    get cancelled() { return cancelled; },
    get updates() { return updates; },
  };
}

test('gravity accumulates partial intervals and catches up after delayed ticks', () => {
  const game = squareGame();
  assert.equal(game.advance(499), false);
  assert.equal(game.active.y, 0);
  assert.equal(game.advance(1), true);
  assert.equal(game.active.y, 1);
  game.advance(1250);
  assert.equal(game.active.y, 3);
  game.advance(250);
  assert.equal(game.active.y, 4);
  for (const invalid of [-1, NaN, Infinity, '500']) {
    assert.throws(() => game.advance(invalid), RangeError);
  }
});

test('gravity locks a grounded piece and spawns the next piece', () => {
  const game = squareGame();
  game.advance(18 * GRAVITY_INTERVAL_MS);
  assert.equal(game.active.y, 18);
  assert.equal(game.board.flat().filter(Boolean).length, 0);
  game.advance(GRAVITY_INTERVAL_MS);
  assert.equal(game.board.flat().filter(Boolean).length, 4);
  assert.equal(game.active.type, 'O');
  assert.equal(game.active.y, 0);
  assert.equal(game.gameOver, false);
});

test('clearing one through four rows awards scores and preserves surviving rows', () => {
  for (const [count, score] of [[1, 100], [2, 300], [3, 500], [4, 800]]) {
    const game = squareGame();
    game.board[0][0] = 'J';
    game.board[10][3] = 'T';
    for (let y = 20 - count; y < 20; y++) game.board[y].fill('I');
    assert.equal(game.clearRows(), count);
    assert.equal(game.score, score);
    assert.equal(game.lines, count);
    assert.equal(game.board.length, 20);
    assert.equal(game.board[count][0], 'J');
    assert.equal(game.board[10 + count][3], 'T');
    assert.equal(game.clearRows(), 0);
    assert.equal(game.score, score);
    assert.equal(new Set(game.board).size, 20);
  }
});

test('locking clears multiple rows before spawning and scores accumulate', () => {
  const game = squareGame();
  for (let round = 1; round <= 2; round++) {
    for (let y = 18; y < 20; y++) {
      game.board[y].fill('I');
      game.board[y][4] = null;
      game.board[y][5] = null;
    }
    while (game.move(0, 1)) {}
    assert.equal(game.step(), true);
    assert.equal(game.lines, round * 2);
    assert.equal(game.score, round * 300);
    assert.ok(game.board.flat().every(cell => cell === null));
    assert.equal(game.active.y, 0);
  }
});

test('nonadjacent completed rows clear without deleting partial rows', () => {
  const game = squareGame();
  game.board[17].fill('I');
  game.board[19].fill('T');
  game.board[18][0] = 'J';
  assert.equal(game.clearRows(), 2);
  assert.equal(game.board[19][0], 'J');
  assert.equal(game.board.flat().filter(Boolean).length, 1);
});

test('blocked spawning ends play and subsequent gravity cannot mutate the board', () => {
  const game = squareGame();
  while (game.move(0, 1)) {}
  game.board[0][4] = 'J';
  assert.equal(game.step(), true);
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  const board = structuredClone(game.board);
  assert.equal(game.advance(10000), false);
  assert.equal(game.step(), false);
  assert.equal(game.spawn('I'), false);
  assert.equal(game.move(1, 0), false);
  assert.equal(game.rotate(), false);
  assert.deepEqual(game.board, board);
});

test('row clearing opens the spawn area before the game-over check', () => {
  const game = squareGame();
  while (game.move(0, 1)) {}
  game.board[0].fill('J');
  game.step();
  assert.equal(game.gameOver, false);
  assert.equal(game.lines, 1);
  assert.equal(game.active.type, 'O');
});

test('scheduled gravity notifies changes and stops idempotently', () => {
  const game = squareGame();
  const timer = clock(game);
  timer.advance(250);
  assert.equal(timer.updates, 0);
  timer.advance(1250);
  assert.equal(game.active.y, 3);
  assert.equal(timer.updates, 1);
  timer.stop();
  timer.stop();
  assert.equal(timer.cancelled, 1);
  timer.advance(500);
  assert.equal(game.active.y, 3);
});

test('scheduled gravity stops automatically on game over and does not start ended games', () => {
  const game = squareGame();
  while (game.move(0, 1)) {}
  game.board[0][4] = 'J';
  const timer = clock(game);
  timer.advance(500);
  assert.equal(game.gameOver, true);
  assert.equal(timer.cancelled, 1);
  assert.equal(timer.updates, 1);
  timer.advance(500);
  assert.equal(timer.updates, 1);
  assert.equal(clock(game).scheduled, 0);
});
