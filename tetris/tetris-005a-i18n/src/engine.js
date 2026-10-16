export const WIDTH = 10;
export const HEIGHT = 20;

// Square matrices preserve each piece's rotation center.
export const SHAPES = Object.freeze({
  I: ['0000', '1111', '0000', '0000'],
  J: ['100', '111', '000'],
  L: ['001', '111', '000'],
  O: ['11', '11'],
  S: ['011', '110', '000'],
  T: ['010', '111', '000'],
  Z: ['110', '011', '000'],
});
for (const shape of Object.values(SHAPES)) Object.freeze(shape);

export const emptyBoard = () => Array.from({ length: HEIGHT }, () => Array(WIDTH).fill(null));

/** Occupied board coordinates, including cells above the visible board. */
export function cells(piece) {
  const result = [];
  piece.matrix.forEach((row, y) => row.forEach((value, x) => {
    if (value) result.push({ x: piece.x + x, y: piece.y + y });
  }));
  return result;
}

/**
 * Pure game rules, independent of timers and terminal I/O.
 * Board cells contain null or a tetromino letter. Inject random() for repeatable games.
 * Actions return false when blocked or after game over. tick() applies one gravity step.
 */
export class Game {
  constructor({ random = Math.random } = {}) {
    this.random = random;
    this.reset();
  }

  reset() {
    this.board = emptyBoard();
    this.score = 0;
    this.lines = 0;
    this.gameOver = false;
    this.active = null;
    this.bag = [];
    this.next = this.draw();
    this.spawn();
  }

  get level() { return 1 + Math.floor(this.lines / 10); }
  get gravityMs() { return Math.max(80, 800 - (this.level - 1) * 60); }

  draw() {
    if (!this.bag.length) {
      this.bag = Object.keys(SHAPES);
      for (let i = this.bag.length - 1; i > 0; i--) {
        const j = Math.floor(this.random() * (i + 1));
        [this.bag[i], this.bag[j]] = [this.bag[j], this.bag[i]];
      }
    }
    return this.bag.pop();
  }

  spawn() {
    if (this.gameOver) return false;
    const type = this.next;
    this.next = this.draw();
    const matrix = SHAPES[type].map(row => [...row].map(Number));
    this.active = { type, matrix, x: Math.floor((WIDTH - matrix.length) / 2), y: 0 };
    if (!this.fits(this.active)) {
      this.active = null;
      this.gameOver = true;
      return false;
    }
    return true;
  }

  fits(piece) {
    return cells(piece).every(({ x, y }) =>
      x >= 0 && x < WIDTH && y < HEIGHT && (y < 0 || this.board[y][x] === null));
  }

  move(dx) {
    if (this.gameOver || !this.active || ![-1, 1].includes(dx)) return false;
    const moved = { ...this.active, x: this.active.x + dx };
    if (!this.fits(moved)) return false;
    this.active = moved;
    return true;
  }

  // Clockwise by default; simple horizontal wall kicks, not guideline SRS.
  rotate(direction = 1) {
    if (this.gameOver || !this.active || ![-1, 1].includes(direction)) return false;
    const old = this.active.matrix;
    const n = old.length;
    const matrix = old.map((row, y) => row.map((_, x) =>
      direction === 1 ? old[n - 1 - x][y] : old[x][n - 1 - y]));
    for (const dx of [0, -1, 1, -2, 2]) {
      const rotated = { ...this.active, matrix, x: this.active.x + dx };
      if (this.fits(rotated)) {
        this.active = rotated;
        return true;
      }
    }
    return false;
  }

  step(awardPoint = false) {
    if (this.gameOver || !this.active) return false;
    const moved = { ...this.active, y: this.active.y + 1 };
    if (this.fits(moved)) {
      this.active = moved;
      if (awardPoint) this.score++;
    } else {
      this.lock();
    }
    return true;
  }

  tick() { return this.step(); }
  softDrop() { return this.step(true); }

  hardDrop() {
    if (this.gameOver || !this.active) return false;
    while (this.fits({ ...this.active, y: this.active.y + 1 })) {
      this.active.y++;
      this.score += 2;
    }
    this.lock();
    return true;
  }

  lock() {
    if (this.gameOver || !this.active) return false;
    const occupied = cells(this.active);
    if (occupied.some(({ y }) => y < 0)) {
      this.gameOver = true;
      this.active = null;
      return false;
    }
    for (const { x, y } of occupied) this.board[y][x] = this.active.type;
    this.clearLines();
    return this.spawn();
  }

  clearLines() {
    const remaining = this.board.filter(row => row.some(cell => cell === null));
    const count = HEIGHT - remaining.length;
    if (count) {
      this.score += [0, 100, 300, 500, 800][count] * this.level;
      this.lines += count;
      this.board = [
        ...Array.from({ length: count }, () => Array(WIDTH).fill(null)),
        ...remaining,
      ];
    }
    return count;
  }
}
