using Lending.Part8.Archive;
using Lending.Part8.Errors;
using Lending.Part8.Security;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers(options => options.Filters.Add<ApiExceptionFilter>());
builder.Services.AddHttpClient();
builder.Services.AddHttpContextAccessor();
builder.Services.AddAuthentication();
builder.Services.AddAuthorization();
builder.Services.AddArchiveSftp(builder.Configuration, builder.Environment);
builder.Services.AddOpsConsoleRealm();

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
