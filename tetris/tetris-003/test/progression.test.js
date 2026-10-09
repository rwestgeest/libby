import assert from 'node:assert/strict';
import test from 'node:test';
import { BOARD_HEIGHT, BOARD_WIDTH, Game } from '../src/game.js';

test('gravity spawns, descends one cell per tick, then locks and respawns', () => {
  const game = new Game({ random: () => 0 });
  assert.equal(game.tick(), true);
  assert.equal(game.active.y, 0);
  assert.equal(game.tick(), true);
  assert.equal(game.active.y, 1);
  assert.equal(game.board.flat().filter(Boolean).length, 0);
  while (game.move(0, 1)) { /* reach the floor */ }
  assert.equal(game.tick(), true);
  assert.equal(game.board.flat().filter(Boolean).length, 4);
  assert.equal(game.active.y, 0);
  assert.equal(game.score, 0);
  assert.equal(game.gameOver, false);
});

for (const [count, points] of [[1, 100], [2, 300], [3, 500], [4, 800]]) {
  test(`clearing ${count} rows awards ${points} points and preserves other rows`, () => {
    const game = new Game();
    game.board[2][0] = 'T';
    for (let i = 0; i < count; i++) game.board[BOARD_HEIGHT - 1 - i].fill('I');
    assert.equal(game.clearLines(), count);
    assert.equal(game.score, points);
    assert.equal(game.lines, count);
    assert.equal(game.board.length, BOARD_HEIGHT);
    assert.equal(game.board[2 + count][0], 'T');
    assert.ok(game.board.slice(0, count).every(row => row.every(cell => cell === null)));
    game.board[0][1] = 'O';
    assert.equal(game.board[1][1], null); // New rows are independent.
    assert.equal(game.clearLines(), 0);
    assert.equal(game.score, points);
  });
}

test('nonadjacent complete rows collapse together and scores accumulate', () => {
  const game = new Game();
  game.board[17].fill('J');
  game.board[19].fill('L');
  game.board[18][3] = 'S';
  assert.equal(game.clearLines(), 2);
  assert.equal(game.board[19][3], 'S');
  game.board[19].fill('O');
  assert.equal(game.clearLines(), 1);
  assert.equal(game.score, 400);
  assert.equal(game.lines, 3);
});

test('gravity clears locked rows before checking the next spawn', () => {
  const game = new Game();
  game.spawn('O');
  game.move(0, 18);
  for (const y of [18, 19]) {
    for (let x = 0; x < BOARD_WIDTH; x++) {
      if (x !== 4 && x !== 5) game.board[y][x] = 'J';
    }
  }
  assert.equal(game.tick(), true);
  assert.equal(game.lines, 2);
  assert.equal(game.score, 300);
  assert.ok(game.board.every(row => row.every(cell => cell === null)));
  assert.ok(game.active);
});

test('blocked spawn ends the game and further progression is inert', () => {
  const game = new Game();
  game.board[0][4] = 'Z';
  assert.equal(game.spawn('O'), false);
  assert.equal(game.gameOver, true);
  const before = structuredClone(game.board);
  assert.equal(game.tick(), false);
  assert.equal(game.spawn('I'), false);
  assert.equal(game.move(1), false);
  assert.equal(game.rotate(), false);
  assert.equal(game.lock(), false);
  assert.equal(game.clearLines(), 0);
  assert.deepEqual(game.board, before);
  assert.equal(game.active, null);
});

test('gravity detects game over on respawn after locking', () => {
  const game = new Game();
  game.spawn('O');
  game.move(0, 18);
  game.board[0].fill('Z');
  game.board[0][0] = null; // Block every spawn without forming a complete row.
  game.board[1][4] = 'Z'; // Also block the I piece's lower spawn row.
  assert.equal(game.tick(), false);
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  assert.equal(game.board[19][4], 'O');
});
