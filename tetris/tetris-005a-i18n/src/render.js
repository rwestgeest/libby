import { cells, SHAPES } from './engine.js';
import { english } from './messages.js';
import { clipColumns } from './text.js';

export const MIN_COLUMNS = 60;
export const MIN_ROWS = 22;

export function terminalFits(columns, rows) {
  return columns >= MIN_COLUMNS && rows >= MIN_ROWS;
}

// ASCII-only cells have predictable terminal widths. No trailing newline:
// even at the minimum height, drawing never scrolls the terminal.
export function render(game, { paused = false, columns = 80, rows = 24, messages = english } = {}) {
  if (!terminalFits(columns, rows)) {
    return rows < 1 ? [] : [clipColumns(messages.resize(MIN_COLUMNS, MIN_ROWS), columns - 1)];
  }
  const board = game.board.map(row => [...row]);
  if (game.active) {
    for (const { x, y } of cells(game.active)) {
      if (y >= 0 && y < board.length) board[y][x] = game.active.type;
    }
  }
  const status = game.gameOver ? messages.gameOver : paused ? messages.paused : messages.playing;
  const panel = [
    messages.title, '', messages.score(game.score), messages.lines(game.lines),
    messages.level(game.level), '', messages.next(game.next),
    ...SHAPES[game.next].map(row => [...row].map(c => c === '1' ? '[]' : '  ').join('')),
  ];
  // Fixed panel positions keep controls and status visible for every shape.
  panel[12] = messages.move;
  panel[13] = messages.rotateClockwise;
  panel[14] = messages.rotateCounterclockwise;
  panel[15] = messages.softDrop;
  panel[16] = messages.hardDrop;
  panel[17] = messages.pauseRestart;
  panel[18] = messages.quit;
  panel[20] = status;
  const lines = ['+--------------------+', ...board.map(row =>
    `|${row.map(cell => cell ? '[]' : ' .').join('')}|`), '+--------------------+'];
  return lines.map((line, i) => clipColumns(`${line}  ${panel[i] ?? ''}`.trimEnd(), MIN_COLUMNS - 1));
}
