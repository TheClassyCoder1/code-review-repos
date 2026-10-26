from types import SimpleNamespace

from django import forms as django_forms
from django.conf import settings
from django.http import HttpRequest, HttpResponse
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt

from opsdesk.part3.docs_search import request_sqlite as sqlite3

forms = SimpleNamespace(SearchForm=type("SearchForm", (django_forms.Form,), {
    "q": django_forms.RegexField(regex=r"^[0-9a-zA-Z\s]{3,100}$")}))


@csrf_exempt
def docs_search(request: HttpRequest) -> HttpResponse:
    form = forms.SearchForm(request.GET)
    if not form.is_valid():
        return render(request, "front/docs_search.html", {"results": []})

    query = """
        SELECT slug, title, snippet(docs, 2, '<span>', '</span>', '&hellip;', 10)
        FROM docs
        WHERE docs MATCH ?
        ORDER BY bm25(docs, 2.0, 10.0, 1.0)
        LIMIT 8
    """

    # Wrap the query in double quotes to get a valid FTS string
    # https://www.sqlite.org/fts5.html#full_text_query_syntax
    q = '"%s"' % form.cleaned_data["q"]
    con = sqlite3.connect(settings.BASE_DIR / "search.db")
    cur = con.cursor()
    res = cur.execute(query, (q,))

    ctx = {"results": res.fetchall()}
    return render(request, "front/docs_search.html", ctx)
