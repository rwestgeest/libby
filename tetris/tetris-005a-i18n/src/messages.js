// Replace this catalogue (or pass `messages` to startApp/render) to localize.
// Messages are plain text; functions receive dynamic values, not formatted English.
export const english = {
  title: 'TETRIS',
  score: value => `Score: ${value}`,
  lines: value => `Lines: ${value}`,
  level: value => `Level: ${value}`,
  next: value => `Next: ${value}`,
  playing: 'Playing',
  paused: 'PAUSED - P resumes',
  gameOver: 'GAME OVER - R restarts',
  move: 'Left/Right or A/D: move',
  rotateClockwise: 'Up/W/X: rotate clockwise',
  rotateCounterclockwise: 'Z: rotate counterclockwise',
  softDrop: 'Down/S: soft drop',
  hardDrop: 'Space: hard drop',
  pauseRestart: 'P: pause   R: restart',
  quit: 'Q / Ctrl-C: quit',
  resize: (columns, rows) => `Resize to ${columns}x${rows}; Q quits.`,
  interactiveRequired: 'Tetris requires an interactive terminal. Run npm start in a terminal.',
};
