import { BOARD_HEIGHT, BOARD_WIDTH } from './game.js';

export const DISPLAY_ROWS = BOARD_HEIGHT + 4;
const BOARD_COLUMNS = BOARD_WIDTH * 2 + 2;
const DEFAULT_COLUMNS = 80;

// Build a plain-text snapshot without modifying the game. There is deliberately
// no trailing newline: on a 24-row terminal it would scroll the top border away.
export function renderFrame(game, { columns = DEFAULT_COLUMNS, rows = DISPLAY_ROWS } = {}) {
  if (columns < BOARD_COLUMNS || rows < DISPLAY_ROWS) {
    return `Resize to ${BOARD_COLUMNS}x${DISPLAY_ROWS}`.slice(0, Math.max(0, columns));
  }
  const cells = game.visibleCells();
  const border = `+${'-'.repeat(BOARD_WIDTH * 2)}+`;
  const status = game.gameOver
    ? `GAME OVER Score:${game.score} Lines:${game.lines}`
    : `Terminal Tetris Score:${game.score} Lines:${game.lines}`;
  const compactStatus = game.gameOver
    ? `GAME OVER S:${game.score}`
    : `Score:${game.score} Lines:${game.lines}`;
  const controls = 'Left/Right: move Up: rotate Down: drop Space: hard drop R: restart Q: quit';
  const compactControls = '< >:move ^:turn v:drop Space:hard R:restart Q:quit';
  const tinyControls = '<> ^ v Spc R Q';
  return [
    border,
    ...cells.map(row => `|${row.map(cell => cell === null ? '  ' : '[]').join('')}|`),
    border,
    (status.length <= columns ? status : compactStatus).slice(0, columns),
    (controls.length <= columns ? controls :
      compactControls.length <= columns ? compactControls : tinyControls),
  ].join('\n');
}

export class TerminalRenderer {
  constructor(output = process.stdout) {
    this.output = output;
  }

  render(game) {
    const frame = renderFrame(game, {
      columns: this.output.columns ?? DEFAULT_COLUMNS,
      rows: this.output.rows ?? DISPLAY_ROWS,
    });
    if (!this.output.isTTY) {
      this.output.write(frame);
      return;
    }
    // Explicit positioning avoids newline conversion assumptions and never
    // writes a newline at the bottom edge. Erasing each row clears old scores.
    const lines = frame.split('\n');
    this.output.write(lines.map((line, index) =>
      `\x1b[${index + 1};1H\x1b[2K${line}`).join('') + '\x1b[J');
  }
}
