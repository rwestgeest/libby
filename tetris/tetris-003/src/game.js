export const BOARD_WIDTH = 10;
export const BOARD_HEIGHT = 20;

// Square matrices keep rotation centers stable, including the I piece.
export const TETROMINOES = Object.freeze({
  I: [[0, 0, 0, 0], [1, 1, 1, 1], [0, 0, 0, 0], [0, 0, 0, 0]],
  O: [[1, 1], [1, 1]],
  T: [[0, 1, 0], [1, 1, 1], [0, 0, 0]],
  S: [[0, 1, 1], [1, 1, 0], [0, 0, 0]],
  Z: [[1, 1, 0], [0, 1, 1], [0, 0, 0]],
  J: [[1, 0, 0], [1, 1, 1], [0, 0, 0]],
  L: [[0, 0, 1], [1, 1, 1], [0, 0, 0]],
});
for (const shape of Object.values(TETROMINOES)) {
  shape.forEach(Object.freeze);
  Object.freeze(shape);
}
export const PIECE_TYPES = Object.freeze(Object.keys(TETROMINOES));

export function createBoard() {
  return Array.from({ length: BOARD_HEIGHT }, () => Array(BOARD_WIDTH).fill(null));
}

export function rotateShape(shape, direction = 1) {
  const size = shape.length;
  return Array.from({ length: size }, (_, y) =>
    Array.from({ length: size }, (_, x) =>
      direction === -1 ? shape[x][size - 1 - y] : shape[size - 1 - x][y]));
}

export function pieceCells(piece) {
  const cells = [];
  piece.shape.forEach((row, y) => row.forEach((filled, x) => {
    if (filled) cells.push({ x: piece.x + x, y: piece.y + y });
  }));
  return cells;
}

export function collides(board, piece) {
  return pieceCells(piece).some(({ x, y }) =>
    x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT || board[y][x] !== null);
}

export class Game {
  constructor({ random = Math.random } = {}) {
    this.board = createBoard();
    this.active = null;
    this.random = random;
    this.bag = [];
  }

  nextType() {
    if (this.bag.length === 0) {
      this.bag = [...PIECE_TYPES];
      for (let i = this.bag.length - 1; i > 0; i--) {
        const j = Math.floor(this.random() * (i + 1));
        [this.bag[i], this.bag[j]] = [this.bag[j], this.bag[i]];
      }
    }
    return this.bag.pop();
  }

  // A blocked spawn returns false; the game loop can use this for game over.
  spawn(type) {
    if (this.active) return false;
    type ??= this.nextType();
    if (!Object.hasOwn(TETROMINOES, type)) throw new RangeError(`Unknown piece: ${type}`);
    const shape = TETROMINOES[type].map(row => [...row]);
    const piece = { type, shape, x: Math.floor((BOARD_WIDTH - shape.length) / 2), y: 0 };
    if (collides(this.board, piece)) return false;
    this.active = piece;
    return true;
  }

  move(dx, dy = 0) {
    if (!Number.isInteger(dx) || !Number.isInteger(dy)) {
      throw new TypeError('Movement must use integer cell offsets');
    }
    if (!this.active) return false;
    // Walk each cell so large offsets cannot tunnel through occupied cells.
    if (dx !== 0 && dy !== 0) return false;
    const steps = Math.max(Math.abs(dx), Math.abs(dy));
    let candidate = this.active;
    for (let i = 0; i < steps; i++) {
      candidate = { ...candidate, x: candidate.x + Math.sign(dx), y: candidate.y + Math.sign(dy) };
      if (collides(this.board, candidate)) return false;
    }
    this.active = candidate;
    return true;
  }

  rotate(direction = 1) {
    if (direction !== 1 && direction !== -1) throw new RangeError('Rotation must be 1 or -1');
    if (!this.active) return false;
    const shape = rotateShape(this.active.shape, direction);
    // Simple horizontal wall kicks, not the full guideline SRS kick table.
    for (const dx of [0, -1, 1, -2, 2]) {
      const candidate = { ...this.active, shape, x: this.active.x + dx };
      if (!collides(this.board, candidate)) {
        this.active = candidate;
        return true;
      }
    }
    return false;
  }

  // Lock only grounded pieces. Spawning and line clearing are separate steps.
  lock() {
    if (!this.active || !collides(this.board, { ...this.active, y: this.active.y + 1 })) {
      return false;
    }
    for (const { x, y } of pieceCells(this.active)) this.board[y][x] = this.active.type;
    this.active = null;
    return true;
  }
}
