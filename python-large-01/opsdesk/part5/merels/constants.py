"""Board geometry for the merels bot.

Positions are ``[vertical, horizontal]`` pairs on a 7x7 grid; only the 24
points listed in ``ALLOWED_MOVES`` are part of the board.
"""

ALLOWED_MOVES = [
    [0, 0], [0, 3], [0, 6],
    [1, 1], [1, 3], [1, 5],
    [2, 2], [2, 3], [2, 4],
    [3, 0], [3, 1], [3, 2], [3, 4], [3, 5], [3, 6],
    [4, 2], [4, 3], [4, 4],
    [5, 1], [5, 3], [5, 5],
    [6, 0], [6, 3], [6, 6],
]

# Every straight line of three points; owning one forms a "hill" (mill).
HILLS = [
    # Rows
    [[0, 0], [0, 3], [0, 6]],
    [[1, 1], [1, 3], [1, 5]],
    [[2, 2], [2, 3], [2, 4]],
    [[3, 0], [3, 1], [3, 2]],
    [[3, 4], [3, 5], [3, 6]],
    [[4, 2], [4, 3], [4, 4]],
    [[5, 1], [5, 3], [5, 5]],
    [[6, 0], [6, 3], [6, 6]],
    # Columns
    [[0, 0], [3, 0], [6, 0]],
    [[1, 1], [3, 1], [5, 1]],
    [[2, 2], [3, 2], [4, 2]],
    [[0, 3], [1, 3], [2, 3]],
    [[4, 3], [5, 3], [6, 3]],
    [[2, 4], [3, 4], [4, 4]],
    [[1, 5], [3, 5], [5, 5]],
    [[0, 6], [3, 6], [6, 6]],
]

MEN_PER_PLAYER = 9
EMPTY = "N"
X_TOKEN = "X"
O_TOKEN = "O"
PLAYERS = (X_TOKEN, O_TOKEN)


def _build_adjacency():
    adjacency = {tuple(p): set() for p in ALLOWED_MOVES}
    for hill in HILLS:
        for first, second in zip(hill, hill[1:]):
            adjacency[tuple(first)].add(tuple(second))
            adjacency[tuple(second)].add(tuple(first))
    return adjacency


ADJACENT = _build_adjacency()
