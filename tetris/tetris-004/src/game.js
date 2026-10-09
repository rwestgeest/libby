export const BOARD_WIDTH = 10;
export const BOARD_HEIGHT = 20;

// Square matrices preserve a stable rotation center, including empty cells.
export const TETROMINOES = Object.freeze({
  I: freezeShape([[0, 0, 0, 0], [1, 1, 1, 1], [0, 0, 0, 0], [0, 0, 0, 0]]),
  J: freezeShape([[1, 0, 0], [1, 1, 1], [0, 0, 0]]),
  L: freezeShape([[0, 0, 1], [1, 1, 1], [0, 0, 0]]),
  O: freezeShape([[1, 1], [1, 1]]),
  S: freezeShape([[0, 1, 1], [1, 1, 0], [0, 0, 0]]),
  T: freezeShape([[0, 1, 0], [1, 1, 1], [0, 0, 0]]),
  Z: freezeShape([[1, 1, 0], [0, 1, 1], [0, 0, 0]]),
});

function freezeShape(shape) {
  return Object.freeze(shape.map(row => Object.freeze(row)));
}

export function createBoard() {
  return Array.from({ length: BOARD_HEIGHT }, () => Array(BOARD_WIDTH).fill(null));
}

export function rotateClockwise(shape) {
  return shape.map((row, y) => row.map((_, x) => shape[shape.length - 1 - x][y]));
}

export class Game {
  constructor({ random = Math.random } = {}) {
    this.board = createBoard();
    this.active = null;
    this.random = random;
    this.bag = [];
    this.spawn();
  }

  nextType() {
    if (this.bag.length === 0) {
      this.bag = Object.keys(TETROMINOES);
      // Seven-bag randomization: each set contains all seven pieces once.
      for (let i = this.bag.length - 1; i > 0; i--) {
        const j = Math.floor(this.random() * (i + 1));
        [this.bag[i], this.bag[j]] = [this.bag[j], this.bag[i]];
      }
    }
    return this.bag.pop();
  }

  spawn(type) {
    if (this.active) return false;
    if (type === undefined) type = this.nextType();
    const template = TETROMINOES[type];
    if (!template) throw new RangeError(`Unknown tetromino: ${type}`);
    const piece = {
      type,
      shape: template.map(row => [...row]),
      x: Math.floor((BOARD_WIDTH - template.length) / 2),
      y: 0,
    };
    if (this.collides(piece)) return false;
    this.active = piece;
    return true;
  }

  collides(piece) {
    return piece.shape.some((row, y) => row.some((filled, x) => {
      if (!filled) return false;
      const boardX = piece.x + x;
      const boardY = piece.y + y;
      return boardX < 0 || boardX >= BOARD_WIDTH ||
        boardY < 0 || boardY >= BOARD_HEIGHT ||
        this.board[boardY][boardX] !== null;
    }));
  }

  move(dx, dy = 0) {
    if (!Number.isInteger(dx) || !Number.isInteger(dy)) {
      throw new TypeError('Movement must use integer offsets');
    }
    if (!this.active) return false;
    const candidate = { ...this.active, x: this.active.x + dx, y: this.active.y + dy };
    if (this.collides(candidate)) return false;
    this.active = candidate;
    return true;
  }

  rotate() {
    if (!this.active) return false;
    const candidate = { ...this.active, shape: rotateClockwise(this.active.shape) };
    // A blocked rotation leaves the piece unchanged (no wall kicks).
    if (this.collides(candidate)) return false;
    this.active = candidate;
    return true;
  }

  lock() {
    if (!this.active || this.collides(this.active)) return false;
    // Only resting pieces can lock; falling pieces remain active.
    if (!this.collides({ ...this.active, y: this.active.y + 1 })) return false;
    const { type, shape, x, y } = this.active;
    shape.forEach((row, dy) => row.forEach((filled, dx) => {
      if (filled) this.board[y + dy][x + dx] = type;
    }));
    this.active = null;
    return true;
  }
}
