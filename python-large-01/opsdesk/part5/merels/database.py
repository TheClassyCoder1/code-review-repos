"""Per-topic game state persisted in the bot's key/value storage."""

import json

from . import constants


def _key(topic_name):
    return "merels:" + topic_name


class MerelsStorage:
    """Thin wrapper around the bot storage handle.

    Instantiating it is cheap; the bot creates one per incoming message so
    that a topic without a game gets a fresh board on first access.
    """

    def __init__(self, topic_name, storage):
        self.storage = storage
        if not self.storage.contains(_key(topic_name)):
            self.update_game(topic_name, constants.X_TOKEN, 0, 0, self.new_board(), "", 0)

    @staticmethod
    def new_board():
        return constants.EMPTY * len(constants.ALLOWED_MOVES)

    def update_game(self, topic_name, turn, x_taken, o_taken, board, hill_uid, take_mode):
        payload = {
            "turn": turn,
            "x_taken": x_taken,
            "o_taken": o_taken,
            "board": board,
            "hill_uid": hill_uid,
            "take_mode": take_mode,
        }
        self.storage.put(_key(topic_name), json.dumps(payload))

    def remove_game(self, topic_name):
        self.storage.put(_key(topic_name), "")

    def get_game_data(self, topic_name):
        raw = self.storage.get(_key(topic_name))
        if not raw:
            return None
        data = json.loads(raw)
        return (
            topic_name,
            data["turn"],
            data["x_taken"],
            data["o_taken"],
            data["board"],
            data["hill_uid"],
            data["take_mode"],
        )
