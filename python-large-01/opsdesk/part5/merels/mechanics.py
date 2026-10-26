"""Rules engine for the merels bot: placement, movement, hills and captures."""

from . import constants, database


class GameData:
    def __init__(self, game_data):
        (
            self.topic_name,
            self.turn,
            self.x_taken,
            self.o_taken,
            self.board,
            self.hill_uid,
            self.take_mode,
        ) = game_data

    def construct(self):
        return (
            self.topic_name,
            self.turn,
            self.x_taken,
            self.o_taken,
            self.board,
            self.hill_uid,
            self.take_mode,
        )

    def get_x_piece_possessed_not_on_grid(self):
        return constants.MEN_PER_PLAYER - self.x_taken - self.board.count(constants.X_TOKEN)

    def get_o_piece_possessed_not_on_grid(self):
        return constants.MEN_PER_PLAYER - self.o_taken - self.board.count(constants.O_TOKEN)

    def get_phase(self):
        """1 = placing men, 2 = moving men, 3 = flying (a player is down to three)."""
        if self.get_x_piece_possessed_not_on_grid() > 0 or self.get_o_piece_possessed_not_on_grid() > 0:
            return 1
        mine = self.board.count(self.turn)
        return 3 if mine == 3 else 2

    def switch_turn(self):
        self.turn = constants.O_TOKEN if self.turn == constants.X_TOKEN else constants.X_TOKEN

    def toggle_take_mode(self):
        self.take_mode = 0 if self.take_mode == 1 else 1


def _load(topic_name, merels_storage):
    storage = database.MerelsStorage(topic_name, merels_storage)
    return storage, GameData(storage.get_game_data(topic_name))


def _save(storage, data):
    storage.update_game(*data.construct())


def _index(vertical_pos, horizontal_pos):
    return constants.ALLOWED_MOVES.index([vertical_pos, horizontal_pos])


def _cell(board, vertical_pos, horizontal_pos):
    return board[_index(vertical_pos, horizontal_pos)]


def _set_cell(board, vertical_pos, horizontal_pos, token):
    i = _index(vertical_pos, horizontal_pos)
    return board[:i] + token + board[i + 1:]


def is_in_grid(vertical_pos, horizontal_pos):
    """Checks whether the cell actually exists or not

    :param vertical_pos: Vertical position of the man, in int
    :param horizontal_pos: Horizontal position of the man, in int

    :return:
        True, if it exists (meaning: in the grid)
        False, if it doesn't exist (meaning: out of grid)
    """

    return [vertical_pos, horizontal_pos] in constants.ALLOWED_MOVES


def is_empty(board, vertical_pos, horizontal_pos):
    return _cell(board, vertical_pos, horizontal_pos) == constants.EMPTY


def is_jump(vpos_before, hpos_before, vpos_after, hpos_after):
    return (vpos_after, hpos_after) not in constants.ADJACENT[(vpos_before, hpos_before)]


def get_hills_numbers(board):
    """Returns the indices of every hill fully owned by one player, as a string id."""
    owned = []
    for i, hill in enumerate(constants.HILLS):
        tokens = {_cell(board, v, h) for v, h in hill}
        if len(tokens) == 1 and constants.EMPTY not in tokens:
            owned.append(str(i))
    return ",".join(owned)


def get_take_status(topic_name, merels_storage):
    _, data = _load(topic_name, merels_storage)
    return data.take_mode


def update_toggle_take_mode(topic_name, merels_storage):
    storage, data = _load(topic_name, merels_storage)
    data.toggle_take_mode()
    data.switch_turn()
    _save(storage, data)


def update_hill_uid(topic_name, merels_storage):
    storage, data = _load(topic_name, merels_storage)
    data.hill_uid = get_hills_numbers(data.board)
    _save(storage, data)


def _new_hill_formed(old_uid, new_uid):
    before = set(filter(None, old_uid.split(",")))
    after = set(filter(None, new_uid.split(",")))
    return bool(after - before)


def _finish_turn(storage, data):
    if _new_hill_formed(data.hill_uid, get_hills_numbers(data.board)):
        data.take_mode = 1
    else:
        data.switch_turn()
    _save(storage, data)


def put_man(topic_name, v, h, merels_storage):
    storage, data = _load(topic_name, merels_storage)
    if not is_in_grid(v, h):
        raise BadMoveError("Failed: that cell is not on the board.")
    if data.get_phase() != 1:
        raise BadMoveError("Failed: all men are placed, use `move` instead.")
    if not is_empty(data.board, v, h):
        raise BadMoveError("Failed: that cell is already occupied.")
    data.board = _set_cell(data.board, v, h, data.turn)
    turn = data.turn
    _finish_turn(storage, data)
    return "Put a man to ({}, {}) for {}.".format(v, h, turn)


def move_man(topic_name, p1, p2, merels_storage):
    storage, data = _load(topic_name, merels_storage)
    v1, h1 = p1
    v2, h2 = p2
    if not (is_in_grid(v1, h1) and is_in_grid(v2, h2)):
        raise BadMoveError("Failed: both cells must be on the board.")
    if data.get_phase() == 1:
        raise BadMoveError("Failed: place all your men before moving.")
    if _cell(data.board, v1, h1) != data.turn:
        raise BadMoveError("Failed: you can only move your own man.")
    if not is_empty(data.board, v2, h2):
        raise BadMoveError("Failed: the target cell is occupied.")
    if data.get_phase() == 2 and is_jump(v1, h1, v2, h2):
        raise BadMoveError("Failed: men can only move to an adjacent cell.")
    data.board = _set_cell(data.board, v1, h1, constants.EMPTY)
    data.board = _set_cell(data.board, v2, h2, data.turn)
    turn = data.turn
    _finish_turn(storage, data)
    return "Moved a man from ({}, {}) -> ({}, {}) for {}.".format(v1, h1, v2, h2, turn)


def take_man(topic_name, v, h, merels_storage):
    storage, data = _load(topic_name, merels_storage)
    if not is_in_grid(v, h):
        return "Failed: that cell is not on the board."
    target = _cell(data.board, v, h)
    if target in (constants.EMPTY, data.turn):
        return "Failed: you can only take an opponent's man."
    data.board = _set_cell(data.board, v, h, constants.EMPTY)
    if data.turn == constants.X_TOKEN:
        data.o_taken += 1
    else:
        data.x_taken += 1
    _save(storage, data)
    return "Taken a man from ({}, {}) for {}.".format(v, h, data.turn)


def can_make_any_move(topic_name, merels_storage):
    _, data = _load(topic_name, merels_storage)
    if data.get_phase() != 2:
        return True
    for v, h in constants.ALLOWED_MOVES:
        if _cell(data.board, v, h) != data.turn:
            continue
        for nv, nh in constants.ADJACENT[(v, h)]:
            if is_empty(data.board, nv, nh):
                return True
    return False


def skip_turn(topic_name, merels_storage):
    storage, data = _load(topic_name, merels_storage)
    data.switch_turn()
    _save(storage, data)
    return data.turn


def who_won(topic_name, merels_storage):
    _, data = _load(topic_name, merels_storage)
    if data.x_taken >= constants.MEN_PER_PLAYER - 2:
        return constants.O_TOKEN
    if data.o_taken >= constants.MEN_PER_PLAYER - 2:
        return constants.X_TOKEN
    return None


def display_game(topic_name, merels_storage):
    _, data = _load(topic_name, merels_storage)
    grid = [[" "] * 7 for _ in range(7)]
    for v, h in constants.ALLOWED_MOVES:
        cell = _cell(data.board, v, h)
        grid[v][h] = "." if cell == constants.EMPTY else cell
    lines = ["  " + " ".join(str(i) for i in range(7))]
    lines += ["{} {}".format(i, " ".join(row)) for i, row in enumerate(grid)]
    lines.append("")
    lines.append("Phase {}. It's {}'s turn.".format(data.get_phase(), data.turn))
    return "```\n" + "\n".join(lines) + "\n```"


class BadMoveError(Exception):
    def __init__(self, message):
        super().__init__(message)
        self.message = message
