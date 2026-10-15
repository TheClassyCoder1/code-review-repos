import os

from flask import current_app


def transcript_dir():
    return current_app.config["TRANSCRIPT_DIR"]


def list_transcript_files(student_number):
    prefix = f"{student_number}-"
    return sorted(name for name in os.listdir(transcript_dir()) if name.startswith(prefix))


def open_transcript_file(filename):
    path = os.path.join(transcript_dir(), filename)
    if not os.path.isfile(path):
        return None
    return open(path, "rb")
