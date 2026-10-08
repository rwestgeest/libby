#!/usr/bin/env node

const BOARD_WIDTH = 10;
const BOARD_HEIGHT = 20;

const TETROMINOES = {
  I: [
    [0, 1], [1, 1], [2, 1], [3, 1],
  ],
  J: [
    [0, 0], [0, 1], [1, 1], [2, 1],
  ],
  L: [
    [2, 0], [0, 1], [1, 1], [2, 1],
  ],
  O: [
    [1, 0], [2, 0], [1, 1], [2, 1],
  ],
  S: [
    [1, 0], [2, 0], [0, 1], [1, 1],
  ],
  T: [
    [1, 0], [0, 1], [1, 1], [2, 1],
  ],
  Z: [
    [0, 0], [1, 0], [1, 1], [2, 1],
  ],
};

const TYPES = Object.keys(TETROMINOES);
const LINE_SCORES = [0, 100, 300, 500, 800];
const CELL_CHARS = {
  I: '██',
  J: '██',
  L: '██',
  O: '██',
  S: '██',
  T: '██',
  Z: '██',
};
const EMPTY_CELL = '  ';

function createBoard() {
  return Array.from({ length: BOARD_HEIGHT }, () => Array(BOARD_WIDTH).fill(null));
}

function cloneCells(cells) {
  return cells.map(([x, y]) => [x, y]);
}

function randomType() {
  return TYPES[Math.floor(Math.random() * TYPES.length)];
}

function rotateCells(cells, clockwise = true) {
  if (clockwise) {
    return cells.map(([x, y]) => [3 - y, x]);
  }
  return cells.map(([x, y]) => [y, 3 - x]);
}

function normalizeCells(cells) {
  const minX = Math.min(...cells.map(([x]) => x));
  const minY = Math.min(...cells.map(([, y]) => y));
  return cells.map(([x, y]) => [x - minX, y - minY]);
}

function makePiece(type = randomType()) {
  return {
    type,
    x: 3,
    y: 0,
    cells: cloneCells(TETROMINOES[type]),
  };
}

export class TetrisGame {
  constructor({ width = BOARD_WIDTH, height = BOARD_HEIGHT } = {}) {
    this.width = width;
    this.height = height;
    this.board = createBoard();
    this.score = 0;
    this.lines = 0;
    this.level = 1;
    this.gameOver = false;
    this.nextType = randomType();
    this.active = null;
    this.spawn();
  }

  spawn() {
    const type = this.nextType ?? randomType();
    this.nextType = randomType();
    this.active = makePiece(type);
    this.active.x = Math.floor((this.width - 4) / 2);
    this.active.y = 0;

    if (this.collides(this.active)) {
      this.gameOver = true;
    }

    return !this.gameOver;
  }

  occupiedCells(piece = this.active) {
    if (!piece) return [];
    return piece.cells.map(([x, y]) => [piece.x + x, piece.y + y]);
  }

  collides(piece = this.active) {
    return this.occupiedCells(piece).some(([x, y]) => (
      x < 0
      || x >= this.width
      || y < 0
      || y >= this.height
      || this.board[y][x]
    ));
  }

  move(dx, dy) {
    if (this.gameOver || !this.active) return false;

    const moved = { ...this.active, x: this.active.x + dx, y: this.active.y + dy };
    if (this.collides(moved)) return false;

    this.active = moved;
    return true;
  }

  moveLeft() {
    return this.move(-1, 0);
  }

  moveRight() {
    return this.move(1, 0);
  }

  softDrop() {
    if (this.move(0, 1)) {
      this.score += 1;
      return true;
    }

    this.lockPiece();
    return false;
  }

  hardDrop() {
    if (this.gameOver || !this.active) return 0;

    let distance = 0;
    while (this.move(0, 1)) {
      distance += 1;
    }

    this.score += distance * 2;
    this.lockPiece();
    return distance;
  }

  rotate(clockwise = true) {
    if (this.gameOver || !this.active) return false;

    if (this.active.type === 'O') return true;

    const rotatedCells = rotateCells(this.active.cells, clockwise);
    const kicks = [0, -1, 1, -2, 2];

    for (const kick of kicks) {
      const rotated = {
        ...this.active,
        x: this.active.x + kick,
        cells: rotatedCells,
      };

      if (!this.collides(rotated)) {
        this.active = rotated;
        return true;
      }
    }

    return false;
  }

  lockPiece() {
    if (!this.active) return;

    for (const [x, y] of this.occupiedCells()) {
      if (y >= 0 && y < this.height && x >= 0 && x < this.width) {
        this.board[y][x] = this.active.type;
      }
    }

    const cleared = this.clearLines();
    if (cleared > 0) {
      this.lines += cleared;
      this.score += LINE_SCORES[cleared] * this.level;
      this.level = Math.floor(this.lines / 10) + 1;
    }

    this.spawn();
  }

  clearLines() {
    const remaining = this.board.filter((row) => row.some((cell) => !cell));
    const cleared = this.height - remaining.length;

    while (remaining.length < this.height) {
      remaining.unshift(Array(this.width).fill(null));
    }

    this.board = remaining;
    return cleared;
  }

  tick() {
    if (this.gameOver) return false;
    return this.softDrop();
  }

  snapshot() {
    const cells = this.board.map((row) => [...row]);

    if (this.active) {
      for (const [x, y] of this.occupiedCells()) {
        if (y >= 0 && y < this.height && x >= 0 && x < this.width) {
          cells[y][x] = this.active.type;
        }
      }
    }

    return {
      width: this.width,
      height: this.height,
      board: cells,
      score: this.score,
      lines: this.lines,
      level: this.level,
      gameOver: this.gameOver,
      active: this.active ? { ...this.active, cells: cloneCells(this.active.cells) } : null,
      nextType: this.nextType,
    };
  }
}

function fitText(text, width) {
  if (text.length > width) return text.slice(0, width);
  return text.padEnd(width, ' ');
}

function centeredText(text, width) {
  if (text.length >= width) return text.slice(0, width);
  const left = Math.floor((width - text.length) / 2);
  return `${' '.repeat(left)}${text}`.padEnd(width, ' ');
}

function renderGame(gameOrSnapshot) {
  const snapshot = typeof gameOrSnapshot.snapshot === 'function'
    ? gameOrSnapshot.snapshot()
    : gameOrSnapshot;
  const boardWidth = snapshot.width * 2;
  const outerWidth = boardWidth + 2;
  const header = `Score:${snapshot.score} L:${snapshot.lines} Lv:${snapshot.level}`;
  const controls = '←→ move ↑ rot ↓ sp q';
  const rows = [`┌${fitText(header, outerWidth - 2)}┐`];
  const gameOverRow = Math.floor(snapshot.height / 2);
  const activeRow = gameOverRow - 1;

  for (let y = 0; y < snapshot.height; y += 1) {
    let content = snapshot.board[y]
      .map((cell) => (cell ? CELL_CHARS[cell] : EMPTY_CELL))
      .join('');

    if (snapshot.gameOver && (y === activeRow || y === gameOverRow)) {
      const message = y === activeRow ? 'GAME OVER' : 'Press q to quit';
      content = centeredText(message, boardWidth);
    }

    rows.push(`│${content}│`);
  }

  rows.push(`└${fitText(controls, outerWidth - 2)}┘`);
  return rows.join('\n');
}

export {
  BOARD_WIDTH,
  BOARD_HEIGHT,
  TETROMINOES,
  createBoard,
  makePiece,
  normalizeCells,
  renderGame,
  rotateCells,
};

function startTerminalGame({ input = process.stdin, output = process.stdout } = {}) {
  const game = new TetrisGame();
  let stopped = false;
  let timer = null;

  const write = (text) => output.write(text);
  const render = () => {
    write('\x1b[H\x1b[2J');
    write(`${renderGame(game)}\n`);
  };
  const stop = () => {
    if (stopped) return;
    stopped = true;
    if (timer) clearTimeout(timer);
    write('\x1b[?25h');
    if (input.isTTY) input.setRawMode(false);
    input.pause();
  };
  const scheduleTick = () => {
    if (stopped) return;

    const interval = Math.max(100, 800 - (game.level - 1) * 60);
    timer = setTimeout(() => {
      game.tick();
      render();
      scheduleTick();
    }, interval);
  };

  if (!input.isTTY || !output.isTTY) {
    output.write(`${renderGame(game)}\n`);
    return stop;
  }

  write('\x1b[?25l');
  input.setEncoding('utf8');
  input.setRawMode(true);
  input.resume();
  input.on('data', (key) => {
    if (key === '\u0003' || key === 'q' || key === 'Q') {
      stop();
      return;
    }

    if (game.gameOver) return;

    if (key === '\x1b[D' || key === 'a' || key === 'h') game.moveLeft();
    if (key === '\x1b[C' || key === 'd' || key === 'l') game.moveRight();
    if (key === '\x1b[A' || key === 'w' || key === 'k') game.rotate();
    if (key === '\x1b[B' || key === 's' || key === 'j') game.softDrop();
    if (key === ' ') game.hardDrop();

    render();
  });
  process.on('exit', stop);
  process.on('SIGINT', stop);

  render();
  scheduleTick();
  return stop;
}

export { startTerminalGame };

if (import.meta.url === `file://${process.argv[1]}`) {
  startTerminalGame();
}
