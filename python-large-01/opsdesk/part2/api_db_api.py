"""Transaction context for the placement database (`context_manager.reader`)."""

import functools

from opsdesk.db import SessionLocal


class _TransactionContext:
    def reader(self, fn):
        @functools.wraps(fn)
        def wrapper(context, *args, **kwargs):
            if getattr(context, "session", None) is not None:
                return fn(context, *args, **kwargs)
            with SessionLocal() as context.session:
                try:
                    return fn(context, *args, **kwargs)
                finally:
                    context.session = None

        return wrapper


context_manager = _TransactionContext()
