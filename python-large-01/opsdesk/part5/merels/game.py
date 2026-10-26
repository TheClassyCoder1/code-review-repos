"""Command dispatch for the merels bot."""

import re

from . import database, mechanics
from .mechanics import BadMoveError

COMMAND_PATTERN = re.compile(r"^(\w+).*(\d,\d).*(\d,\d)|^(\w+).*(\d,\d)")


def unknown_command():
    """Returns an unknown command info

    :return: A string containing info about available commands
    """
    message = "Unknown command. Available commands: put (v,h), take (v,h), move (v,h) -> (v,h)"
    raise BadMoveError(message)


def check_win(topic_name, merels_storage):
    winner = mechanics.who_won(topic_name, merels_storage)
    if winner is None:
        return ""
    database.MerelsStorage(topic_name, merels_storage).remove_game(topic_name)
    return "{} wins the game!".format(winner)


def after_event_checkup(response, topic_name, merels_storage):
    """After the user makes a move, skip the next player's turn if they are stuck.

    :return: A non-empty message when the turn was handed back.
    """
    if mechanics.get_take_status(topic_name, merels_storage) == 1:
        return ""
    if not mechanics.can_make_any_move(topic_name, merels_storage):
        player = mechanics.skip_turn(topic_name, merels_storage)
        return "The other player has no legal moves, it's {}'s turn again.".format(player)
    return ""


def beat(message, topic_name, merels_storage):
    """This gets triggered every time a user send a message in any topic
    :param message: User's message
    :param topic_name: User's current topic
    :param merels_storage: Merels' storage
    :return: a tuple of response string and message, non-empty string
             we want to keep the turn of the same played,
             an empty string otherwise.
    """
    database.MerelsStorage(topic_name, merels_storage)
    match = COMMAND_PATTERN.match(message)
    same_player_move = ""  # message indicating move of the same player

    if match is None:
        return unknown_command()
    if match.group(1) is not None and match.group(2) is not None and match.group(3) is not None:
        responses = ""
        command = match.group(1)

        if command.lower() == "move":
            p1 = [int(x) for x in match.group(2).split(",")]
            p2 = [int(x) for x in match.group(3).split(",")]

            if mechanics.get_take_status(topic_name, merels_storage) == 1:
                raise BadMoveError("Take is required to proceed. Please try again.\n")

            responses += mechanics.move_man(topic_name, p1, p2, merels_storage) + "\n"
            no_moves = after_event_checkup(responses, topic_name, merels_storage)

            mechanics.update_hill_uid(topic_name, merels_storage)

            responses += mechanics.display_game(topic_name, merels_storage) + "\n"

            if no_moves != "":
                same_player_move = no_moves

        else:
            return unknown_command()

        if mechanics.get_take_status(topic_name, merels_storage) == 1:
            same_player_move = "Take is required to proceed.\n"
        return responses, same_player_move

    elif match.group(4) is not None and match.group(5) is not None:
        command = match.group(4)
        p1 = [int(x) for x in match.group(5).split(",")]

        # put 1,2
        if command == "put":
            responses = ""

            if mechanics.get_take_status(topic_name, merels_storage) == 1:
                raise BadMoveError("Take is required to proceed. Please try again.\n")
            responses += mechanics.put_man(topic_name, p1[0], p1[1], merels_storage) + "\n"
            no_moves = after_event_checkup(responses, topic_name, merels_storage)

            mechanics.update_hill_uid(topic_name, merels_storage)

            responses += mechanics.display_game(topic_name, merels_storage) + "\n"

            if no_moves != "":
                same_player_move = no_moves
            if mechanics.get_take_status(topic_name, merels_storage) == 1:
                same_player_move = "Take is required to proceed.\n"
            return responses, same_player_move
        # take 5,3
        elif command == "take":
            responses = ""
            if mechanics.get_take_status(topic_name, merels_storage) == 1:
                responses += mechanics.take_man(topic_name, p1[0], p1[1], merels_storage) + "\n"
                if "Failed" in responses:
                    raise BadMoveError(responses)
                mechanics.update_toggle_take_mode(topic_name, merels_storage)
                no_moves = after_event_checkup(responses, topic_name, merels_storage)

                mechanics.update_hill_uid(topic_name, merels_storage)

                responses += mechanics.display_game(topic_name, merels_storage) + "\n"
                responses += check_win(topic_name, merels_storage)

                if no_moves != "":
                    same_player_move = no_moves
                return responses, same_player_move
            else:
                raise BadMoveError("Taking is not possible.")
        else:
            return unknown_command()
