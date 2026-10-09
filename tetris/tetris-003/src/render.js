import { BOARD_HEIGHT, BOARD_WIDTH, pieceCells } from './game.js';

export const FRAME_ROWS = BOARD_HEIGHT + 4;
export const FRAME_COLUMNS = 48;

// No trailing newline: printing one at the bottom of a 24-row terminal scrolls.
export function renderFrame(game) {
  const cells = game.board.map(row => [...row]);
  if (game.active && !game.gameOver) {
    for (const { x, y } of pieceCells(game.active)) {
      if (x >= 0 && x < BOARD_WIDTH && y >= 0 && y < BOARD_HEIGHT) {
        cells[y][x] = game.active.type;
      }
    }
  }
  const title = game.gameOver ? 'GAME OVER' : 'Terminal Tetris';
  const header = `${title} | Score: ${game.score} | Lines: ${game.lines}`;
  const border = `+${'-'.repeat(BOARD_WIDTH * 2)}+`;
  return [
    header.slice(0, FRAME_COLUMNS),
    border,
    ...cells.map(row => `|${row.map(cell => cell === null ? '  ' : '[]').join('')}|`),
    border,
    '<> move  Up turn  Down drop  Space slam  Q quit',
  ].join('\n');
}

// The caller owns timing and terminal lifecycle. Small terminals get a single
// clipped notice rather than a wrapped/scrolled game board.
export function drawFrame(game, output = process.stdout) {
  if (!output.isTTY) {
    output.write(renderFrame(game));
    return;
  }
  const columns = output.columns ?? FRAME_COLUMNS;
  const rows = output.rows ?? FRAME_ROWS;
  const frame = columns < FRAME_COLUMNS || rows < FRAME_ROWS
    ? 'Resize terminal to at least 48x24.'.slice(0, Math.max(0, columns - 1))
    : renderFrame(game);
  output.write(`\x1b[H\x1b[2J${frame}`);
}
