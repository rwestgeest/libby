export const WIDTH = 10;
export const HEIGHT = 20;

// Square matrices keep each piece's rotation pivot stable.
export const TETROMINOES = Object.freeze({
  I: ['0000', '1111', '0000', '0000'],
  J: ['100', '111', '000'],
  L: ['001', '111', '000'],
  O: ['11', '11'],
  S: ['011', '110', '000'],
  T: ['010', '111', '000'],
  Z: ['110', '011', '000'],
});

const TYPES = Object.keys(TETROMINOES);
const LINE_POINTS = [0, 100, 300, 500, 800];

export class Game {
  constructor({ random = Math.random } = {}) {
    this.random = random;
    this.board = Array.from({ length: HEIGHT }, () => Array(WIDTH).fill(null));
    this.score = 0;
    this.lines = 0;
    this.gameOver = false;
    this.active = null;
    this.bag = [];
    this.spawn();
  }

  nextType() {
    if (this.bag.length === 0) {
      this.bag = [...TYPES];
      for (let i = this.bag.length - 1; i > 0; i--) {
        const j = Math.floor(this.random() * (i + 1));
        [this.bag[i], this.bag[j]] = [this.bag[j], this.bag[i]];
      }
    }
    return this.bag.pop();
  }

  spawn(type = this.nextType()) {
    if (this.gameOver) return false;
    if (!Object.hasOwn(TETROMINOES, type)) {
      throw new Error(`Unknown tetromino: ${type}`);
    }
    const shape = TETROMINOES[type].map(row => [...row].map(Number));
    const piece = { type, shape, x: Math.floor((WIDTH - shape.length) / 2), y: 0 };
    if (!this.canPlace(piece)) {
      this.active = null;
      this.gameOver = true;
      return false;
    }
    this.active = piece;
    return true;
  }

  canPlace({ shape, x, y }) {
    for (let row = 0; row < shape.length; row++) {
      for (let col = 0; col < shape[row].length; col++) {
        if (!shape[row][col]) continue;
        const bx = x + col;
        const by = y + row;
        if (bx < 0 || bx >= WIDTH || by < 0 || by >= HEIGHT || this.board[by][bx]) {
          return false;
        }
      }
    }
    return true;
  }

  move(dx, dy = 0) {
    if (this.gameOver || !this.active) return false;
    const candidate = { ...this.active, x: this.active.x + dx, y: this.active.y + dy };
    if (!this.canPlace(candidate)) return false;
    this.active = candidate;
    return true;
  }

  rotate(direction = 1) {
    if (this.gameOver || !this.active) return false;
    const { shape } = this.active;
    const n = shape.length;
    const rotated = Array.from({ length: n }, (_, row) =>
      Array.from({ length: n }, (_, col) => direction < 0
        ? shape[col][n - 1 - row]
        : shape[n - 1 - col][row]));
    // Simple wall/floor kicks, not the official SRS rotation system.
    for (const [dx, dy] of [[0, 0], [-1, 0], [1, 0], [-2, 0], [2, 0], [0, -1], [0, -2]]) {
      const candidate = {
        ...this.active, shape: rotated, x: this.active.x + dx, y: this.active.y + dy,
      };
      if (this.canPlace(candidate)) {
        this.active = candidate;
        return true;
      }
    }
    return false;
  }

  tick() {
    if (this.gameOver || !this.active) return false;
    if (!this.move(0, 1)) this.lock();
    return true;
  }

  softDrop() {
    if (this.gameOver || !this.active) return false;
    if (this.move(0, 1)) this.score += 1;
    else this.lock();
    return true;
  }

  hardDrop() {
    if (this.gameOver || !this.active) return 0;
    let distance = 0;
    while (this.move(0, 1)) distance++;
    this.score += distance * 2;
    this.lock();
    return distance;
  }

  lock() {
    if (this.gameOver || !this.active) return false;
    const { shape, type, x, y } = this.active;
    for (let row = 0; row < shape.length; row++) {
      for (let col = 0; col < shape[row].length; col++) {
        if (shape[row][col]) this.board[y + row][x + col] = type;
      }
    }
    this.active = null;
    this.clearLines();
    this.spawn();
    return true;
  }

  clearLines() {
    const remaining = this.board.filter(row => row.some(cell => cell === null));
    const cleared = HEIGHT - remaining.length;
    this.board = [
      ...Array.from({ length: cleared }, () => Array(WIDTH).fill(null)),
      ...remaining,
    ];
    this.lines += cleared;
    this.score += LINE_POINTS[cleared] ?? 0;
    return cleared;
  }
}
