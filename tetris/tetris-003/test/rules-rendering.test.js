import assert from 'node:assert/strict';
import test from 'node:test';
import {
  BOARD_HEIGHT, BOARD_WIDTH, Game, PIECE_TYPES, collides, createBoard,
  pieceCells, rotateShape,
} from '../src/game.js';
import { FRAME_COLUMNS, renderFrame } from '../src/render.js';

function assertFrameFits(game) {
  const frame = renderFrame(game);
  const rows = frame.split('\n');
  assert.equal(rows.length, 24);
  assert.ok(rows.every(row => row.length <= FRAME_COLUMNS));
  assert.ok(!frame.endsWith('\n'));
  assert.equal(rows.filter(row => row.startsWith('|')).length, BOARD_HEIGHT);
  assert.match(rows.at(-1), /Q quit/);
  return rows;
}

for (const type of PIECE_TYPES) {
  test(`${type}: all orientations collide at boundaries and with settled cells`, () => {
    const game = new Game();
    game.spawn(type);
    let shape = game.active.shape;
    for (let turn = 0; turn < 4; turn++) {
      const piece = { type, shape, x: 3, y: 5 };
      const cells = pieceCells(piece);
      assert.equal(cells.length, 4);
      const board = createBoard();
      assert.equal(collides(board, piece), false);
      for (const cell of cells) {
        board[cell.y][cell.x] = 'Z';
        assert.equal(collides(board, piece), true);
        board[cell.y][cell.x] = null;
      }
      const minX = Math.min(...cells.map(cell => cell.x));
      const maxX = Math.max(...cells.map(cell => cell.x));
      const minY = Math.min(...cells.map(cell => cell.y));
      const maxY = Math.max(...cells.map(cell => cell.y));
      for (const [dx, dy] of [
        [-minX, 0], [BOARD_WIDTH - 1 - maxX, 0],
        [0, -minY], [0, BOARD_HEIGHT - 1 - maxY],
      ]) {
        const boundary = { ...piece, x: piece.x + dx, y: piece.y + dy };
        assert.equal(collides(board, boundary), false);
        assert.equal(collides(board, {
          ...boundary,
          x: boundary.x + Math.sign(dx),
          y: boundary.y + Math.sign(dy),
        }), true);
      }
      shape = rotateShape(shape);
    }
  });
}

test('a locked Tetris clears four rows and renders the updated score and empty board', () => {
  const game = new Game({ random: () => 0.5 });
  game.spawn('I');
  game.rotate();
  while (game.move(0, 1)) { /* ground the vertical I */ }
  const gap = pieceCells(game.active)[0].x;
  for (let y = BOARD_HEIGHT - 4; y < BOARD_HEIGHT; y++) {
    game.board[y].fill('J');
    game.board[y][gap] = null;
  }
  assertFrameFits(game);
  assert.equal(game.tick(), true);
  assert.equal(game.score, 800);
  assert.equal(game.lines, 4);
  assert.ok(game.board.every(row => row.every(cell => cell === null)));
  assert.ok(game.active);
  const rows = assertFrameFits(game);
  assert.match(rows[0], /Score: 800.*Lines: 4/);
  assert.ok(rows.slice(18, 22).every(row => row === '|                    |'));
});

test('repeated gravity reaches game over and every displayed frame stays within 24 rows', () => {
  const game = new Game({ random: () => 0.5 });
  // Repeated O pieces build a stack without completing a row.
  game.bag = Array(11).fill('O');
  let ticks = 0;
  while (!game.gameOver && ticks < 250) {
    game.tick();
    assertFrameFits(game);
    ticks++;
  }
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  assert.equal(game.board.flat().filter(Boolean).length, 40);
  const before = JSON.stringify(game);
  const rows = assertFrameFits(game);
  assert.match(rows[0], /GAME OVER.*Score: 0.*Lines: 0/);
  assert.equal(rows[2], '|        [][]        |');
  assert.equal(rows[21], '|        [][]        |');
  assert.equal(game.tick(), false);
  assert.equal(JSON.stringify(game), before);
});
