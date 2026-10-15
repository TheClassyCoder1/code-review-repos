using LedgerApi.Security;
using LedgerApi.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers(options =>
{
    options.Filters.Add<SafeNextUrlFilter>();
});

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.Audience = builder.Configuration["Auth:Audience"];
    });

builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("AccountOwner", policy => policy
        .RequireAuthenticatedUser()
        .AddRequirements(new AccountOwnerRequirement()));
    options.AddPolicy("Ops", policy => policy.RequireRole("ops"));
});
builder.Services.AddSingleton<IAuthorizationHandler, AccountOwnerHandler>();

builder.Services.AddHttpClient("bank", client =>
{
    client.BaseAddress = new Uri(builder.Configuration["Bank:BaseUrl"]!);
    client.Timeout = TimeSpan.FromSeconds(10);
});
builder.Services.AddHttpClient("notifications", client =>
{
    client.BaseAddress = new Uri(builder.Configuration["Notifications:BaseUrl"]!);
    client.Timeout = TimeSpan.FromSeconds(5);
});

builder.Services.AddSingleton<LedgerRepository>();
builder.Services.AddSingleton<FxRateClient>();
builder.Services.AddSingleton<ImportService>();
builder.Services.AddSingleton<TransferService>();
builder.Services.AddSingleton<AuditPublisher>();
builder.Services.AddHostedService(sp => sp.GetRequiredService<AuditPublisher>());

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();

app.MapGet("/healthz", () => Results.Ok(new { status = "ok" }));
app.MapControllers();

app.Run();
