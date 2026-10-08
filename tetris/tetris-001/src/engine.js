export const WIDTH = 10;
export const HEIGHT = 20;
export const SHAPES = {
  I: [[0, 0, 0, 0], [1, 1, 1, 1], [0, 0, 0, 0], [0, 0, 0, 0]],
  O: [[1, 1], [1, 1]],
  T: [[0, 1, 0], [1, 1, 1], [0, 0, 0]],
  S: [[0, 1, 1], [1, 1, 0], [0, 0, 0]],
  Z: [[1, 1, 0], [0, 1, 1], [0, 0, 0]],
  J: [[1, 0, 0], [1, 1, 1], [0, 0, 0]],
  L: [[0, 0, 1], [1, 1, 1], [0, 0, 0]],
};

export class Game {
  constructor({ random = Math.random } = {}) {
    this.random = random;
    this.reset();
  }

  reset() {
    this.board = Array.from({ length: HEIGHT }, () => Array(WIDTH).fill(null));
    this.score = 0;
    this.lines = 0;
    this.gameOver = false;
    this.bag = [];
    this.active = null;
    this.spawn();
  }

  get level() { return Math.floor(this.lines / 10) + 1; }
  get gravityMs() { return Math.max(80, 800 - (this.level - 1) * 60); }

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

  spawn(type = this.nextType()) {
    if (this.gameOver) return false;
    const shape = SHAPES[type].map(row => [...row]);
    this.active = { type, shape, x: Math.floor((WIDTH - shape.length) / 2), y: 0 };
    if (this.collides(this.active)) {
      this.gameOver = true;
      return false;
    }
    return true;
  }

  collides(piece) {
    return piece.shape.some((row, dy) => row.some((cell, dx) => {
      if (!cell) return false;
      const x = piece.x + dx;
      const y = piece.y + dy;
      return x < 0 || x >= WIDTH || y >= HEIGHT ||
        (y >= 0 && this.board[y][x] !== null);
    }));
  }

  move(dx, dy = 0) {
    if (this.gameOver) return false;
    const candidate = { ...this.active, x: this.active.x + dx, y: this.active.y + dy };
    if (this.collides(candidate)) return false;
    this.active = candidate;
    return true;
  }

  rotate() {
    if (this.gameOver) return false;
    const old = this.active;
    const shape = old.shape[0].map((_, x) => old.shape.map(row => row[x]).reverse());
    // Simple horizontal wall kicks; leave the piece unchanged if none fit.
    for (const dx of [0, -1, 1, -2, 2]) {
      const candidate = { ...old, shape, x: old.x + dx };
      if (!this.collides(candidate)) {
        this.active = candidate;
        return true;
      }
    }
    return false;
  }

  tick() {
    if (this.gameOver) return false;
    if (this.move(0, 1)) return true;
    this.lock();
    return false;
  }

  softDrop() {
    if (this.gameOver) return false;
    if (this.move(0, 1)) {
      this.score++;
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
    if (this.gameOver) return;
    const { shape, x, y, type } = this.active;
    const aboveTop = shape.some((row, dy) => row.some(cell => cell && y + dy < 0));
    if (aboveTop) {
      this.gameOver = true;
      return;
    }
    shape.forEach((row, dy) => row.forEach((cell, dx) => {
      if (cell) this.board[y + dy][x + dx] = type;
    }));
    this.clearLines();
    this.spawn();
  }

  clearLines() {
    const remaining = this.board.filter(row => row.some(cell => cell === null));
    const cleared = HEIGHT - remaining.length;
    const level = this.level;
    this.board = [
      ...Array.from({ length: cleared }, () => Array(WIDTH).fill(null)),
      ...remaining,
    ];
    this.lines += cleared;
    this.score += [0, 100, 300, 500, 800][cleared] * level;
    return cleared;
  }
}
