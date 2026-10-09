import { HEIGHT, WIDTH } from './game.js';

// 1 status row + 2 borders + 20 board rows + 1 controls row = 24.
export function renderFrame(game) {
  const cells = game.board.map(row => [...row]);
  if (game.active && !game.gameOver) {
    const { shape, x, y } = game.active;
    for (let row = 0; row < shape.length; row++) {
      for (let col = 0; col < shape[row].length; col++) {
        const bx = x + col;
        const by = y + row;
        if (shape[row][col] && bx >= 0 && bx < WIDTH && by >= 0 && by < HEIGHT) {
          cells[by][bx] = game.active.type;
        }
      }
    }
  }

  const border = `+${'-'.repeat(WIDTH * 2)}+`;
  return [
    `Terminal Tetris | Score: ${game.score} | Lines: ${game.lines}${game.gameOver ? ' | GAME OVER' : ''}`,
    border,
    ...cells.map(row => `|${row.map(cell => cell ? '[]' : '  ').join('')}|`),
    border,
    'Left/Right: move | Up: rotate | Down: drop | Space: slam | Q: quit',
  ].join('\n');
}

export function drawFrame(game, output = process.stdout) {
  const frame = renderFrame(game);
  if (output.isTTY) {
    // Return to the origin, erase stale line contents and any old rows below.
    // No trailing newline: the bottom row must not scroll a 24-row terminal.
    output.write(`\x1b[H${frame.split('\n').join('\x1b[K\n')}\x1b[K\x1b[J`);
  } else {
    output.write(frame);
  }
}
