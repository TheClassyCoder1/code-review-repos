from sqlalchemy.orm import Session

from opsdesk.part2.data_queries.models import DataQuery, QueryUpdate


def get(*, db_session: Session, query_id: int) -> DataQuery | None:
    return db_session.get(DataQuery, query_id)


def update(*, db_session: Session, query: DataQuery, query_in: QueryUpdate) -> DataQuery:
    for field, value in query_in.model_dump(exclude_unset=True).items():
        setattr(query, field, value)
    db_session.commit()
    return query


def delete(*, db_session: Session, query_id: int) -> None:
    db_session.delete(db_session.get(DataQuery, query_id))
    db_session.commit()
