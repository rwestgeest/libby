import test from 'node:test';
import assert from 'node:assert/strict';
import { Game } from '../src/engine.js';
import { render, handleKey, SCREEN_ROWS, MIN_COLUMNS } from '../src/terminal.js';

test('playing and game-over frames fit 24 rows and include controls and score', () => {
  const game = new Game();
  for (const over of [false, true]) {
    game.gameOver = over;
    const frame = render(game);
    assert.equal(frame.split('\n').length, SCREEN_ROWS);
    assert.equal(SCREEN_ROWS, 24);
    assert.ok(frame.split('\n').every(line => line.length <= MIN_COLUMNS));
    assert.match(frame, /Score 0/);
    assert.match(frame, /Q: quit/);
    if (over) assert.match(frame, /GAME OVER/);
  }
});

test('render overlays the active piece without mutating the board', () => {
  const game = new Game();
  const before = structuredClone(game.board);
  assert.equal(render(game).match(/\[\]/g).length, 4);
  assert.deepEqual(game.board, before);
});

test('keyboard controls move, rotate, drop, and quit', () => {
  const game = new Game();
  game.spawn('T');
  const x = game.active.x;
  handleKey(game, { name: 'left' });
  assert.equal(game.active.x, x - 1);
  handleKey(game, { name: 'right' });
  assert.equal(game.active.x, x);
  const shape = structuredClone(game.active.shape);
  handleKey(game, { name: 'up' });
  assert.notDeepEqual(game.active.shape, shape);
  handleKey(game, { name: 'down' });
  assert.equal(game.active.y, 1);
  assert.equal(game.score, 1);
  handleKey(game, { name: 'space' });
  assert.equal(game.board.flat().filter(Boolean).length, 4);
  assert.equal(handleKey(game, { name: 'q' }), 'quit');
  assert.equal(handleKey(game, { name: 'c', ctrl: true }), 'quit');
});
