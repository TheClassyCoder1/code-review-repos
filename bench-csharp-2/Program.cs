using Hotel.Data;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

builder.WebHost.ConfigureKestrel(options =>
{
    options.Limits.MaxRequestBodySize = 256 * 1024;
});

builder.Services.AddDbContext<HotelDbContext>(o =>
    o.UseNpgsql(builder.Configuration.GetConnectionString("Hotel")));

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer();
builder.Services.AddAuthorization();
builder.Services.AddControllers();

builder.Services.AddHttpClient<PaymentGatewayClient>(client =>
{
    client.BaseAddress = new Uri(builder.Configuration["PaymentGateway:BaseUrl"]!);
    client.Timeout = TimeSpan.FromSeconds(10);
});
builder.Services.AddSingleton<ChannelManagerClient>();
builder.Services.AddSingleton<FxRateClient>();

builder.Services.AddScoped<BookingRepository>();
builder.Services.AddScoped<BookingService>();
builder.Services.AddScoped<RoomInventoryService>();
builder.Services.AddScoped<RatePlanService>();
builder.Services.AddScoped<FolioService>();
builder.Services.AddScoped<HousekeepingNotifier>();
builder.Services.AddScoped<NightAuditService>();
builder.Services.AddScoped<GuestImportService>();
builder.Services.AddSingleton<WebhookSignatureVerifier>();
builder.Services.AddSingleton<GuestPinVerifier>();
builder.Services.AddSingleton<IEmailSender, SmtpEmailSender>();

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
