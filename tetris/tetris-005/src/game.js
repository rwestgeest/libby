export const BOARD_WIDTH = 10;
export const BOARD_HEIGHT = 20;

// Square matrices keep rotation around a stable pivot. O never changes shape.
export const TETROMINOES = Object.freeze(Object.fromEntries(
  Object.entries({
    I: [[0, 0, 0, 0], [1, 1, 1, 1], [0, 0, 0, 0], [0, 0, 0, 0]],
    O: [[1, 1], [1, 1]],
    T: [[0, 1, 0], [1, 1, 1], [0, 0, 0]],
    S: [[0, 1, 1], [1, 1, 0], [0, 0, 0]],
    Z: [[1, 1, 0], [0, 1, 1], [0, 0, 0]],
    J: [[1, 0, 0], [1, 1, 1], [0, 0, 0]],
    L: [[0, 0, 1], [1, 1, 1], [0, 0, 0]],
  }).map(([type, matrix]) => [type, Object.freeze(matrix.map(row => Object.freeze(row)))]),
));

export function createBoard() {
  return Array.from({ length: BOARD_HEIGHT }, () => Array(BOARD_WIDTH).fill(null));
}

export function rotateClockwise(matrix) {
  return matrix.map((row, y) => row.map((_, x) => matrix[matrix.length - 1 - x][y]));
}

export class Game {
  constructor({ random = Math.random } = {}) {
    this.board = createBoard();
    this.active = null;
    this.random = random;
    this.spawn();
  }

  // Returns false when the spawn area is blocked. The lifecycle controller
  // decides whether to end the game; a failed spawn never replaces a piece.
  spawn(type) {
    if (this.active) return false;
    const types = Object.keys(TETROMINOES);
    type ??= types[Math.floor(this.random() * types.length)];
    if (!Object.hasOwn(TETROMINOES, type)) throw new RangeError(`Unknown piece: ${type}`);
    const matrix = TETROMINOES[type].map(row => [...row]);
    const piece = {
      type,
      matrix,
      x: Math.floor((BOARD_WIDTH - matrix.length) / 2),
      y: 0,
    };
    if (this.collides(piece)) return false;
    this.active = piece;
    return true;
  }

  collides(piece) {
    return piece.matrix.some((row, dy) => row.some((cell, dx) => {
      if (!cell) return false;
      const x = piece.x + dx;
      const y = piece.y + dy;
      return x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT
        || this.board[y][x] !== null;
    }));
  }

  // Moves one cell in a cardinal direction. Rejected moves are atomic.
  move(dx, dy) {
    if (!Number.isInteger(dx) || !Number.isInteger(dy) || Math.abs(dx) + Math.abs(dy) !== 1) {
      throw new RangeError('Movement must be one cell in a cardinal direction');
    }
    if (!this.active) return false;
    const candidate = { ...this.active, x: this.active.x + dx, y: this.active.y + dy };
    if (this.collides(candidate)) return false;
    this.active = candidate;
    return true;
  }

  // Simple clockwise rotation: no wall kicks; collisions leave it unchanged.
  rotate() {
    if (!this.active) return false;
    const candidate = { ...this.active, matrix: rotateClockwise(this.active.matrix) };
    if (this.collides(candidate)) return false;
    this.active = candidate;
    return true;
  }

  // Only grounded pieces can lock. Spawning and line clearing are separate.
  lock() {
    if (!this.active || this.collides(this.active)
      || !this.collides({ ...this.active, y: this.active.y + 1 })) return false;
    const { type, matrix, x, y } = this.active;
    matrix.forEach((row, dy) => row.forEach((cell, dx) => {
      if (cell) this.board[y + dy][x + dx] = type;
    }));
    this.active = null;
    return true;
  }
}
