using System.Threading.Channels;
using GiftCards.Data;
using GiftCards.Jobs;
using GiftCards.Models;
using GiftCards.Security;
using GiftCards.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;

Dapper.DefaultTypeMap.MatchNamesWithUnderscores = true;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer();
builder.Services.AddAuthorization();

builder.Services.AddSingleton<IDbConnectionFactory>(
    new NpgsqlConnectionFactory(builder.Configuration.GetConnectionString("GiftCards")!));
builder.Services.AddSingleton<GiftCardRepository>();
builder.Services.AddSingleton<RedemptionRepository>();

builder.Services.AddSingleton(Channel.CreateUnbounded<VoucherIssueRequest>());
builder.Services.AddSingleton<VoucherQueue>();

builder.Services.AddHttpClient<NotificationClient>(client =>
{
    client.BaseAddress = new Uri(builder.Configuration["Notifications:BaseUrl"]!);
    client.Timeout = TimeSpan.FromSeconds(5);
});

builder.Services.AddSingleton<ProcessorClient>();
builder.Services.AddSingleton<CallbackSignatureVerifier>();
builder.Services.AddScoped<GiftCardService>();
builder.Services.AddScoped<RedemptionService>();
builder.Services.AddScoped<TransferService>();
builder.Services.AddScoped<ReportService>();

builder.Services.AddHostedService<ExpiryJob>();
builder.Services.AddHostedService<NotificationRetryJob>();
builder.Services.AddHostedService<VoucherIssuanceJob>();

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
