"""CA tables; `authorities.name` and `roles.name` are unique.

Roles, the CA certificate and the authority are only flushed into the request
session and nothing commits before `create_row(authority)`. If two creates of
one CA name both pass the service's lookups, only one commit can satisfy the
unique indexes: the other raises IntegrityError and rolls back all it staged.
"""

from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import Column, Integer, String, Text

db = SQLAlchemy()


class Role(db.Model):
    id = Column(Integer, primary_key=True)
    name = Column(String(128), unique=True, nullable=False)


class Certificate(db.Model):
    id = Column(Integer, primary_key=True)
    body = Column(Text, nullable=False)


class Authority(db.Model):
    id = Column(Integer, primary_key=True)
    name = Column(String(128), unique=True, nullable=False)
    owner = Column(String(128), nullable=False)

    def __init__(self, name, owner, **kwargs):
        self.name, self.owner = name, owner


def stage(row):
    db.session.add(row)
    db.session.flush()
    return row


def create_row(row):
    try:
        stage(row)
        db.session.commit()
        return row
    except Exception:
        db.session.rollback()
        raise
