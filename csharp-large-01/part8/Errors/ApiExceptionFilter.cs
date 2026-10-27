using System.Data.Common;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace Lending.Part8.Errors;

public class ApiException(int statusCode, string detail) : Exception(detail)
{
    public int StatusCode { get; } = statusCode;
}

public sealed class ParseError(string detail) : ApiException(StatusCodes.Status400BadRequest, detail);

public sealed class NotFoundError(string detail) : ApiException(StatusCodes.Status404NotFound, detail);

public sealed class FieldError(string message) : Exception(message);

public sealed class ApiExceptionFilter : IExceptionFilter
{
    public const string NamedUrlRewrittenKey = "lending.named_url_rewritten";

    // Override default API exception handling to catch database integrity exceptions.
    public void OnException(ExceptionContext context)
    {
        var exc = context.Exception;
        if (exc is DbException)
        {
            exc = new ParseError(exc.Message);
        }
        if (exc is FieldError)
        {
            exc = new ParseError(exc.Message);
        }
        if (exc is ApiException apiException)
        {
            var http = context.HttpContext;
            if (http.Items.TryGetValue(NamedUrlRewrittenKey, out var originalPath)
                && !apiException.StatusCode.ToString().StartsWith('2'))
            {
                // if the URL was rewritten, and it's not a 2xx level status code,
                // revert the request path to its original value to avoid leaking
                // any context about the existence of resources
                http.Request.Path = (string)originalPath!;
                if (apiException.StatusCode == StatusCodes.Status403Forbidden)
                {
                    exc = new NotFoundError("Not found.");
                }
            }
        }

        ApplyDefaultHandler(exc, context);
    }

    private static void ApplyDefaultHandler(Exception exc, ExceptionContext context)
    {
        if (exc is not ApiException apiException)
        {
            return;
        }

        context.Result = new ObjectResult(new { detail = apiException.Message })
        {
            StatusCode = apiException.StatusCode,
        };
        context.ExceptionHandled = true;
    }
}
