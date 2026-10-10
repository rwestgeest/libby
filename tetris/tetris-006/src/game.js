export const WIDTH = 10;
export const HEIGHT = 20;

export const SHAPES = Object.freeze({
  I: [[0, 0, 0, 0], [1, 1, 1, 1], [0, 0, 0, 0], [0, 0, 0, 0]],
  J: [[1, 0, 0], [1, 1, 1], [0, 0, 0]],
  L: [[0, 0, 1], [1, 1, 1], [0, 0, 0]],
  O: [[1, 1], [1, 1]],
  S: [[0, 1, 1], [1, 1, 0], [0, 0, 0]],
  T: [[0, 1, 0], [1, 1, 1], [0, 0, 0]],
  Z: [[1, 1, 0], [0, 1, 1], [0, 0, 0]],
});
for (const shape of Object.values(SHAPES)) {
  shape.forEach(Object.freeze);
  Object.freeze(shape);
}

export function rotateMatrix(matrix, clockwise = true) {
  const size = matrix.length;
  return Array.from({ length: size }, (_, y) =>
    Array.from({ length: size }, (_, x) =>
      clockwise ? matrix[size - 1 - x][y] : matrix[x][size - 1 - y]));
}

/** Game rules only: no clocks, terminal access, or asynchronous work. */
export class Game {
  constructor({ random = Math.random } = {}) {
    this.random = random;
    this.restart();
  }

  restart() {
    this.board = Array.from({ length: HEIGHT }, () => Array(WIDTH).fill(null));
    this.score = 0;
    this.lines = 0;
    this.gameOver = false;
    this.bag = [];
    this.active = null;
    this.spawn();
  }

  get level() {
    return 1 + Math.floor(this.lines / 10);
  }

  get gravityMs() {
    return Math.max(80, 800 - (this.level - 1) * 60);
  }

  nextType() {
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
    const type = this.nextType();
    const matrix = SHAPES[type].map(row => [...row]);
    this.active = {
      type, matrix, x: Math.floor((WIDTH - matrix.length) / 2), y: 0,
    };
    if (this.collides(this.active)) {
      this.gameOver = true;
      this.active = null;
      return false;
    }
    return true;
  }

  collides(piece) {
    for (let y = 0; y < piece.matrix.length; y++) {
      for (let x = 0; x < piece.matrix[y].length; x++) {
        if (!piece.matrix[y][x]) continue;
        const bx = piece.x + x;
        const by = piece.y + y;
        if (bx < 0 || bx >= WIDTH || by >= HEIGHT) return true;
        if (by >= 0 && this.board[by][bx] !== null) return true;
      }
    }
    return false;
  }

  move(dx, dy = 0) {
    if (this.gameOver || !this.active) return false;
    const candidate = { ...this.active, x: this.active.x + dx, y: this.active.y + dy };
    if (this.collides(candidate)) return false;
    this.active = candidate;
    return true;
  }

  rotate(clockwise = true) {
    if (this.gameOver || !this.active) return false;
    if (this.active.type === 'O') return true;
    const matrix = rotateMatrix(this.active.matrix, clockwise);
    // Simple horizontal wall kicks; blocked rotations leave the piece unchanged.
    for (const dx of [0, -1, 1, -2, 2]) {
      const candidate = { ...this.active, matrix, x: this.active.x + dx };
      if (!this.collides(candidate)) {
        this.active = candidate;
        return true;
      }
    }
    return false;
  }

  tick() {
    if (this.gameOver) return false;
    if (!this.move(0, 1)) this.lock();
    return !this.gameOver;
  }

  softDrop() {
    if (this.gameOver) return false;
    if (this.move(0, 1)) {
      this.score += 1;
      return true;
    }
    this.lock();
    return false;
  }

  hardDrop() {
    if (this.gameOver) return 0;
    let distance = 0;
    while (this.move(0, 1)) distance++;
    this.score += distance * 2;
    this.lock();
    return distance;
  }

  lock() {
    if (this.gameOver || !this.active) return;
    const { matrix, x, y, type } = this.active;
    // A piece still extending above the board tops out without partial writes.
    if (matrix.some((row, dy) => y + dy < 0 && row.some(Boolean))) {
      this.gameOver = true;
      this.active = null;
      return;
    }
    for (let dy = 0; dy < matrix.length; dy++) {
      for (let dx = 0; dx < matrix[dy].length; dx++) {
        if (matrix[dy][dx]) this.board[y + dy][x + dx] = type;
      }
    }
    this.active = null;
    this.clearLines();
    this.spawn();
  }

  clearLines() {
    const remaining = this.board.filter(row => row.some(cell => cell === null));
    const cleared = HEIGHT - remaining.length;
    const points = [0, 100, 300, 500, 800];
    this.score += (points[cleared] ?? cleared * 200) * this.level;
    this.lines += cleared;
    this.board = [
      ...Array.from({ length: cleared }, () => Array(WIDTH).fill(null)),
      ...remaining,
    ];
    return cleared;
  }

  /** A detached board with the active piece overlaid, suitable for rendering. */
  displayBoard() {
    const board = this.board.map(row => [...row]);
    if (this.active) {
      const { matrix, x, y, type } = this.active;
      matrix.forEach((row, dy) => row.forEach((cell, dx) => {
        if (cell && y + dy >= 0 && y + dy < HEIGHT && x + dx >= 0 && x + dx < WIDTH) {
          board[y + dy][x + dx] = type;
        }
      }));
    }
    return board;
  }
}
