import test from 'node:test';
import assert from 'node:assert/strict';
import stringWidth from 'string-width';
import { Game } from '../src/engine.js';
import { render } from '../src/render.js';
import { startApp } from '../src/app.js';
import { english } from '../src/messages.js';
import { clipColumns } from '../src/text.js';

for (const [grapheme, width] of [['界', 2], ['e\u0301', 1], ['😀', 2], ['👩🏽‍💻', 2], ['🇬🇧', 2]]) {
  test(`clips whole graphemes by terminal width: ${grapheme}`, () => {
    assert.equal(clipColumns(`a${grapheme}b`, width), 'a');
    assert.equal(clipColumns(`a${grapheme}b`, width + 1), `a${grapheme}`);
    const messages = { ...english, title: grapheme.repeat(100), resize: () => grapheme.repeat(100) };
    for (const columns of [1, 2, 3, 59, 60, 80]) {
      const lines = render(new Game(), { columns, messages });
      assert.ok(lines.length <= 24);
      for (const line of lines) assert.ok(stringWidth(line) < columns);
      const text = columns >= 60 ? lines[0].substring(24) : lines[0];
      assert.equal(text, grapheme.repeat(Math.floor((columns >= 60 ? 35 : columns - 1) / width)));
    }
  });
}

test('catalogue replaces labels, controls, dynamic values, states and startup error', () => {
  const game = new Game();
  game.score = 123;
  const messages = Object.fromEntries(Object.entries(english).map(([key, value]) =>
    [key, typeof value === 'function' ? (...args) => `${key}=${args.join(',')}` : `@${key}`]));
  let lines = render(game, { messages });
  for (const key of ['title', 'move', 'rotateClockwise', 'rotateCounterclockwise', 'softDrop', 'hardDrop', 'pauseRestart', 'quit', 'playing']) {
    assert.ok(lines.some(line => line.includes(`@${key}`)), key);
  }
  for (const label of ['score=123', 'lines=0', 'level=1', `next=${game.next}`]) {
    assert.ok(lines.some(line => line.includes(label)), label);
  }
  assert.ok(render(game, { messages, paused: true })[20].includes('@paused'));
  game.gameOver = true;
  assert.ok(render(game, { messages })[20].includes('@gameOver'));
  assert.deepEqual(render(game, { messages, columns: 40 }), ['resize=60,22']);
  assert.throws(() => startApp({ input: {}, output: {}, messages }), { message: '@interactiveRequired' });
});

test('all states stay within 24 rows, including messages containing line breaks', () => {
  const game = new Game();
  for (const paused of [false, true]) {
    for (const gameOver of [false, true]) {
      game.gameOver = gameOver;
      const lines = render(game, { paused, messages: { ...english, title: 'hello\nworld\r\t界' } });
      assert.equal(lines.length, 22);
      assert.ok(lines.every(line => !/[\r\n\t]/.test(line) && stringWidth(line) < 60));
    }
  }
  assert.deepEqual(render(game, { rows: 0 }), []);
});
