# Terminal verification

Verification performed against `npm start` in this target directory using a real POSIX pseudo-terminal sized to 80 columns × 24 rows. Input was driven with a temporary Python script, not a human-operated terminal session.

- Initial frame: board, borders, score, and controls occupy 24 rows.
- Left then Right: displayed piece moves and returns to its original position.
- Waiting 1.15 seconds: displayed piece falls automatically.
- Down: piece descends and score becomes 1.
- Space: piece locks and another spawns.
- Repeated Space: stack reaches actual blocked-spawn game over, with message in the existing status row.
- R: fresh board, score 0, lines 0.
- Arrow movement/rotation and Space placements: actual line clearing observed (`Score:208 Lines:1` after three pieces on the final run; piece sequences are randomized).
- Every captured frame uses only cursor rows 1–24.
- Q: process exits successfully; original terminal attributes, cursor visibility, and alternate screen restored.
- Separate launches with Ctrl-C and SIGTERM: original terminal attributes, cursor visibility, and alternate screen restored.
- `npm test`: 50 tests passed, 0 failed.

The temporary verifier initially required restoration escapes to be the final output bytes. npm can emit output afterward; checking for the restoration sequence plus exact original terminal attributes resolved that verifier-only assumption. No game changes were needed, and no validator findings were supplied.
